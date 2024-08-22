package io.github.gonalez.znpcs.commands;

import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;

public class CommandInvoker {
  private final Command command;
  
  private final Method commandMethod;
  
  private final String permission;
  
  public CommandInvoker(Command command, Method commandMethod, String permission) {
    this.command = command;
    this.commandMethod = commandMethod;
    this.permission = permission;
  }
  
  public void execute(CommandSender sender, Object command) throws CommandPermissionException, CommandExecuteException {
    if (this.permission.length() > 0 && !sender.hasPermission(this.permission))
      throw new CommandPermissionException("Insufficient permission."); 
    try {
      this.commandMethod.invoke(this.command, sender, command);
    } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) {
      throw new CommandExecuteException(e.getMessage(), e.getCause());
    } 
  }
}
