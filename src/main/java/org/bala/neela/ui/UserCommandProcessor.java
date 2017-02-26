package org.bala.neela.ui;

import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.Scanner;

import org.bala.neela.service.BootStrapper;
import org.bala.neela.service.Browser;
import org.bala.neela.service.Downloader;
import org.bala.neela.service.SearchEngine;
import org.bala.neela.service.Uploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bala.neela.grpc.SearchResponse;

public class UserCommandProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserCommandProcessor.class); 
  private static final String CMD_PROMPT = "\n>>";
  
  private final BootStrapper bootStrapper;
  private final Browser browser;
  private final SearchEngine searchEngine;
  private final Downloader downloader;
  private final Uploader uploader;
  
  public UserCommandProcessor(final BootStrapper bootStrapper,
                              final Browser browser,
                              final SearchEngine searchEngine,
                              final Downloader downloader,
                              final Uploader uploader) {
    this.bootStrapper = requireNonNull(bootStrapper);
    this.browser = requireNonNull(browser);
    this.searchEngine = requireNonNull(searchEngine);
    this.downloader = requireNonNull(downloader);
    this.uploader = requireNonNull(uploader);
  }
  
  public void serviceUserRequests() {
    try (Scanner scanner = new Scanner(System.in)) {
      while(true) {
        try {
          processCommand(acceptCommand(scanner));
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        } catch (RuntimeException ex) {
          LOGGER.error("Exception processing user command", ex);
        }
      }
    }
  }

  private UserCommand acceptCommand(final Scanner scanner) throws InterruptedException {
    display(CMD_PROMPT + "Accepting user commands...");
    final String input = scanner.nextLine();
    display("Processing \"" + input + "\"...");
    try {
      return new UserCommand(input);
    } catch (RuntimeException ex) {
      LOGGER.error("Invalid User command",ex);
      return null;
    }
  }
  
  private void processCommand(final UserCommand userCommand) throws InterruptedException {
    if (userCommand == null) {
      display("Invalid command!");
      usage();
      return;
    }
    switch(userCommand.getCommand()) {
      case HELP:
        usage();
        break;
      case LISTPEERS:
        bootStrapper.listPeers(this::display);
        break;
      case BROWSE:
        browser.browse(userCommand, this::display);
        break;
      case SEARCH:
        searchEngine.search(userCommand, this::display);
        break;
      case DOWNLOAD:
        downloader.download(userCommand);
        break;
      case UPLOAD:
        uploader.upload(userCommand, this::display);
        break;
      case QUIT:
        display("Exiting...");
        System.exit(0);
        break;
      default:
        LOGGER.error("Unsupported user command {}", userCommand);
    }
  }

  private void usage() {
    display("Available valid commands...");
    Arrays.asList(UserCommand.Command.values())
          .forEach(this::display);
  }

  private void display(final Object object) {
    System.out.println(object);
    LOGGER.info("{}", object);
  }

  private void display(final SearchResponse response) {
    String string = response.getFound() ? "Found:" : "NotFound:";
    System.out.println(string + response.toString());
    LOGGER.info("{}{}", string, response);
  }
}