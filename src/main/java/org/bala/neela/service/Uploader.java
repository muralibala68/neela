package org.bala.neela.service;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.bala.neela.service.BootStrapper.isValidPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.NeelaGrpc;
import org.bala.neela.grpc.UploadRequest;
import org.bala.neela.grpc.UploadResponse;
import org.bala.neela.ui.UserCommand;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class Uploader {
  private static final Logger LOGGER = LoggerFactory.getLogger(Uploader.class);
  
  private final int port;
  private final String sharedLocation;
  
  public Uploader(final int port, final String sharedLocation) {
    validateConstructorArgs(port, sharedLocation);
    this.port = port;
    this.sharedLocation = sharedLocation.trim();
  }

  public void upload(final UserCommand userCommand, final Consumer<? super String> consumer) {
    final Entry<Path, String> pathAndHostPair = validateAndParse(userCommand);
    CompletableFuture.supplyAsync(() -> upload(pathAndHostPair.getKey(), pathAndHostPair.getValue()))
                     .exceptionally(this::handleException)
                     .thenAcceptAsync(consumer);
  }

  private String upload(final Path path, final String host) {
    final CountDownLatch uploadCompletedLatch = new CountDownLatch(1);
    final ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
    final NeelaGrpc.NeelaStub asyncStub = NeelaGrpc.newStub(channel);
    final StreamObserver<UploadResponse> responseObserver = new UploadResponseObserver(uploadCompletedLatch); 
    final StreamObserver<UploadRequest> requestObserver = asyncStub.upload(responseObserver);

    try {
      return streamContent(path, requestObserver);
    } finally {
      try {
        if (!uploadCompletedLatch.await(5, TimeUnit.SECONDS)) {
          LOGGER.warn("upload can not finish within 5 seconds");
        }
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  public String streamContent(final Path path, final StreamObserver<UploadRequest> requestObserver) {
    try (final Stream<String> stream = Files.lines(path)) {
      stream.map(line -> constructRequest(path, line))
            .forEach(requestObserver::onNext);
      requestObserver.onCompleted();
      return "streamed contents of " + path.toString() + " OK";
    } catch (IOException|RuntimeException e) {
      requestObserver.onError(e);
      final String errorMsg = "Error uploading file " + path.toString() + ", " + e.toString();
      LOGGER.error(errorMsg);
      return errorMsg;
    }
  }
  
  private UploadRequest constructRequest(Path path, final String line) {
    return UploadRequest.newBuilder()
                        .setFilename(path.getFileName().toString())
                        .setContent(line)
                        .build();    
  }
  
  private String handleException(final Throwable ex) {
    return "Error uploading " + ex.toString();
  }
  
  private void validateConstructorArgs(final int port, final String sharedLocation) {
    checkArgument(isValidPort(port), "Invalid port; out of valid range");
    checkArgument(isNotBlank(sharedLocation), "Invalid sharedLocation");
  }

  private Entry<Path, String> validateAndParse(final UserCommand userCommand) {
    try {
      final String[] argv = validateArgs(userCommand);
      final String filename = argv[0].trim();
      final String host = argv[1].trim();
      final Path path = Paths.get(sharedLocation + "/" + filename);
      checkArgument(path.toFile().exists(), "file " + filename + " notfound; Usage: upload <filename>:<host>");
      return new SimpleEntry<>(path,host);
    } catch (RuntimeException ex) {
      System.out.println(ex.getMessage());
      throw ex;
    }
  }
  
  private String[] validateArgs(final UserCommand userCommand) {
    final String[] argv = userCommand.getCommandArg().split(":");
    checkArgument(argv!=null && argv.length==2, "Invalid upload command; Usage: upload <filename>:<host>");
    checkArgument(isNotBlank(argv[0]), "Invalid filename; Usage: upload <filename>:<host>");
    checkArgument(isNotBlank(argv[1]), "Invalid hostname; Usage: upload <filename>:<host>");
    return argv;
  }
}