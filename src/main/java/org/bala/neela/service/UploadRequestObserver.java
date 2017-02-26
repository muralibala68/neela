package org.bala.neela.service;

import static org.bala.neela.service.Downloader.getPathToWriteTo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.UploadRequest;
import org.bala.neela.grpc.UploadResponse;

import io.grpc.stub.StreamObserver;

public class UploadRequestObserver implements StreamObserver<UploadRequest> {
  private static final Logger LOGGER = LoggerFactory.getLogger(UploadRequestObserver.class);

  private final StreamObserver<UploadResponse> responseObserver;
  private final String sharedLocation;
  private Path path=null;
  
  public UploadRequestObserver(final StreamObserver<UploadResponse> responseObserver,
                               final String sharedLocation) {
    this.responseObserver = responseObserver;
    this.sharedLocation = sharedLocation;
  }

  @Override
  public void onNext(UploadRequest value) {
    persistContent(value);
  }

  @Override
  public void onError(Throwable t) {
    LOGGER.error("ReceivingPeer:Error receiving/processing uploaded content", t);
  }

  @Override
  public void onCompleted() {
    final String statusMsg = "ReceivingPeer:Uploaded content received and saved as " + path.toString();
    LOGGER.info(statusMsg);
    responseObserver.onNext(constructUploadResponse(statusMsg,true));
    responseObserver.onCompleted();
  }

  private void persistContent(final UploadRequest value) {
    path = (path==null) ? getPathToWriteTo(sharedLocation, value.getFilename()) : path;
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writer.write(value.getContent() + "\n");
    } catch (IOException e) {
      LOGGER.error("Error persisting content to {}, {}", path, e);
      throw new RuntimeException(e);
    }
  }

  private UploadResponse constructUploadResponse(final String statusMsg, final boolean receivedOk) {
    return UploadResponse.newBuilder()
                         .setFilename(statusMsg)
                         .setReceivedOk(receivedOk)
                         .build();
  }
}