package org.bala.neela.fs;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.IOException;

import org.bala.neela.fs.P2PServer;
import org.bala.neela.fs.Peer;
import org.bala.neela.service.BootStrapper;
import org.bala.neela.ui.UserCommandProcessor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PeerTest {
  /* System under test */
  private Peer peer;
  
  @Mock private P2PServer mockP2PServer;
  @Mock private BootStrapper mockBootStrapper;
  @Mock private UserCommandProcessor mockUserCommandProcessor;
  
  @Rule public ExpectedException exception = ExpectedException.none(); 
  
  @Test
  public void whenP2PServerIsNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    peer = new Peer(null, mockBootStrapper, mockUserCommandProcessor);
  }

  @Test
  public void whenBootStrapperIsNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    peer = new Peer(mockP2PServer, null, mockUserCommandProcessor);
  }

  @Test
  public void whenUserCommandProcesserIsNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    peer = new Peer(mockP2PServer, mockBootStrapper, null);
  }

  @Test
  public void whenAllArgumentsAreNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    peer = new Peer(null, null, null);
  }
  
  @Test
  public void whenListenForRemotePeersIsCalled() throws IOException, InterruptedException {
    // given
    peer = new Peer(mockP2PServer, mockBootStrapper, mockUserCommandProcessor);
    
    // when
    peer.listenForRemotePeers();
    
    // then
    verify(mockP2PServer).start();
    verify(mockP2PServer).blockUntilShutdown();
    verifyZeroInteractions(mockBootStrapper, mockUserCommandProcessor);
  }
  
  @Test
  public void whenListenForLocalUserCommandsIsCalled() throws InterruptedException {
    // given
    peer = new Peer(mockP2PServer, mockBootStrapper, mockUserCommandProcessor);
    
    // when
    peer.listenForLocalUserCommands();
    
    // then
    InOrder inOrder = inOrder(mockBootStrapper, mockUserCommandProcessor);
    inOrder.verify(mockBootStrapper).bootStrap();
    inOrder.verify(mockUserCommandProcessor).serviceUserRequests();
    verifyZeroInteractions(mockP2PServer);
  }  
}
