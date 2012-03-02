package net.servfire.hellfire.bukkit.ControllerBlock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class ControllerBlock extends JavaPlugin
  implements Runnable
{
  private static String configFile = "ControllerBlock.ini";
  private static String saveDataFile = "ControllerBlock.dat";

  public Logger log = new Logger(this, "Minecraft");
  private Config config = new Config();
  private PermissionHandler permissionHandler = new PermissionHandler(this);
  private final CBlockListener blockListener = new CBlockListener(this);
  private final CPlayerListener playerListener = new CPlayerListener(this);
  private final CBlockRedstoneCheck checkRunner = new CBlockRedstoneCheck(this);

  public boolean blockPhysicsEditCheck = false;
  private boolean beenLoaded = false;
  private boolean beenEnabled = false;

  public HashMap<Player, CBlock> map = new HashMap();

  public List<CBlock> blocks = new ArrayList();

  HashMap<String, CBlock> movingCBlock = new HashMap();
  HashMap<String, Location> moveHere = new HashMap();
  private Material CBlockType;
  private Material semiProtectedCBlockType;
  private Material unProtectedCBlockType;
  private List<Material> DisallowedTypesAll = new ArrayList();
  private List<Material> UnprotectedBlocks = new ArrayList();

  public void onDisable()
  {
  }

  public void onLoad()
  {
    if (!this.beenLoaded) {
      this.log.info(getDescription().getVersion() + " by Zero9195 (Original by Hell_Fire)");
      checkPluginDataDir();
      loadConfig();

      this.beenLoaded = true;
    }
  }

  public void onEnable()
  {
    if (!this.beenEnabled) {
      PluginManager pm = getServer().getPluginManager();

      this.log.debug("Registering events:");

      this.log.debug(" - BLOCK_DAMAGE");
      pm.registerEvent(Event.Type.BLOCK_DAMAGE, this.blockListener, Event.Priority.Highest, this);

      this.log.debug(" - BLOCK_BREAK");
      pm.registerEvent(Event.Type.BLOCK_BREAK, this.blockListener, Event.Priority.Highest, this);

      this.log.debug(" - BLOCK_PLACE");
      pm.registerEvent(Event.Type.BLOCK_PLACE, this.blockListener, Event.Priority.Monitor, this);

      this.log.debug(" - BLOCK_PHYSICS");
      pm.registerEvent(Event.Type.BLOCK_PHYSICS, this.blockListener, Event.Priority.Monitor, this);

      this.log.debug(" - BLOCK_FROMTO");
      pm.registerEvent(Event.Type.BLOCK_FROMTO, this.blockListener, Event.Priority.Monitor, this);

      this.log.debug(" - BLOCK_PISTON");
      pm.registerEvent(Event.Type.BLOCK_PISTON_EXTEND, this.blockListener, Event.Priority.Monitor, this);
      pm.registerEvent(Event.Type.BLOCK_PISTON_RETRACT, this.blockListener, Event.Priority.Monitor, this);

      this.log.debug(" - PLAYER_INTERACT");
      pm.registerEvent(Event.Type.PLAYER_INTERACT, this.playerListener, Event.Priority.Highest, this);

      this.log.debug("Scheduling tasks:");

      this.log.debug(" - Anti-dupe/changed-block edit check");
      if (getServer().getScheduler().scheduleSyncRepeatingTask(this, this.blockListener, 1L, 1L) == -1) {
        this.log.warning("Scheduling BlockListener anti-dupe check failed, falling back to old BLOCK_PHYSICS event");
        this.blockPhysicsEditCheck = true;
      }
      if (this.config.getBool(Config.Option.DisableEditDupeProtection)) {
        this.log.warning("Edit dupe protection has been disabled, you're on your own from here");
      }

      if (!this.config.getBool(Config.Option.QuickRedstoneCheck)) {
        this.log.debug(" - Redstone check");
        this.log.info("Enabling full redstone check");
        if (getServer().getScheduler().scheduleSyncRepeatingTask(this, this.checkRunner, 1L, 1L) == -1) {
          this.log.warning("Scheduling CBlockRedstoneCheck task failed, falling back to quick REDSTONE_CHANGE event");
          this.config.setOpt(Config.Option.QuickRedstoneCheck, Boolean.valueOf(true));
        }
      }

      if (this.config.getBool(Config.Option.QuickRedstoneCheck)) {
        this.log.info("Enabling 'quick' redstone check - this mode of operation is depreciated and may be removed later");
        pm.registerEvent(Event.Type.REDSTONE_CHANGE, this.blockListener, Event.Priority.Monitor, this);
      }

      if (getServer().getScheduler().scheduleSyncDelayedTask(this, this, 1L) == -1) {
        this.log.severe("Failed to schedule loadData, loading now, will probably not work with multiworld plugins");
        loadData();
      }

      this.log.info("Events registered");

      this.beenEnabled = true;
    }
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if ((sender instanceof Player))
    {
      Player player = (Player)sender;
      if (player.isOp())
      {
        if (label.equals("cblock"))
        {
          if (args[0].equals("reload"))
          {
            loadConfig();
            System.out.println("Config reloaded");
            player.sendMessage("Config reloaded");
          }
        }
      }
    }
    else if ((sender instanceof ConsoleCommandSender))
    {
      if (label.equals("cblock"))
      {
        if (args[0].equals("reload"))
        {
          loadConfig();
          System.out.println("Config reloaded");
        }
      }
    }

    return true;
  }

  public Config getConfigu()
  {
    return this.config;
  }

  public PermissionHandler getPerm() {
    return this.permissionHandler;
  }

  public CBlock createCBlock(Location l, String o, byte pl)
  {
    CBlock c = new CBlock(this, l, o, pl);
    this.blocks.add(c);
    return c;
  }

  public CBlock destroyCBlock(Location l, boolean drops)
  {
    CBlock block = getCBlock(l);
    if (block == null) return block;
    if (drops) {
      block.destroy();
    }
    else {
      block.destroyWithOutDrops();
    }
    this.blocks.remove(block);
    saveData();
    return block;
  }
  public CBlock destroyCBlock(Location l) {
    CBlock block = getCBlock(l);
    if (block == null) return block;
    block.destroy();
    this.blocks.remove(block);
    saveData();
    return block;
  }

  public CBlock getCBlock(Location l) {
    for (Iterator i = this.blocks.iterator(); i.hasNext(); ) {
      CBlock block = (CBlock)i.next();
      if (Util.locEquals(block.getLoc(), l)) return block;
    }
    return null;
  }

  public boolean isControlBlock(Location l) {
    return getCBlock(l) != null;
  }

  public boolean isControlledBlock(Location l)
  {
    return getControllerBlockFor(null, l, null, null) != null;
  }

  public boolean isControlledBlock(Location l, Material m)
  {
    return getControllerBlockFor(null, l, m, null) != null;
  }

  public CBlock getControllerBlockFor(CBlock c, Location l, Material m, Boolean o)
  {
    for (Iterator i = this.blocks.iterator(); i.hasNext(); ) {
      CBlock block = (CBlock)i.next();

      if ((c != block) && 
        ((m == null) || (m.equals(block.getType()))) && 
        ((o == null) || (o.equals(Boolean.valueOf(block.isOn())))) && 
        (block.hasBlock(l)))
        return block;
    }
    return null;
  }

  public CBlock moveControllerBlock(CBlock c, Location l)
  {
    Iterator oldBlockDescs = c.getBlocks();
    CBlock newC = createCBlock(l, c.getOwner(), c.protectedLevel);
    newC.setType(c.getType());
    if (c.isOn())
    {
      while (oldBlockDescs.hasNext())
      {
        newC.addBlock(((BlockDesc)oldBlockDescs.next()).blockLoc.getBlock());
      }
      destroyCBlock(c.getLoc(), false);
      return newC;
    }
    return null;
  }

  private void checkPluginDataDir()
  {
    this.log.debug("Checking plugin data directory " + getDataFolder());
    File dir = getDataFolder();
    if (!dir.isDirectory()) {
      this.log.debug("Isn't a directory");
      if (!dir.mkdir()) {
        this.log.severe("Couldn't create plugin data directory " + getDataFolder());
        return;
      }
    }
  }

  public void loadData() {
    int v = 1;
    String s = "";
    Integer i = Integer.valueOf(0);
    Integer l = Integer.valueOf(1);
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(getDataFolder() + "/ControllerBlock.dat"), "UTF-8"));
      String version = in.readLine().trim();
      if (version.equals("# v2"))
        v = 2;
      else if (version.equals("# v3"))
        v = 3;
      else if (version.equals("# v4"))
        v = 4;
      else {
        l = Integer.valueOf(l.intValue() - 1);
      }
      while ((s = in.readLine()) != null) {
        l = Integer.valueOf(l.intValue() + 1);
        if (!s.trim().isEmpty()) {
          CBlock newBlock = new CBlock(this, v, s.trim());
          if (newBlock.getLoc() != null) {
            this.blocks.add(newBlock);
            i = Integer.valueOf(i.intValue() + 1);
          } else {
            this.log.severe("Error loading ControllerBlock on line " + l);
          }
        }
      }
      in.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    catch (FileNotFoundException localFileNotFoundException) {
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    this.log.info("Loaded v" + v + " data - " + i + " ControllerBlocks loaded");
  }

  public void saveData() {
    this.log.debug("Saving ControllerBlock data");
    String dump = "# v4";
    for (Iterator i = this.blocks.iterator(); i.hasNext(); ) {
      CBlock cblock = (CBlock)i.next();
      dump = dump + "\n" + cblock.serialize();
    }
    try {
      Writer out = new OutputStreamWriter(new FileOutputStream(getDataFolder() + "/" + saveDataFile + ".tmp"), "UTF-8");
      out.write(dump);
      out.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      this.log.severe("ERROR: Couldn't open the file to write ControllerBlock data to!");
      this.log.severe("       Check your server installation has write access to " + getDataFolder());
      e.printStackTrace();
    } catch (IOException e) {
      this.log.severe("ERROR: Couldn't save ControllerBlock data! Possibly corrupted/incomplete data");
      this.log.severe("       Check if the disk is full, then edit/finish editing a ControllerBlock");
      this.log.severe("       in game to try to save again.");
      e.printStackTrace();
    }

    File newData = new File(getDataFolder() + "/" + saveDataFile + ".tmp");
    File curData = new File(getDataFolder() + "/" + saveDataFile);
    if (!newData.renameTo(curData))
    {
      if (!curData.delete()) {
        this.log.warning("Couldn't delete old save data during fallback, will probably error next");
      }
      if (!newData.renameTo(curData)) {
        this.log.severe("ERROR: Couldn't move temporary save file over current save file");
        this.log.severe("       Check that your server installation has write access to " + getDataFolder() + "/" + saveDataFile);
      }
    }
  }

  public Material getCBlockType()
  {
    return this.CBlockType;
  }

  public Material getSemiProtectedCBlockType() {
    return this.semiProtectedCBlockType;
  }

  public Material getUnProtectedCBlockType() {
    return this.unProtectedCBlockType;
  }

  public boolean isValidMaterial(Material m) {
    if (!m.isBlock()) return false;
    Iterator i = this.DisallowedTypesAll.iterator();
    while (i.hasNext()) {
      if (((Material)i.next()).equals(m)) return false;
    }
    return true;
  }

  public boolean isUnprotectedMaterial(Material m)
  {
    if (!m.isBlock()) return false;
    Iterator i = this.UnprotectedBlocks.iterator();
    while (i.hasNext())
    {
      if (((Material)i.next()).equals(m))
      {
        return true;
      }
    }
    return false;
  }

  private void loadError(String cmd, String arg, Integer line, String def)
  {
    if (def.length() != 0)
      def = "defaulting to " + def;
    else {
      def = "it has been skipped";
    }
    this.log.warning("Couldn't parse " + cmd + " " + arg + " on line " + line + ", " + def);
  }

  private void loadConfig()
  {
    Integer oldConfigLine = Integer.valueOf(-1);
    Integer l = Integer.valueOf(0);
    ConfigSections c = ConfigSections.oldConfig;
    List configText = new ArrayList();
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(getDataFolder() + "/" + configFile), "UTF-8"));
      String s;
      while ((s = in.readLine()) != null)
      {
        String s;
        configText.add(s.trim());
        l = Integer.valueOf(l.intValue() + 1);
        if ((s.trim().isEmpty()) || 
          (s.startsWith("#"))) continue;
        if (s.toLowerCase().trim().equals("[general]")) {
          c = ConfigSections.general;
        }
        else if (s.toLowerCase().trim().equals("[adminplayers]")) {
          c = ConfigSections.adminPlayers;
        }
        else if (s.toLowerCase().trim().equals("[disallowed]")) {
          c = ConfigSections.disallowedAll;
        }
        else if (s.toLowerCase().trim().equals("[unprotected]")) {
          c = ConfigSections.unprotectedBlocks;
        }
        else if (c.equals(ConfigSections.general)) {
          String[] line = s.split("=", 2);
          if (line.length >= 2) {
            String cmd = line[0].toLowerCase();
            String arg = line[1];
            if (cmd.equals("ControllerBlockType".toLowerCase())) {
              this.CBlockType = Material.getMaterial(arg);
              if (this.CBlockType == null) {
                loadError("ControllerBlockType", arg, l, "IRON_BLOCK");
                this.CBlockType = Material.IRON_BLOCK;
              }
              this.config.setOpt(Config.Option.ControllerBlockType, this.CBlockType);
            }
            else if (cmd.equals("SemiProtectedControllerBlockType".toLowerCase())) {
              this.semiProtectedCBlockType = Material.getMaterial(arg);
              if (this.semiProtectedCBlockType == null) {
                loadError("SemiProtectedControllerBlockType", arg, l, "GOLD_BLOCK");
                this.semiProtectedCBlockType = Material.GOLD_BLOCK;
              }
              this.config.setOpt(Config.Option.SemiProtectedControllerBlockType, this.semiProtectedCBlockType);
            }
            else if (cmd.equals("UnProtectedControllerBlockType".toLowerCase())) {
              this.unProtectedCBlockType = Material.getMaterial(arg);
              if (this.unProtectedCBlockType == null) {
                loadError("UnProtectedControllerBlockType", arg, l, "DIAMOND_BLOCK");
                this.unProtectedCBlockType = Material.DIAMOND_BLOCK;
              }
              this.config.setOpt(Config.Option.UnProtectedControllerBlockType, this.unProtectedCBlockType);
            }
            else if (cmd.equals("QuickRedstoneCheck".toLowerCase())) {
              this.config.setOpt(Config.Option.QuickRedstoneCheck, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
            else if (cmd.equals("BlockProtectMode".toLowerCase())) {
              this.config.setOpt(Config.Option.BlockProtectMode, BlockProtectMode.valueOf(arg.toLowerCase()));
            }
            else if (cmd.equals("BlockEditProtectMode".toLowerCase())) {
              this.config.setOpt(Config.Option.BlockEditProtectMode, BlockProtectMode.valueOf(arg.toLowerCase()));
            }
            else if (cmd.equals("BlockPhysicsProtectMode".toLowerCase())) {
              this.config.setOpt(Config.Option.BlockPhysicsProtectMode, BlockProtectMode.valueOf(arg.toLowerCase()));
            }
            else if (cmd.equals("BlockFlowProtectMode".toLowerCase())) {
              this.config.setOpt(Config.Option.BlockFlowProtectMode, BlockProtectMode.valueOf(arg.toLowerCase()));
            }
            else if (cmd.equals("DisableEditDupeProtection".toLowerCase())) {
              this.config.setOpt(Config.Option.DisableEditDupeProtection, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
            else if (cmd.equals("PistonProtection".toLowerCase())) {
              this.config.setOpt(Config.Option.PistonProtection, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
            else if (cmd.equals("MaxBlocksPerController".toLowerCase())) {
              this.config.setOpt(Config.Option.MaxBlocksPerController, Integer.valueOf(Integer.parseInt(arg)));
            }
            else if (cmd.equals("MaxDistanceFromController".toLowerCase())) {
              this.config.setOpt(Config.Option.MaxDistanceFromController, Integer.valueOf(Integer.parseInt(arg)));
            }
            else if (cmd.equals("DisableNijikokunPermissions".toLowerCase())) {
              this.config.setOpt(Config.Option.DisableNijikokunPermissions, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
            else if (cmd.equals("ServerOpIsAdmin".toLowerCase())) {
              this.config.setOpt(Config.Option.ServerOpIsAdmin, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
            else if (cmd.equals("AnyoneCanCreate".toLowerCase())) {
              this.config.setOpt(Config.Option.AnyoneCanCreate, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
            else if (cmd.equals("AnyoneCanModifyOther".toLowerCase())) {
              this.config.setOpt(Config.Option.AnyoneCanModifyOther, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
            else if (cmd.equals("AnyoneCanDestroyOther".toLowerCase())) {
              this.config.setOpt(Config.Option.AnyoneCanDestroyOther, Boolean.valueOf(Boolean.parseBoolean(arg)));
            }
          }
        }
        else if (c.equals(ConfigSections.adminPlayers)) {
          this.permissionHandler.addBuiltinAdminPlayer(s.trim());
        }
        else if (c.equals(ConfigSections.disallowedAll)) {
          Material m = Material.getMaterial(s.trim());
          if (m == null)
            loadError("disallowed type", s.trim(), l, "");
          else
            this.DisallowedTypesAll.add(m);
        }
        else if (c.equals(ConfigSections.unprotectedBlocks)) {
          Material m = Material.getMaterial(s.trim());
          if (m == null)
            loadError("disallowed type", s.trim(), l, "");
          else {
            this.UnprotectedBlocks.add(m);
          }

        }
        else if (c.equals(ConfigSections.oldConfig)) {
          if (oldConfigLine.intValue() == -1) {
            this.CBlockType = Material.getMaterial(s.trim());
            if (this.CBlockType == null) {
              this.log.warning("Couldn't parse ControllerBlock type " + s.trim() + ", defaulting to IRON_BLOCK");
              this.CBlockType = Material.IRON_BLOCK;
            }
            this.config.setOpt(Config.Option.ControllerBlockType, this.CBlockType);
            oldConfigLine = Integer.valueOf(oldConfigLine.intValue() + 1);
          } else {
            Material m = Material.getMaterial(s.trim());
            if (m == null) {
              this.log.warning("Couldn't parse disallowed type " + s.trim() + ", it has been skipped");
            } else {
              this.DisallowedTypesAll.add(m);
              oldConfigLine = Integer.valueOf(oldConfigLine.intValue() + 1);
            }
          }
        }
      }
      writeConfig(configText);
      in.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    catch (FileNotFoundException e)
    {
      this.log.warning("No config found, using defaults, writing defaults out to " + configFile);
      writeConfig(null);
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.CBlockType = ((Material)this.config.getOpt(Config.Option.ControllerBlockType));
    this.log.info("Using " + this.CBlockType + " (" + this.CBlockType.getId() + ") as ControllerBlock, loaded " + this.DisallowedTypesAll.size() + " disallowed types from config");
  }

  private String writePatch(ConfigSections c)
  {
    String dump = "";
    if (c == null) return dump;
    if (c.equals(ConfigSections.general)) {
      if (!this.config.hasOption(Config.Option.ControllerBlockType)) {
        dump = dump + "\n";
        dump = dump + "# ControllerBlockType is the material allowed of new ControllerBlocks\n";
        dump = dump + "# Doesn't affect already assigned ControllerBlocks\n";
        dump = dump + "ControllerBlockType=" + this.config.getOpt(Config.Option.ControllerBlockType) + "\n";
      }
      if (!this.config.hasOption(Config.Option.SemiProtectedControllerBlockType)) {
        dump = dump + "\n";
        dump = dump + "# SemiProtectedControllerBlockType is the material that semi-protected\n";
        dump = dump + "# Controller Blocks are made from, this block will turn on in a protected\n";
        dump = dump + "# state, but when turned off, blocks controlled won't disappear, instead\n";
        dump = dump + "# they lose their protection and can be destroyed\n";
        dump = dump + "SemiProtectedControllerBlockType=" + this.config.getOpt(Config.Option.SemiProtectedControllerBlockType) + "\n";
      }
      if (!this.config.hasOption(Config.Option.UnProtectedControllerBlockType)) {
        dump = dump + "\n";
        dump = dump + "# UnProtectedControllerBlockType is the material that unprotected\n";
        dump = dump + "# Controller Blocks are made from, blocks controlled by this will create\n";
        dump = dump + "# when turned on, but won't disappear when turned off, much like the\n";
        dump = dump + "# semi-protected controlled blocks, however, blocks controlled have no\n";
        dump = dump + "# protection against being broken even in the on state\n";
        dump = dump + "UnProtectedControllerBlockType=" + this.config.getOpt(Config.Option.UnProtectedControllerBlockType) + "\n";
      }
      if (!this.config.hasOption(Config.Option.QuickRedstoneCheck)) {
        dump = dump + "\n";
        dump = dump + "# QuickRedstoneCheck to false enables per-tick per-controllerblock isBlockPowered() checks\n";
        dump = dump + "# This is potentially laggier, but blocks can be powered like regular redstone blocks\n";
        dump = dump + "# If set to true, wire needs to be run on top of the controller block\n";
        dump = dump + "QuickRedstoneCheck=" + this.config.getOpt(Config.Option.QuickRedstoneCheck) + "\n";
      }
      if (!this.config.hasOption(Config.Option.BlockProtectMode)) {
        dump = dump + "\n";
        dump = dump + "# BlockProtectMode changes how we handle destroying controlled blocks\n";
        dump = dump + "# It has 3 modes:\n";
        dump = dump + "# protect - default, tries to prevent controlled blocks from being destroyed\n";
        dump = dump + "# remove - removes controlled blocks from controller if destroyed\n";
        dump = dump + "# none - don't do anything, this effectively makes controlled blocks dupable\n";
        dump = dump + "BlockProtectMode=" + this.config.getOpt(Config.Option.BlockProtectMode) + "\n";
      }
      if (!this.config.hasOption(Config.Option.BlockPhysicsProtectMode)) {
        dump = dump + "\n";
        dump = dump + "# BlockPhysicsProtectMode changes how we handle changes against controlled blocks\n";
        dump = dump + "# It has 3 modes:\n";
        dump = dump + "# protect - default, stops physics interactions with controlled blocks\n";
        dump = dump + "# remove - removes controlled blocks from controller if changed\n";
        dump = dump + "# none - don't do anything, could have issues with some blocks\n";
        dump = dump + "BlockPhysicsProtectMode=" + this.config.getOpt(Config.Option.BlockPhysicsProtectMode) + "\n";
      }
      if (!this.config.hasOption(Config.Option.BlockFlowProtectMode)) {
        dump = dump + "\n";
        dump = dump + "# BlockFlowProtectMode changes how we handle water/lava flowing against controlled blocks\n";
        dump = dump + "# It has 3 modes:\n";
        dump = dump + "# protect - default, tries to prevent controlled blocks from being interacted\n";
        dump = dump + "# remove - removes controlled blocks from controller if flow event on it\n";
        dump = dump + "# none - don't do anything, things that drop when flowed over can be dupable\n";
        dump = dump + "BlockFlowProtectMode=" + this.config.getOpt(Config.Option.BlockFlowProtectMode) + "\n";
      }
      if (!this.config.hasOption(Config.Option.DisableEditDupeProtection)) {
        dump = dump + "\n";
        dump = dump + "# DisableEditDupeProtection set to true disables all the checks for changes while in\n";
        dump = dump + "# edit mode, this will make sure blocks placed in a spot will always be in that spot\n";
        dump = dump + "# even if they get removed by some kind of physics/flow event in the meantime\n";
        dump = dump + "DisableEditDupeProtection=" + this.config.getOpt(Config.Option.DisableEditDupeProtection) + "\n";
      }
      if (!this.config.hasOption(Config.Option.PistonProtection))
      {
        dump = dump + "\n";
        dump = dump + "# PistonProtection set to true disables the ability of Pistons to move\n";
        dump = dump + "# ControllerBlocks or controlled Blocks.\n";
        dump = dump + "PistonProtection=" + this.config.getOpt(Config.Option.PistonProtection) + "\n";
      }
      if (!this.config.hasOption(Config.Option.MaxDistanceFromController)) {
        dump = dump + "\n";
        dump = dump + "# MaxDistanceFromController sets how far away controlled blocks are allowed\n";
        dump = dump + "# to be attached and controlled to a controller block - 0 for infinte/across worlds\n";
        dump = dump + "MaxDistanceFromController=" + this.config.getOpt(Config.Option.MaxDistanceFromController) + "\n";
      }
      if (!this.config.hasOption(Config.Option.MaxBlocksPerController)) {
        dump = dump + "\n";
        dump = dump + "# MaxControlledBlocksPerController sets how many blocks are allowed to be attached\n";
        dump = dump + "# to a single controller block - 0 for infinite\n";
        dump = dump + "MaxBlocksPerController=" + this.config.getOpt(Config.Option.MaxBlocksPerController) + "\n";
      }
      if (!this.config.hasOption(Config.Option.DisableNijikokunPermissions)) {
        dump = dump + "\n";
        dump = dump + "# Nijikokun Permissions support\n";
        dump = dump + "# The nodes for permissions are:\n";
        dump = dump + "# controllerblock.admin - user isn't restricted by block counts or distance, able to\n";
        dump = dump + "#                         create/modify/destroy other users controllerblocks\n";
        dump = dump + "# controllerblock.create - user is allowed to setup controllerblocks\n";
        dump = dump + "# controllerblock.modifyOther - user is allowed to modify other users controllerblocks\n";
        dump = dump + "# controllerblock.destroyOther - user is allowed to destroy other users controllerblocks\n";
        dump = dump + "#\n";
        dump = dump + "# DisableNijikokunPermissions will disable any lookups against Permissions if you\n";
        dump = dump + "# do have it installed, but want to disable this plugins use of it anyway\n";
        dump = dump + "# Note: You don't have to do this, the plugin isn't dependant on Permissions\n";
        dump = dump + "DisableNijikokunPermissions=" + this.config.getOpt(Config.Option.DisableNijikokunPermissions) + "\n";
      }
      if (!this.config.hasOption(Config.Option.ServerOpIsAdmin)) {
        dump = dump + "\n";
        dump = dump + "# Users listed in ops.txt (op through server console) counts as an admin\n";
        dump = dump + "ServerOpIsAdmin=" + this.config.getOpt(Config.Option.ServerOpIsAdmin) + "\n";
      }
      if (!this.config.hasOption(Config.Option.AnyoneCanCreate)) {
        dump = dump + "\n";
        dump = dump + "# Everyone on the server can create new ControllerBlocks\n";
        dump = dump + "AnyoneCanCreate=" + this.config.getOpt(Config.Option.AnyoneCanCreate) + "\n";
      }
      if (!this.config.hasOption(Config.Option.AnyoneCanModifyOther)) {
        dump = dump + "\n";
        dump = dump + "# Everyone can modify everyone elses ControllerBlocks\n";
        dump = dump + "AnyoneCanModifyOther=" + this.config.getOpt(Config.Option.AnyoneCanModifyOther) + "\n";
      }
      if (!this.config.hasOption(Config.Option.AnyoneCanDestroyOther)) {
        dump = dump + "\n";
        dump = dump + "# Everyone can destroy everyone elses ControllerBlocks\n";
        dump = dump + "AnyoneCanDestroyOther=" + this.config.getOpt(Config.Option.AnyoneCanDestroyOther) + "\n";
      }
    }
    if (dump.length() != 0) {
      dump = dump + "\n";
    }
    return dump;
  }

  private void writeConfig(List<String> prevConfig) {
    String dump = "";
    if (prevConfig == null) {
      dump = "# ControllerBlock configuration file\n";
      dump = dump + "\n";
      dump = dump + "# Blank lines and lines starting with # are ignored\n";
      dump = dump + "# Material names can be found: http://javadoc.lukegb.com/Bukkit/d7/dd9/namespaceorg_1_1bukkit.html#ab7fa290bb19b9a830362aa88028ec80a\n";
      dump = dump + "\n";
    }
    boolean hasGeneral = false;
    boolean hasAdminPlayers = false;
    boolean hasDisallowed = false;
    ConfigSections c = null;

    if (prevConfig != null) {
      Iterator pci = prevConfig.listIterator();
      while (pci.hasNext()) {
        String line = (String)pci.next();
        if (line.toLowerCase().trim().equals("[general]")) {
          dump = dump + writePatch(c);
          c = ConfigSections.general;
          hasGeneral = true;
        } else if (line.toLowerCase().trim().equals("[adminplayers]")) {
          dump = dump + writePatch(c);
          c = ConfigSections.adminPlayers;
          hasAdminPlayers = true;
        } else if (line.toLowerCase().trim().equals("[disallowed]")) {
          dump = dump + writePatch(c);
          c = ConfigSections.disallowedAll;
          hasDisallowed = true;
        }
        dump = dump + line + "\n";
      }
      pci = null;
      dump = dump + writePatch(c);
    }

    if (!hasGeneral) {
      dump = dump + "[general]\n";
      dump = dump + writePatch(ConfigSections.general);
      dump = dump + "\n";
    }
    if (!hasAdminPlayers) {
      dump = dump + "[adminPlayers]\n";
      dump = dump + "# One name per line, users listed here are admins, and can\n";
      dump = dump + "# create/modify/destroy all ControllerBlocks on the server\n";
      dump = dump + "# Block restrictions don't apply to admins\n";
      dump = dump + "\n";
    }
    if (!hasDisallowed) {
      dump = dump + "[disallowed]\n";
      dump = dump + "# Add disallowed blocks here, one Material per line.\n";
      dump = dump + "# Item IDs higher than 255 are excluded automatically due to failing Material.isBlock() check\n";
      dump = dump + "#RED_ROSE\n#YELLOW_FLOWER\n#RED_MUSHROOM\n#BROWN_MUSHROOM\n";
      dump = dump + "\n";
      Iterator i = this.DisallowedTypesAll.listIterator();
      while (i.hasNext()) {
        dump = dump + i.next() + "\n";
      }
      dump = dump + "[unprotected]\n";
      dump = dump + "# Add unprotected blocks here, one Material per line.\n";
      dump = dump + "# Item IDs higher than 255 are excluded automatically due to failing Material.isBlock() check\n";
      dump = dump + "# These Blocks ARE allowed to be pushed by Pistons and to be used with (semi) unprotected CBlocks.\n";
      dump = dump + "#RED_ROSE\n#YELLOW_FLOWER\n#RED_MUSHROOM\n#BROWN_MUSHROOM\n";
      dump = dump + "\n";
      i = this.UnprotectedBlocks.listIterator();
      while (i.hasNext()) {
        dump = dump + i.next() + "\n";
      }
    }
    try
    {
      Writer out = new OutputStreamWriter(new FileOutputStream(getDataFolder() + "/" + configFile), "UTF-8");
      out.write(dump);
      out.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void run()
  {
    loadData();
  }
}