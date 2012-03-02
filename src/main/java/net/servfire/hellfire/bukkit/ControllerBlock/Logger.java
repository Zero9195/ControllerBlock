package net.servfire.hellfire.bukkit.ControllerBlock;

import org.bukkit.plugin.PluginDescriptionFile;

public class Logger
{
  private java.util.logging.Logger actualLog;
  private ControllerBlock parent;

  public Logger(ControllerBlock c, String n)
  {
    this.parent = c;
    this.actualLog = java.util.logging.Logger.getLogger(n);
  }

  public void info(String msg) {
    this.actualLog.info(this.parent.getDescription().getName() + ": " + msg);
  }

  public void warning(String msg) {
    this.actualLog.warning(this.parent.getDescription().getName() + ": " + msg);
  }

  public void severe(String msg) {
    this.actualLog.severe(this.parent.getDescription().getName() + ": " + msg);
  }

  public void debug(String msg)
  {
  }
}