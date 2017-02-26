package org.bala.neela.fs;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.bala.neela.service.BootStrapper;
import org.bala.neela.service.Browser;
import org.bala.neela.service.Downloader;
import org.bala.neela.service.SearchEngine;
import org.bala.neela.service.UploadRequestObserver;

import org.bala.neela.grpc.BrowserOutput;
import org.bala.neela.grpc.DownloadRequest;
import org.bala.neela.grpc.DownloadResponse;
import org.bala.neela.grpc.NeelaGrpc;
import org.bala.neela.grpc.PeerRegister;
import org.bala.neela.grpc.SearchRequest;
import org.bala.neela.grpc.SearchResponse;
import org.bala.neela.grpc.UploadRequest;
import org.bala.neela.grpc.UploadResponse;

import io.grpc.stub.StreamObserver;

public class P2PServices extends NeelaGrpc.NeelaImplBase {
  private final BootStrapper bootStrapper;
  private final Browser browser;
  private final SearchEngine searchEngine;
  private final Downloader downloader;
  private final String sharedLocation;
  
  public P2PServices(final BootStrapper bootStrapper,
                     final Browser browser,
                     final SearchEngine searchEngine,
                     final Downloader downloader,
                     final String sharedLocation) {
    this.bootStrapper = requireNonNull(bootStrapper);
    this.browser = requireNonNull(browser);
    this.searchEngine = requireNonNull(searchEngine);
    this.downloader = requireNonNull(downloader);
    checkArgument(isNotBlank(sharedLocation), "Invalid sharedLocation");
    this.sharedLocation = sharedLocation.trim();
  }
  
  @Override
  public void bootstrap(PeerRegister peerRegister, StreamObserver<PeerRegister> responseObserver) {
    responseObserver.onNext(bootStrapper.mergePeerRegister(peerRegister));
    responseObserver.onCompleted();
  }
  
  @Override
  public void browse(PeerRegister peerRegister, StreamObserver<BrowserOutput> responseObserver) {
    bootStrapper.mergePeerRegister(peerRegister);
    responseObserver.onNext(browser.browse());
    responseObserver.onCompleted();
  }

  @Override
  public void search(SearchRequest searchRequest, StreamObserver<SearchResponse> responseObserver) {
    responseObserver.onNext(searchEngine.search(searchRequest));
    responseObserver.onCompleted();
  }

  @Override
  public void download(DownloadRequest downloadRequest, StreamObserver<DownloadResponse> responseObserver) {
    downloader.streamResponse(downloadRequest, responseObserver);
    responseObserver.onCompleted();
  }
  
  @Override
  public StreamObserver<UploadRequest> upload(StreamObserver<UploadResponse> responseObserver) {
    return new UploadRequestObserver(responseObserver, sharedLocation);
  }
}