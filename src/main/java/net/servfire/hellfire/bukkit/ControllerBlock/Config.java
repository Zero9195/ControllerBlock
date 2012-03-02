package net.servfire.hellfire.bukkit.ControllerBlock;

import java.util.HashMap;
import org.bukkit.Material;

public class Config
{
  private HashMap<Option, Object> options = new HashMap();

  public void setOpt(Option opt, Object arg) {
    this.options.put(opt, arg);
  }

  public boolean getBool(Option opt) {
    return ((Boolean)getOpt(opt)).booleanValue();
  }

  public Integer getInt(Option opt) {
    return (Integer)getOpt(opt);
  }

  public Object getOpt(Option opt) {
    if (!hasOption(opt))
      switch (opt) {
      case AnyoneCanCreate:
        return Material.IRON_BLOCK;
      case AnyoneCanDestroyOther:
        return Material.GOLD_BLOCK;
      case AnyoneCanModifyOther:
        return Material.DIAMOND_BLOCK;
      case MaxBlocksPerController:
        return Boolean.valueOf(false);
      case DisableNijikokunPermissions:
        return BlockProtectMode.protect;
      case SemiProtectedControllerBlockType:
        return BlockProtectMode.protect;
      case ServerOpIsAdmin:
        return BlockProtectMode.protect;
      case PistonProtection:
        return Boolean.valueOf(false);
      case UnProtectedControllerBlockType:
        return Boolean.valueOf(true);
      case ControllerBlockType:
        return Integer.valueOf(0);
      case DisableEditDupeProtection:
        return Integer.valueOf(0);
      case MaxDistanceFromController:
        return Boolean.valueOf(false);
      case BlockEditProtectMode:
        return Boolean.valueOf(true);
      case BlockFlowProtectMode:
        return Boolean.valueOf(true);
      case BlockPhysicsProtectMode:
        return Boolean.valueOf(true);
      case BlockProtectMode:
        return Boolean.valueOf(true);
      case QuickRedstoneCheck:
      }
    return this.options.get(opt);
  }

  public boolean hasOption(Option opt) {
    return this.options.containsKey(opt);
  }

  public static enum Option
  {
    ControllerBlockType, 
    SemiProtectedControllerBlockType, 
    UnProtectedControllerBlockType, 
    ServerOpIsAdmin, 
    AnyoneCanCreate, 
    AnyoneCanModifyOther, 
    AnyoneCanDestroyOther, 
    MaxBlocksPerController, 
    MaxDistanceFromController, 
    BlockProtectMode, 
    QuickRedstoneCheck, 
    DisableNijikokunPermissions, 
    DisableEditDupeProtection, 
    BlockEditProtectMode, 
    BlockPhysicsProtectMode, 
    BlockFlowProtectMode, 
    PistonProtection;
  }
}