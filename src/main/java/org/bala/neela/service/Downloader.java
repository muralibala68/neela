package org.bala.neela.service;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.bala.neela.service.BootStrapper.isValidPort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.bala.neela.ui.UserCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.DownloadRequest;
import org.bala.neela.grpc.DownloadResponse;
import org.bala.neela.grpc.NeelaGrpc;
import org.bala.neela.grpc.SearchResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class Downloader {
  private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);
  
  private final int port;
  private final String sharedLocation;
  private final SearchEngine searchEngine;
  
  public Downloader(final int port, final String sharedLocation, final SearchEngine searchEngine) {
    validateConstructorArgs(port, sharedLocation);
    this.port = port;
    this.sharedLocation = sharedLocation.trim();
    this.searchEngine = requireNonNull(searchEngine);
  }

  public void download(final UserCommand userCommand) {
    searchEngine.search(userCommand, this::download);
  }
  
  private void download(final SearchResponse searchResponse) {
    LOGGER.info("Search response received:{}", searchResponse);
    if (!searchResponse.getFound()) {
      return;
    }
    final DownloadRequest downloadRequest = DownloadRequest.newBuilder().setFilename(searchResponse.getFilename()).build();
    final ManagedChannel channel = ManagedChannelBuilder.forAddress(searchResponse.getHostAddress(), port).usePlaintext(true).build();
    final NeelaGrpc.NeelaBlockingStub blockingStub = NeelaGrpc.newBlockingStub(channel);

    try {
      Iterator<DownloadResponse> it = blockingStub.download(downloadRequest);
      persist(downloadRequest.getFilename(),it);
    } catch (StatusRuntimeException e) {
      LOGGER.error("Error downloading {} from {}, {}", downloadRequest, searchResponse.getHostAddress(), e);
    } finally {
      try {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  public void streamResponse(final DownloadRequest downloadRequest, StreamObserver<DownloadResponse> responseObserver) {
    LOGGER.info("Processing downloadRequest {}",  downloadRequest);
    try (final Stream<String> stream = Files.lines(Paths.get(sharedLocation + "/" + downloadRequest.getFilename()))) {
      stream.map(line -> constructResponse(downloadRequest, line))
            .forEach(responseObserver::onNext);
    } catch (IOException e) {
      LOGGER.error("Error reading file {}, {}", downloadRequest, e);
      throw new RuntimeException(e);
    }
  }

  private DownloadResponse constructResponse(final DownloadRequest downloadRequest, final String line) {
    return DownloadResponse.newBuilder()
                           .setFilename(downloadRequest.getFilename())
                           .setContent(line)
                           .build();
  }
  
  private void persist(final String filename, final Iterator<DownloadResponse> it) {
    if (it == null || !it.hasNext()) {
      LOGGER.error("Empty/Null iterator!!!");
      return;
    }

    final Path path = getPathToWriteTo(sharedLocation, filename);
    write(it, path);

    final String msg = "Downloading of " + filename + " complete";
    System.out.println(msg);
    LOGGER.info(msg);
  }

  private void write(final Iterator<DownloadResponse> it, final Path path) {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW)) {
      while (it.hasNext()) {
        writer.write(it.next().getContent() + "\n");
      }
    } catch (IOException e) {
      LOGGER.error("Error writing to file:{}, {}", path, e);
      throw new RuntimeException(e);
    }
  }
  
  public static Path getPathToWriteTo(final String sharedLocation, final String filename) {
    Path path = Paths.get(sharedLocation + "/" + filename);
    if (path.toFile().exists()) {
      final String newFilename = filename + "." + System.currentTimeMillis();
      path = Paths.get(sharedLocation + "/" + newFilename);
    }
    return path;
  }

  private void validateConstructorArgs(final int port, final String sharedLocation) {
    checkArgument(isValidPort(port), "Invalid port; out of valid range");
    checkArgument(StringUtils.isNoneBlank(sharedLocation), "Invalid sharedLocation");
  }
}