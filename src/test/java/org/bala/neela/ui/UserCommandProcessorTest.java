package org.bala.neela.ui;

import org.bala.neela.service.BootStrapper;
import org.bala.neela.service.Browser;
import org.bala.neela.service.Downloader;
import org.bala.neela.service.SearchEngine;
import org.bala.neela.service.Uploader;
import org.bala.neela.ui.UserCommandProcessor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserCommandProcessorTest {
  /* System under test */
  @SuppressWarnings("unused")
  private UserCommandProcessor userCommandProcessor;
  
  @Mock private BootStrapper mockBootStrapper;
  @Mock private Browser mockBrowser;
  @Mock private SearchEngine mockSearchEngine;
  @Mock private Downloader mockDownloader;
  @Mock private Uploader mockUploader;
  
  @Rule public ExpectedException exception = ExpectedException.none(); 

  @Test
  public void whenBootStrapperIsNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    userCommandProcessor = new UserCommandProcessor(null, mockBrowser, mockSearchEngine, mockDownloader, mockUploader);
  }
 
  @Test
  public void whenBrowserIsNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    userCommandProcessor = new UserCommandProcessor(mockBootStrapper, null, mockSearchEngine, mockDownloader, mockUploader);
  }

  @Test
  public void whenSearchEngineIsNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    userCommandProcessor = new UserCommandProcessor(mockBootStrapper, mockBrowser, null, mockDownloader, mockUploader);
  }

  @Test
  public void whenDownloaderIsNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    userCommandProcessor = new UserCommandProcessor(mockBootStrapper, mockBrowser, mockSearchEngine, null, mockUploader);
  }

  @Test
  public void whenAllArgumentsAreNullConstructorToThrow() {
    exception.expect(NullPointerException.class);
    userCommandProcessor = new UserCommandProcessor(null, null, null, null, null);
  }
}
