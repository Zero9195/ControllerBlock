package net.servfire.hellfire.bukkit.ControllerBlock;

import com.nijikokun.bukkit.Permissions.Permissions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PermissionHandler
{
  private ControllerBlock parent = null;

  private Permissions nijikokunPermissions = null;

  private List<String> builtinAdminPlayers = new ArrayList();

  public PermissionHandler(ControllerBlock p) {
    this.parent = p;
  }

  public boolean checkNijikokunPermissions(Player p, String perm) {
    if (this.nijikokunPermissions == null) {
      Plugin plug = this.parent.getServer().getPluginManager().getPlugin("Permissions");
      if (plug != null) {
        this.nijikokunPermissions = ((Permissions)plug);
        this.parent.log.debug("Nijikokun Permissions detected and enabled");
      }
    }
    if (this.nijikokunPermissions != null) {
      this.parent.log.debug("Running Nijikokun Permissions check on " + p.getName() + " for " + perm);
      return this.nijikokunPermissions.getHandler().has(p, perm);
    }
    return false;
  }

  public void addBuiltinAdminPlayer(String name)
  {
    this.builtinAdminPlayers.add(name);
  }

  public boolean isAdminPlayer(Player p) {
    this.parent.log.debug("Checking if " + p.getName() + " is a CB admin");
    if ((this.parent.getConfigu().getBool(Config.Option.ServerOpIsAdmin)) && (p.isOp())) {
      this.parent.log.debug(p.getName() + " is a server operator, and serverOpIsAdmin is set");
      return true;
    }

    if (checkNijikokunPermissions(p, "controllerblock.admin")) {
      this.parent.log.debug("Nijikokun Permissions said " + p.getName() + " has admin permissions");
      return true;
    }

    String pn = p.getName();
    Iterator i = this.builtinAdminPlayers.iterator();
    while (i.hasNext()) {
      if (((String)i.next()).equals(pn)) {
        this.parent.log.debug(p.getName() + " is listed in the ControllerBlock.ini as an admin");
        return true;
      }
    }
    this.parent.log.debug(p.getName() + " isn't an admin");
    return false;
  }

  public boolean canCreate(Player p) {
    if (isAdminPlayer(p)) {
      this.parent.log.debug(p.getName() + " is an admin, can create");
      return true;
    }

    if (checkNijikokunPermissions(p, "controllerblock.create")) {
      this.parent.log.debug("Nijikokun Permissions said " + p.getName() + " can create");
      return true;
    }

    if (this.parent.getConfigu().getBool(Config.Option.AnyoneCanCreate)) {
      this.parent.log.debug("Anyone is allowed to create, letting " + p.getName() + " create");
    }
    return this.parent.getConfigu().getBool(Config.Option.AnyoneCanCreate);
  }

  public boolean canModify(Player p) {
    if (isAdminPlayer(p)) {
      this.parent.log.debug(p.getName() + " is an admin, can modify");
      return true;
    }

    if (checkNijikokunPermissions(p, "controllerblock.modifyOther")) {
      this.parent.log.debug("Nijikokun Permissions says " + p.getName() + " has global modify permissions");
      return true;
    }

    if (this.parent.getConfigu().getBool(Config.Option.AnyoneCanModifyOther)) {
      this.parent.log.debug("Anyone is allowed to modify anyones blocks, allowing " + p.getName() + " to modify");
    }
    return this.parent.getConfigu().getBool(Config.Option.AnyoneCanModifyOther);
  }

  public boolean canModify(Player p, CBlock c) {
    if (p.getName().equals(c.getOwner())) {
      this.parent.log.debug(p.getName() + " owns this controller, allowing to modify");
      return true;
    }
    return canModify(p);
  }

  public boolean canDestroy(Player p) {
    if (isAdminPlayer(p)) {
      this.parent.log.debug(p.getName() + " is an admin, allowing destroy");
      return true;
    }

    if (checkNijikokunPermissions(p, "controllerblock.destroyOther")) {
      this.parent.log.debug("Nijikokun Permissions says " + p.getName() + " has global destroy permissions");
      return true;
    }

    if (this.parent.getConfigu().getBool(Config.Option.AnyoneCanDestroyOther)) {
      this.parent.log.debug("Anyone is allowed to destroy anyones blocks, allowing " + p.getName() + " to destroy");
    }
    return this.parent.getConfigu().getBool(Config.Option.AnyoneCanDestroyOther);
  }

  public boolean canDestroy(Player p, CBlock c) {
    if (p.getName().equals(c.getOwner())) {
      this.parent.log.debug(p.getName() + "owns this controller, allowing them to destroy it");
      return true;
    }
    return canDestroy(p);
  }
}