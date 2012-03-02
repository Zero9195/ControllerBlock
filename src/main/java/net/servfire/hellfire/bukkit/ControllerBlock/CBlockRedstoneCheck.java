package net.servfire.hellfire.bukkit.ControllerBlock;

import java.util.Iterator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class CBlockRedstoneCheck
  implements Runnable
{
  private ControllerBlock parent = null;

  public CBlockRedstoneCheck(ControllerBlock c) {
    this.parent = c;
  }

  public void run()
  {
    Iterator i = this.parent.blocks.iterator();
    while (i.hasNext()) {
      CBlock c = (CBlock)i.next();
      if (!c.isBeingEdited()) {
        Block b = Util.getBlockAtLocation(c.getLoc());

        boolean on = c.isOn();
        if (b.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH_ON)) {
          if (on)
            c.turnOff();
        }
        else if (b.getRelative(BlockFace.UP).getType().equals(Material.REDSTONE_TORCH_OFF)) {
          if (!on) {
            c.turnOn();
          }
        }
        else if ((on) && (b.isBlockPowered()))
          c.turnOff();
        else if ((!on) && (!b.isBlockPowered()))
          c.turnOn();
      }
    }
  }
}