package net.servfire.hellfire.bukkit.ControllerBlock;

import java.util.HashMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;

public class CPlayerListener extends PlayerListener
{
  ControllerBlock parent;

  public CPlayerListener(ControllerBlock parent)
  {
    this.parent = parent;
  }
  public boolean isRedstone(Block b) {
    Material t = b.getType();

    return (t.equals(Material.REDSTONE_WIRE)) || 
      (t.equals(Material.REDSTONE_TORCH_ON)) || 
      (t.equals(Material.REDSTONE_TORCH_OFF));
  }

  public void onPlayerInteract(PlayerInteractEvent e)
  {
    Player player = e.getPlayer();
    Block block = e.getClickedBlock();
    if ((e.getItem() != null) && (player.getGameMode().equals(GameMode.SURVIVAL)))
    {
      if ((e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && (e.getItem().getType().equals(Material.STICK)))
      {
        if (this.parent.movingCBlock.containsKey(player.getName()))
        {
          CBlock c = (CBlock)this.parent.movingCBlock.get(player.getName());
          if (c.getLoc().getBlock().getType().equals(block.getType()))
          {
            this.parent.moveHere.put(player.getName(), block.getLocation());
            if (this.parent.moveControllerBlock(c, block.getLocation()) != null)
            {
              player.sendMessage("ControllerBlock successfully moved");
            }
            else
            {
              player.sendMessage("The ControllerBlock needs to be turned off");
            }
          }
        }
        else
        {
          player.sendMessage("First Left-Click the ControllerBlock you want to move");
        }
      }
    }
  }
}