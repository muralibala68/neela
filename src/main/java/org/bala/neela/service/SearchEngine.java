package org.bala.neela.service;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.bala.neela.service.BootStrapper.isValidPort;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bala.neela.ui.UserCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.NeelaGrpc;
import org.bala.neela.grpc.PeerInfo;
import org.bala.neela.grpc.SearchRequest;
import org.bala.neela.grpc.SearchResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class SearchEngine {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngine.class);
  
  private static final String HOST_ADDRESS = getMyHostAddress();
  private static final SearchResponse ERROR = SearchResponse.newBuilder().setHostAddress(HOST_ADDRESS).setFound(false).build();

  private final int port;
  private final String sharedLocation;
  private final BootStrapper bootStrapper;
  
  public SearchEngine(final int port, final String sharedLocation, final BootStrapper bootStrapper) {
    validateConstructorArgs(port, sharedLocation);
    this.port = port;
    this.sharedLocation = sharedLocation;
    this.bootStrapper = requireNonNull(bootStrapper);
  }
  
  public void search(final UserCommand userCommand, final Consumer<? super SearchResponse> consumer) {
    final AtomicInteger completedCount = new AtomicInteger(0);
    final CompletableFuture<SearchResponse> resultFuture = new CompletableFuture<>();
    final List<CompletableFuture<SearchResponse>> futures = createFutures(userCommand);
    futures.forEach(future -> future.whenComplete((response, throwable) -> completeIfSuccessful(response, throwable, resultFuture, completedCount, futures.size(), userCommand)));
    resultFuture.exceptionally(this::handleException).thenAcceptAsync(consumer);
  }

  private void completeIfSuccessful(final SearchResponse response, final Throwable throwable, final CompletableFuture<SearchResponse> resultFuture, final AtomicInteger completedCount, final int futuresCount, final UserCommand userCommand) {
    int completed = completedCount.incrementAndGet();
    if (successful(response, throwable)) {
      resultFuture.complete(response);
    } else if (completed == futuresCount) {
      completeExceptionally(userCommand, resultFuture);
    }
  }
  
  private void completeExceptionally(final UserCommand userCommand, final CompletableFuture<SearchResponse> resultFuture) {
    final String errorMsg = userCommand.getCommandArg() + " not found with any of the known peers";
    if (resultFuture.completeExceptionally(new RuntimeException(errorMsg))) {
      System.out.println(errorMsg);
    }
  }

  private List<CompletableFuture<SearchResponse>> createFutures(final UserCommand userCommand) {
    final ExecutorService executor = Executors.newCachedThreadPool();
    final List<CompletableFuture<SearchResponse>> futures = new ArrayList<>(); 
    getHostsToSearch().forEach(host -> createFuture(userCommand, host, futures, executor));
    return futures;
  }
  
  private CompletableFuture<SearchResponse> createFuture(final UserCommand userCommand, final String host, final List<CompletableFuture<SearchResponse>> futures, final ExecutorService executor) {
    CompletableFuture<SearchResponse> cf = CompletableFuture.supplyAsync(() -> search(userCommand.getCommandArg(), host), executor);
    futures.add(cf);
    cf.whenComplete((response, throwable) -> cancelTheRestIfSuccessful(response, throwable, futures, executor));  
    return cf;
  }
  
  private void cancelTheRestIfSuccessful(final SearchResponse response, final Throwable throwable, final List<CompletableFuture<SearchResponse>> futures, final ExecutorService executor) {
    if (successful(response, throwable)) {
      cancelTheRest(futures, executor);
    }
  }

  private boolean successful(final SearchResponse response, final Throwable throwable) {
    return throwable==null && response.getFound();
  }
  
  private void cancelTheRest(final List<CompletableFuture<SearchResponse>> futures, final ExecutorService executor) {
    futures.forEach(future -> future.cancel(true));
    executor.shutdown();
    try {
      executor.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  private SearchResponse search(final String filename, final String host) {
    System.out.println("Searching peer " + host + " for " + filename);
    final SearchRequest searchRequest = SearchRequest.newBuilder().setFilename(filename).build();
    final ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
    final NeelaGrpc.NeelaBlockingStub blockingStub = NeelaGrpc.newBlockingStub(channel);

    try {
      return blockingStub.search(searchRequest);
    } catch (StatusRuntimeException e) {
      LOGGER.error("Error searching host {} for {}, {}", host, searchRequest, e);
      return ERROR;
    } finally {
      try {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  public SearchResponse search(final SearchRequest searchRequest) {
    return SearchResponse.newBuilder()
                         .setHostAddress(HOST_ADDRESS)
                         .setFilename(searchRequest.getFilename())
                         .setFound(Paths.get(sharedLocation + "/" + searchRequest.getFilename()).toFile().exists())
                         .build();
  }

  private static String getMyHostAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      LOGGER.error("Unable to find my address ;(", e);
      return "unknown";
    }
  }
  
  private List<String> getHostsToSearch() {
    return bootStrapper.getPeerRegister()
                       .getKnownPeersList()
                       .stream()
                       .map(PeerInfo::getHostAddress)
                       .distinct()
                       .collect(toList());
  }
  
  private SearchResponse handleException(final Throwable ex) {
    LOGGER.error("Error searching",ex);
    return SearchResponse.newBuilder().setFound(false).build();
  }  

  private void validateConstructorArgs(final int port, final String sharedLocation) {
    checkArgument(isValidPort(port), "Invalid port; out of valid range");
    checkArgument(isNotBlank(sharedLocation), "Invalid sharedLocation");
  }
}