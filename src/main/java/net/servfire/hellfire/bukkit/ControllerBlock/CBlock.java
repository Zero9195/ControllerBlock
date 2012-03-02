package net.servfire.hellfire.bukkit.ControllerBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.RedstoneWire;

public class CBlock
{
  private Location blockLocation = null;
  private Material blockType = null;
  private List<BlockDesc> placedBlocks = new ArrayList();
  private String owner = null;

  private ControllerBlock parent = null;
  private boolean on = false;
  private boolean edit = false;
  public byte protectedLevel = 0;

  public CBlock(ControllerBlock p, Location l, String o, byte pl) {
    this.parent = p;
    this.blockLocation = l;
    this.owner = o;
    this.protectedLevel = pl;
  }

  public ControllerBlock getParent() {
    return this.parent;
  }

  public String getOwner() {
    return this.owner;
  }

  public Material getType() {
    return this.blockType;
  }

  public void setType(Material m) {
    this.blockType = m;
  }

  public Location getLoc() {
    return this.blockLocation;
  }

  public Iterator<BlockDesc> getBlocks() {
    return this.placedBlocks.iterator();
  }

  public boolean addBlock(Block b) {
    if (b.getType().equals(this.blockType)) {
      Location bloc = b.getLocation();

      if (this.placedBlocks.size() == 0) {
        this.placedBlocks.add(new BlockDesc(bloc, Byte.valueOf(b.getData())));
        return true;
      }
      ListIterator i = this.placedBlocks.listIterator();
      while (i.hasNext()) {
        BlockDesc loc = (BlockDesc)i.next();
        if (bloc.getBlockY() > loc.blockLoc.getBlockY()) {
          i.previous();
          i.add(new BlockDesc(bloc, Byte.valueOf(b.getData())));
          return true;
        }
      }
      this.placedBlocks.add(new BlockDesc(bloc, Byte.valueOf(b.getData())));
      return true;
    }

    return false;
  }

  public boolean delBlock(Block b) {
    Location u = b.getLocation();
    for (Iterator i = this.placedBlocks.iterator(); i.hasNext(); ) {
      Location t = ((BlockDesc)i.next()).blockLoc;
      if (t.equals(u)) {
        i.remove();
        CBlock check = this.parent.getControllerBlockFor(this, u, null, Boolean.valueOf(true));
        if (check != null) {
          b.setType(check.blockType);
          b.setData(check.getBlock(u).blockData);
        }
        return true;
      }
    }
    return false;
  }

  public int numBlocks() {
    return this.placedBlocks.size();
  }

  public BlockDesc getBlock(Location l) {
    Iterator i = this.placedBlocks.iterator();
    while (i.hasNext()) {
      BlockDesc d = (BlockDesc)i.next();
      if (d.blockLoc.equals(l)) return d;
    }
    return null;
  }

  public boolean hasBlock(Location l) {
    return getBlock(l) != null;
  }

  public void updateBlock(Block b)
  {
    Iterator i = this.placedBlocks.iterator();
    while (i.hasNext()) {
      BlockDesc d = (BlockDesc)i.next();
      if (d.blockLoc.equals(b.getLocation())) {
        d.blockData = b.getState().getData().getData();
        return;
      }
    }
  }

  public boolean isBeingEdited() {
    return this.edit;
  }

  public void editBlock(boolean b) {
    this.edit = b;
    if (this.edit) {
      turnOn();
    } else {
      this.parent.saveData();
      doRedstoneCheck();
    }
  }

  public void destroyWithOutDrops()
  {
    turnOff();
  }
  public void destroy() {
    turnOff();
    int i = this.placedBlocks.size();
    int j = 0;
    while (i > 0) {
      if (i > 64) {
        j = 64;
        i -= 64;
      } else {
        j = i;
        i -= i;
      }
      this.blockLocation.getWorld().dropItemNaturally(this.blockLocation, new ItemStack(this.blockType, j));
    }
  }

  public boolean isOn() {
    return this.on;
  }

  public void doRedstoneCheck() {
    Block check = Util.getBlockAtLocation(this.blockLocation).getRelative(BlockFace.UP);
    doRedstoneCheck(check.getState());
  }

  public void doRedstoneCheck(BlockState s) {
    if (isBeingEdited()) return;
    if (s.getType().equals(Material.REDSTONE_TORCH_ON))
      turnOff();
    else if (s.getType().equals(Material.REDSTONE_TORCH_OFF))
      turnOn();
    else if (s.getType().equals(Material.REDSTONE_WIRE)) {
      if (((RedstoneWire)s.getData()).isPowered())
        turnOff();
      else
        turnOn();
    }
    else if (s.getType().equals(Material.AIR))
      turnOn();
  }

  public void turnOff()
  {
    Iterator i = this.placedBlocks.iterator();
    while (i.hasNext()) {
      BlockDesc d = (BlockDesc)i.next();
      Location loc = d.blockLoc;
      CBlock check = this.parent.getControllerBlockFor(this, loc, null, Boolean.valueOf(true));
      Block cur = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      boolean applyPhysics = true;

      if (check != null)
        cur.setTypeIdAndData(check.blockType.getId(), check.getBlock(loc).blockData, applyPhysics);
      else if (this.protectedLevel == 0) {
        cur.setType(Material.AIR);
      }
    }
    this.on = false;
  }

  public void turnOn() {
    for (Iterator i = this.placedBlocks.iterator(); i.hasNext(); ) {
      BlockDesc b = (BlockDesc)i.next();
      Location loc = b.blockLoc;
      Block cur = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
      boolean applyPhysics = true;
      if (this.protectedLevel == 0)
      {
        if ((cur.getType().equals(Material.SAND)) || (cur.getType().equals(Material.GRAVEL)) || (cur.getType().equals(Material.TORCH)) || (cur.getType().equals(Material.REDSTONE_TORCH_OFF)) || (cur.getType().equals(Material.REDSTONE_TORCH_ON)) || (cur.getType().equals(Material.RAILS)) || (cur.getType().equals(Material.LADDER)) || (cur.getType().equals(Material.GRAVEL)) || (cur.getType().equals(Material.POWERED_RAIL)) || (cur.getType().equals(Material.DETECTOR_RAIL)))
        {
          applyPhysics = false;
        }
      }
      cur.setTypeIdAndData(this.blockType.getId(), b.blockData, applyPhysics);
    }
    this.on = true;
  }

  public void turnOn(Location l) {
    Iterator i = this.placedBlocks.iterator();
    while (i.hasNext()) {
      BlockDesc b = (BlockDesc)i.next();
      if (l.equals(b.blockLoc)) {
        Block cur = Util.getBlockAtLocation(l);
        boolean applyPhysics = true;
        if (this.protectedLevel == 0)
        {
          applyPhysics = false;
        }
        cur.setTypeIdAndData(this.blockType.getId(), b.blockData, applyPhysics);
      }
    }
  }

  public CBlock(ControllerBlock p, int version, String s)
  {
    this.parent = p;
    String[] args = s.split(",");

    if (((version < 3) && (args.length < 4)) || (
      (version >= 3) && (args.length < 5)))
    {
      this.parent.log.severe("ERROR: Invalid ControllerBlock description in data file, skipping");
      return;
    }

    if (version >= 4) {
      this.blockLocation = parseLocation(p.getServer(), args[0], args[1], args[2], args[3]);
      this.parent.log.debug("CB Location: " + Util.formatLocation(this.blockLocation));
    }

    this.blockType = Material.getMaterial(args[4]);
    int i;
    int i;
    if (version >= 3) {
      this.owner = args[5];
      i = 6;
    } else {
      this.owner = null;
      i = 5;
    }

    this.protectedLevel = 0;
    if (i < args.length) {
      if (args[i].equals("protected")) {
        this.protectedLevel = 0;
        i++;
      }
      if (args[i].equals("semi-protected")) {
        this.protectedLevel = 1;
        i++;
      }
      else if (args[i].equals("unprotected")) {
        this.protectedLevel = 2;
        i++;
      }
    }

    while (i < args.length)
      if (version >= 4)
        if (args.length - i >= 5) {
          this.placedBlocks.add(new BlockDesc(parseLocation(p.getServer(), args[(i++)], args[(i++)], args[(i++)], args[(i++)]), Byte.valueOf(Byte.parseByte(args[(i++)]))));
        } else {
          this.parent.log.severe("ERROR: Block description in save file is corrupt");
          return;
        }
  }

  public String serialize()
  {
    String result = loc2str(this.blockLocation);
    result = result + "," + this.blockType;
    result = result + "," + this.owner;

    if (this.protectedLevel == 1)
      result = result + ",semi-protected";
    else if (this.protectedLevel == 2) {
      result = result + ",unprotected";
    }
    Iterator i = this.placedBlocks.iterator();
    while (i.hasNext()) {
      BlockDesc b = (BlockDesc)i.next();
      result = result + "," + loc2str(b.blockLoc);
      result = result + "," + Byte.toString(b.blockData);
    }
    return result;
  }

  public Location parseLocation(Server server, String worldName, String X, String Y, String Z)
  {
    return new Location(server.getWorld(worldName), Integer.parseInt(X), Integer.parseInt(Y), Integer.parseInt(Z));
  }

  public String loc2str(Location l)
  {
    if (l == null) {
      this.parent.log.severe("ERROR: null location while trying to save CBlock at " + loc2str(this.blockLocation));
    }
    if (l.getWorld() == null) {
      this.parent.log.severe("ERROR: null world in location while trying to save CBlock");
    }
    return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
  }
}