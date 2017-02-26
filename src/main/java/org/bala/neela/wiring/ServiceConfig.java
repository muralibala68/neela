package org.bala.neela.wiring;

import java.util.Arrays;

import org.bala.neela.fs.P2PServer;
import org.bala.neela.fs.P2PServices;
import org.bala.neela.fs.Peer;
import org.bala.neela.service.BootStrapper;
import org.bala.neela.service.Browser;
import org.bala.neela.service.Downloader;
import org.bala.neela.service.SearchEngine;
import org.bala.neela.service.Uploader;
import org.bala.neela.ui.UserCommandProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.BindableService;

@Configuration
public class ServiceConfig {
  private final String host = "localhost"; // host where a peer is guaranteed to be running for others to bootstrap
  private final int port = 51162;
  private final String sharedLocation = "./share";
  private final String peerRegister = "./share/PeerRegister.txt";
  
  @Autowired BootStrapper bootStrapper;
	
	@Bean
	public BindableService p2pServices() {
	  return new P2PServices(bootStrapper, browser(), searchEngine(), downloader(), sharedLocation);
	}
	
	@Bean
	public P2PServer p2pServer() {
	  return new P2PServer(port, Arrays.asList(p2pServices()));
	}
	
	@Bean
	public BootStrapper bootStrapper() {
	  return new BootStrapper(host, port, peerRegister);
	}
	
	@Bean
	public Browser browser() {
	  return new Browser(port, sharedLocation, bootStrapper);
	}
	
	@Bean
	public SearchEngine searchEngine() {
	  return new SearchEngine(port, sharedLocation, bootStrapper);
	}
	
	@Bean
	public Downloader downloader() {
	  return new Downloader(port, sharedLocation, searchEngine());
	}

	@Bean
	public Uploader uploader() {
	  return new Uploader(port, sharedLocation);
	}
	
	@Bean
	public UserCommandProcessor userCommandProcessor() {
	  return new UserCommandProcessor(bootStrapper, browser(), searchEngine(), downloader(), uploader());
	}
	
	@Bean
	public Peer peer() {
	  return new Peer(p2pServer(), bootStrapper, userCommandProcessor());
	}
}