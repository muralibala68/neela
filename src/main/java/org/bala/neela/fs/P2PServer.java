package org.bala.neela.fs;

import static com.google.common.base.Preconditions.checkArgument;
import static org.bala.neela.service.BootStrapper.isValidPort;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

public class P2PServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(P2PServer.class);
  
  private final int port;
  private Server server; 
  private final List<BindableService> bindableServices;
  
  public P2PServer(final int port, final List<BindableService> bindableServices) {
    checkArgument(isValidPort(port), "Invalid port; out of valid range");
    checkArgument(bindableServices != null && !bindableServices.isEmpty(), "No services to bind to!");

    this.port = port;
    this.bindableServices = bindableServices;
  }
  
  public void start() throws IOException {
    final NettyServerBuilder nettyServerBuilder = NettyServerBuilder.forPort(port);
    bindableServices.forEach(nettyServerBuilder::addService);
    server = nettyServerBuilder.build();
    server.start();
    LOGGER.info("Listening for requests from remote peers at port {}...", port);
  }
  
  public void stop() {
    if (server!=null) {
      server.shutdown();
    }
  }

  public void blockUntilShutdown() throws InterruptedException {
    if (server!=null) {
      server.awaitTermination();
    }
  }
}