package org.bala.neela.service;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.UploadResponse;

import io.grpc.stub.StreamObserver;

public class UploadResponseObserver implements StreamObserver<UploadResponse> {
  private static final Logger LOGGER = LoggerFactory.getLogger(UploadResponseObserver.class);

  private final CountDownLatch uploadCompletedLatch;
  
  public UploadResponseObserver(final CountDownLatch uploadCompletedLatch) {
    this.uploadCompletedLatch = uploadCompletedLatch;
  }

  @Override
  public void onNext(UploadResponse value) {
    LOGGER.info("UploadingPeer:Received uploadResponse {}", value);
    System.out.println("Upload completed");
  }

  @Override
  public void onError(Throwable t) {
    LOGGER.error("UploadingPeer:Error receiving/processing uploaded file content", t);
    uploadCompletedLatch.countDown();
  }

  @Override
  public void onCompleted() {
    LOGGER.info("UploadingPeer:Processed uploadResponse ok");
    uploadCompletedLatch.countDown();
  }
}