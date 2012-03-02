package net.servfire.hellfire.bukkit.ControllerBlock;

import org.bukkit.Location;

public class BlockDesc
{
  public Location blockLoc;
  public byte blockData;

  public BlockDesc(Location l, Byte b)
  {
    this.blockLoc = l;
    this.blockData = b.byteValue();
  }
}
