package org.bala.neela.fs;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bala.neela.wiring.NeelaConfig;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Neela {

  public static void main(String[] argv) throws InterruptedException, IOException {
    final ExecutorService executor = Executors.newSingleThreadExecutor();

    try (final ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(NeelaConfig.class)) {
      applicationContext.registerShutdownHook();
      final Peer peer = applicationContext.getBean(Peer.class); 
      executor.execute(() -> peer.listenForLocalUserCommands());
      peer.listenForRemotePeers();
    }
    
    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);
  }
}