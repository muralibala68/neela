package org.bala.neela.ui;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static java.util.Objects.requireNonNull;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

@AutoProperty
public class UserCommand {
  
  public enum Command {
    HELP(false),
    LISTPEERS(false),
    BROWSE(true),
    SEARCH(true),
    DOWNLOAD(true),
    UPLOAD(true),
    QUIT(false);
    
    private final boolean requireArg;
    
    private Command(final boolean requireArg) {
      this.requireArg = requireArg;
    }
    
    public boolean requiresArg() {
      return requireArg;
    }
  };
  
  private final Command command;
  private String commandArg;
  
  public UserCommand(final String userInput) {
    checkArgument(isNoneBlank(userInput), "Invalid userInput, blank/null");
    final String[] argv = userInput.split("\\s+");
    this.command = Command.valueOf(argv[0].trim().toUpperCase());
    if (command.requiresArg()) {
      this.commandArg = requireNonNull(argv[1].trim());
    }
  }
  
  public Command getCommand() {
    return command;
  }

  public String getCommandArg() {
    return commandArg;
  }
  
  @Override
  public String toString() {
    return Pojomatic.toString(this);
  }
}