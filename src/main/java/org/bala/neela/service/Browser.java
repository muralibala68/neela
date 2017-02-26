package org.bala.neela.service;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.bala.neela.service.BootStrapper.isValidPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.bala.neela.ui.UserCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.BrowserOutput;
import org.bala.neela.grpc.NeelaGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class Browser {
  private static final Logger LOGGER = LoggerFactory.getLogger(BootStrapper.class);
  public static final BrowserOutput EMPTY_OUTPUT = BrowserOutput.newBuilder().build();

  private final int port;
  private final String sharedLocation;
  private final BootStrapper bootStrapper;
  
  public Browser(final int port, final String sharedLocation, final BootStrapper bootStrapper) {
    checkArgument(isValidPort(port), "Invalid port; out of valid range");
    checkArgument(isNoneBlank(sharedLocation), "Invalid sharedLocation");

    this.port = port;
    this.sharedLocation = sharedLocation;
    this.bootStrapper = requireNonNull(bootStrapper);
  }
  
  public void browse(final UserCommand userCommand, final Consumer<? super BrowserOutput> consumer) {
    CompletableFuture.supplyAsync(() -> browse(userCommand.getCommandArg()))
                     .exceptionally(this::handleException)
                     .thenAcceptAsync(consumer);
  }

  private BrowserOutput browse(final String host) {
    final ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
    final NeelaGrpc.NeelaBlockingStub blockingStub = NeelaGrpc.newBlockingStub(channel);

    try {
      return blockingStub.browse(bootStrapper.getPeerRegister());
    } catch (StatusRuntimeException e) {
      LOGGER.error("Error browsing host:{}, {}", host, e);
      return EMPTY_OUTPUT;
    } finally {
      try {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  public BrowserOutput browse() {
    try (final Stream<Path> paths = Files.list(Paths.get(sharedLocation))) {
      return BrowserOutput.newBuilder().addAllFilenames(getFilenames(paths)).build();
    } catch (IOException e) {
      LOGGER.error("Error browsing location {}, {}", sharedLocation, e);
      return EMPTY_OUTPUT;
    }
  }
  
  private List<String> getFilenames(final Stream<Path> paths) throws IOException {
    return paths.filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(toList());
  }

  private BrowserOutput handleException(final Throwable ex) {
    return BrowserOutput.newBuilder().setErrorStatus("Error browsing:" + ex.toString()).build();
  }
}