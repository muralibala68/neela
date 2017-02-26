package org.bala.neela.service;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.NeelaGrpc;
import org.bala.neela.grpc.PeerInfo;
import org.bala.neela.grpc.PeerRegister;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class BootStrapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(BootStrapper.class);

  private final String host;
  private final int port;
  private final String peerRegisterPath;
  private AtomicReference<PeerRegister> peerRegister = new AtomicReference<>();
  
  public BootStrapper(final String host, final int port, final String peerRegisterPath) {
    checkArgument(isNoneBlank(host), "Invalid central host");
    checkArgument(isValidPort(port), "Invalid port; out of valid range");
    checkArgument(isNoneBlank(peerRegisterPath), "Invalid peerRegisterPath");

    this.host = host.trim();
    this.port = port;
    this.peerRegisterPath = peerRegisterPath.trim();
  }

  @PostConstruct
  private void loadFromLocalCache() throws IOException {
    LOGGER.info("Loading locally existing peerRegister...");
    peerRegister.set(PeerRegister.newBuilder().addAllKnownPeers(getKnownPeers()).build());
  }

  private List<PeerInfo> getKnownPeers() throws IOException {
    final Path path = Paths.get(peerRegisterPath);
    if (path.toFile().exists()) {
      try (final Stream<String> hosts = Files.lines(path)) {
        return hosts.map(h -> PeerInfo.newBuilder().setHostAddress(h).build()).collect(toList());
      }
    } else {
      return Arrays.asList(PeerInfo.newBuilder().setHostAddress("127.0.0.1").build());
    }
  }
  
  @PreDestroy
  private void persistPeerRegister() throws IOException {
    LOGGER.info("Persisting the current peerRegister");
    final Path path = Paths.get(peerRegisterPath);
    final List<String> lines = peerRegister.get().getKnownPeersList().stream().map(PeerInfo::getHostAddress).collect(toList());
    Files.write(path, lines, StandardOpenOption.CREATE);
  }
  
  public void bootStrap() throws InterruptedException {
    LOGGER.info("Bootstrapping from {}:{}...", host, port);
    final ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
    final NeelaGrpc.NeelaBlockingStub blockingStub = NeelaGrpc.newBlockingStub(channel);
    try {
      peerRegister.set(blockingStub.bootstrap(peerRegister.get()));
      LOGGER.info("Updated list of known peers: {}", peerRegister);
    } catch (StatusRuntimeException e) {
      LOGGER.error("Failed bootsrapping",e);
    } finally {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
  
  public PeerRegister getPeerRegister() {
    return peerRegister.get();
  }
  
  public synchronized PeerRegister mergePeerRegister(final PeerRegister peerRegisterToMerge) {
    final Set<PeerInfo> peerInfosMerged = new HashSet<>(peerRegisterToMerge.getKnownPeersList());
    final PeerRegister peerRegisterExisting = peerRegister.get();
    peerInfosMerged.addAll(peerRegisterExisting.getKnownPeersList());
    final PeerRegister peerRegisterMerged = PeerRegister.newBuilder().addAllKnownPeers(peerInfosMerged).build();
    peerRegister.set(peerRegisterMerged);
    return peerRegister.get();
  }
  
  public void listPeers(final Consumer<? super PeerInfo> consumer) {
    getPeerRegister().getKnownPeersList().forEach(consumer);
  }
  
  public static boolean isValidPort(int port) {
    return port > 1024 && port < 65535;
  }
}