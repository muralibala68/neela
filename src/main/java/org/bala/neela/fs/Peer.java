package org.bala.neela.fs;

import static java.util.Objects.requireNonNull;
import java.io.IOException;

import org.bala.neela.service.BootStrapper;
import org.bala.neela.ui.UserCommandProcessor;

public class Peer {
  private final P2PServer p2pServer;
  private final BootStrapper bootStrapper;
  private final UserCommandProcessor userCommandProcessor;
  
  public Peer(final P2PServer p2pServer,
              final BootStrapper bootStrapper,
              final UserCommandProcessor userCommandProcessor) {
    this.p2pServer = requireNonNull(p2pServer);
    this.bootStrapper = requireNonNull(bootStrapper);
    this.userCommandProcessor = requireNonNull(userCommandProcessor);
  }
  
  public void listenForRemotePeers() throws IOException, InterruptedException {
    p2pServer.start();
    p2pServer.blockUntilShutdown();
  }

  public void listenForLocalUserCommands() {
    try {
      bootStrapper.bootStrap();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    userCommandProcessor.serviceUserRequests();
  }
}