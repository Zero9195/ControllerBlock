package net.servfire.hellfire.bukkit.ControllerBlock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

public class CBlockListener extends BlockListener
  implements Runnable
{
  private ControllerBlock parent;

  public CBlockListener(ControllerBlock controllerBlock)
  {
    this.parent = controllerBlock;
  }

  public Player getPlayerEditing(CBlock c) {
    for (Map.Entry e : this.parent.map.entrySet()) {
      if (((CBlock)e.getValue()).equals(c))
        return (Player)e.getKey();
    }
    return null;
  }

  public void removePlayersEditing(CBlock c)
  {
    Player p;
    while ((p = getPlayerEditing(c)) != null)
    {
      Player p;
      this.parent.map.remove(p);
    }
  }

  public boolean isRedstone(Block b) {
    Material t = b.getType();

    return (t.equals(Material.REDSTONE_WIRE)) || 
      (t.equals(Material.REDSTONE_TORCH_ON)) || 
      (t.equals(Material.REDSTONE_TORCH_OFF));
  }

  public void onBlockBreak(BlockBreakEvent e)
  {
    if (e.isCancelled()) return;
    Player player = e.getPlayer();
    Block b = e.getBlock();
    PlayerInventory inv = player.getInventory();
    Material item = inv.getItemInHand().getType();

    if ((player.getGameMode().equals(GameMode.CREATIVE)) && (item.isBlock()))
    {
      CBlock conBlock = (CBlock)this.parent.map.get(player);

      if ((item.equals(Material.WOOD_PICKAXE)) || 
        (item.equals(Material.STONE_PICKAXE)) || 
        (item.equals(Material.IRON_PICKAXE)) || 
        (item.equals(Material.GOLD_PICKAXE)) || 
        (item.equals(Material.DIAMOND_PICKAXE))) {
        return;
      }
      if (conBlock != null)
      {
        if (this.parent.isControlBlock(b.getLocation())) {
          conBlock.editBlock(false);
          this.parent.map.remove(player);

          if (Util.locEquals(conBlock.getLoc(), b.getLocation())) {
            player.sendMessage("Finished editing ControllerBlock");
            e.setCancelled(true);
            return;
          }

          player.sendMessage("Finished editing previous ControllerBlock");
          e.setCancelled(true);
          conBlock = null;
        }

      }

      if (conBlock == null)
      {
        conBlock = this.parent.getCBlock(b.getLocation());
        if (conBlock == null) {
          if (!isRedstone(b.getRelative(BlockFace.UP))) return;
          byte cBType;
          if (b.getType() == this.parent.getCBlockType()) {
            String cBTypeStr = "protected";
            cBType = 0;
          }
          else
          {
            byte cBType;
            if (b.getType() == this.parent.getSemiProtectedCBlockType()) {
              String cBTypeStr = "semi-protected";
              cBType = 1;
            }
            else
            {
              byte cBType;
              if (b.getType() == this.parent.getUnProtectedCBlockType()) {
                String cBTypeStr = "unprotected";
                cBType = 2;
              } else {
                return;
              }
            }
          }
          byte cBType;
          String cBTypeStr;
          if (!this.parent.getPerm().canCreate(player)) {
            player.sendMessage("You're not allowed to create " + cBTypeStr + " ControllerBlocks");
            e.setCancelled(true);
            return;
          }
          if (this.parent.isControlledBlock(b.getLocation())) {
            player.sendMessage("This block is controlled, controlled blocks can't be controllers");
            e.setCancelled(true);
            return;
          }
          conBlock = this.parent.createCBlock(b.getLocation(), player.getName(), cBType);
          player.sendMessage("Created " + cBTypeStr + " controller block");
          e.setCancelled(true);
        }

        if (conBlock == null) return;
        if (!this.parent.getPerm().canModify(player, conBlock)) {
          player.sendMessage("You're not allowed to modify this ControllerBlock");
          e.setCancelled(true);
          return;
        }

        if (item.equals(Material.STICK))
        {
          this.parent.movingCBlock.put(player.getName(), conBlock);
          player.sendMessage("ControllerBlock is registered as the next to move.   Right-Click the position where to move it.");
          e.setCancelled(true);
          return;
        }

        if (conBlock.numBlocks() == 0) {
          if (!this.parent.isValidMaterial(item)) {
            player.sendMessage("Can't set the ControllerBlock type to " + item);
            e.setCancelled(true);
            return;
          }

          if (((conBlock.protectedLevel == 1) || (conBlock.protectedLevel == 2)) && (!this.parent.isUnprotectedMaterial(item)))
          {
            player.sendMessage("The Material is protected, can't use with (semi-)unprotected ControllerBlocks.");
            e.setCancelled(true);
            return;
          }
          conBlock.setType(item);
        }

        if ((item != Material.AIR) && (item != conBlock.getType())) {
          player.sendMessage("This ControllerBlock needs to be edited with " + conBlock.getType());
          e.setCancelled(true);
          return;
        }

        this.parent.map.put(player, conBlock);
        conBlock.editBlock(true);
        player.sendMessage("You're now editing this block with " + conBlock.getType() + " " + Util.formatBlockCount(conBlock));
        e.setCancelled(true);
        return;
      }
    }
    CBlock conBlock = this.parent.getCBlock(b.getLocation());
    if (conBlock != null) {
      if (!this.parent.getPerm().canDestroy(player, conBlock)) {
        player.sendMessage("You're not allowed to destroy this ControllerBlock");
        e.setCancelled(true);
        return;
      }
      conBlock = this.parent.destroyCBlock(b.getLocation());
      if (conBlock != null) {
        player.sendMessage("Destroyed controller block");
        removePlayersEditing(conBlock);
      }
    }

    conBlock = (CBlock)this.parent.map.get(player);
    if ((conBlock != null) && (conBlock.hasBlock(b.getLocation())) && (conBlock.getType().equals(b.getType()))) {
      if (conBlock.delBlock(b)) player.sendMessage("Block removed from controller " + Util.formatBlockCount(conBlock)); 
    }
    else if ((conBlock = this.parent.getControllerBlockFor(null, b.getLocation(), b.getType(), null)) != null)
      switch ($SWITCH_TABLE$net$servfire$hellfire$bukkit$ControllerBlock$BlockProtectMode()[((BlockProtectMode)this.parent.getConfigu().getOpt(Config.Option.BlockProtectMode)).ordinal()]) {
      case 1:
        if ((conBlock.protectedLevel != 0) && ((conBlock.isOn()) || (conBlock.protectedLevel == 2))) break;
        player.sendMessage("This block is controlled by a controller block at " + 
          conBlock.getLoc().getBlockX() + ", " + 
          conBlock.getLoc().getBlockY() + ", " + 
          conBlock.getLoc().getBlockZ());
        e.setCancelled(true);

        break;
      case 2:
        conBlock.delBlock(b);
        break;
      case 3:
      }
  }

  public void onBlockDamage(BlockDamageEvent e)
  {
    Player player = e.getPlayer();

    if ((e.isCancelled()) && (e.getBlock().getType().equals(Material.AIR)))
    {
      CBlock conBlock;
      if ((conBlock = this.parent.destroyCBlock(e.getBlock().getLocation())) != null) {
        player.sendMessage("Destroyed controller block with superpickaxe?");
        removePlayersEditing(conBlock);
      }
    }
    if ((e.isCancelled()) || (player.getGameMode().equals(GameMode.CREATIVE))) return;
    PlayerInventory inv = player.getInventory();
    Material item = inv.getItemInHand().getType();
    Block b = e.getBlock();
    CBlock conBlock = (CBlock)this.parent.map.get(player);

    if ((item.equals(Material.WOOD_PICKAXE)) || 
      (item.equals(Material.STONE_PICKAXE)) || 
      (item.equals(Material.IRON_PICKAXE)) || 
      (item.equals(Material.GOLD_PICKAXE)) || 
      (item.equals(Material.DIAMOND_PICKAXE))) {
      return;
    }
    if (conBlock != null)
    {
      if (this.parent.isControlBlock(b.getLocation())) {
        conBlock.editBlock(false);
        this.parent.map.remove(player);

        if (Util.locEquals(conBlock.getLoc(), b.getLocation())) {
          player.sendMessage("Finished editing ControllerBlock");
          return;
        }

        player.sendMessage("Finished editing previous ControllerBlock");
        conBlock = null;
      }

    }

    if (conBlock == null)
    {
      conBlock = this.parent.getCBlock(b.getLocation());
      if (conBlock == null) {
        if (!isRedstone(b.getRelative(BlockFace.UP))) return;
        byte cBType;
        if (b.getType() == this.parent.getCBlockType()) {
          String cBTypeStr = "protected";
          cBType = 0;
        }
        else
        {
          byte cBType;
          if (b.getType() == this.parent.getSemiProtectedCBlockType()) {
            String cBTypeStr = "semi-protected";
            cBType = 1;
          }
          else
          {
            byte cBType;
            if (b.getType() == this.parent.getUnProtectedCBlockType()) {
              String cBTypeStr = "unprotected";
              cBType = 2;
            } else {
              return;
            }
          }
        }
        byte cBType;
        String cBTypeStr;
        if (!this.parent.getPerm().canCreate(player)) {
          player.sendMessage("You're not allowed to create " + cBTypeStr + " ControllerBlocks");
          return;
        }
        if (this.parent.isControlledBlock(b.getLocation())) {
          player.sendMessage("This block is controlled, controlled blocks can't be controllers");
          return;
        }
        conBlock = this.parent.createCBlock(b.getLocation(), player.getName(), cBType);
        player.sendMessage("Created " + cBTypeStr + " controller block");
      }

      if (conBlock == null) return;
      if (!this.parent.getPerm().canModify(player, conBlock)) {
        player.sendMessage("You're not allowed to modify this ControllerBlock");
        return;
      }

      if (item.equals(Material.STICK))
      {
        this.parent.movingCBlock.put(player.getName(), conBlock);
        player.sendMessage("ControllerBlock is registered as the next to move.   Right-Click the position where to move it.");
        return;
      }

      if (conBlock.numBlocks() == 0) {
        if (!this.parent.isValidMaterial(item)) {
          player.sendMessage("Can't set the ControllerBlock type to " + item);
          return;
        }

        if (((conBlock.protectedLevel == 1) || (conBlock.protectedLevel == 2)) && (!this.parent.isUnprotectedMaterial(item)))
        {
          player.sendMessage("The Material is protected, can't use with (semi-)unprotected ControllerBlocks.");
          return;
        }
        conBlock.setType(item);
      }

      if ((item != Material.AIR) && (item != conBlock.getType())) {
        player.sendMessage("This ControllerBlock needs to be edited with " + conBlock.getType());
        return;
      }

      this.parent.map.put(player, conBlock);
      conBlock.editBlock(true);
      player.sendMessage("You're now editing this block with " + conBlock.getType() + " " + Util.formatBlockCount(conBlock));
    }
  }

  public void onBlockPlace(BlockPlaceEvent e)
  {
    if ((e.isCancelled()) || (!e.canBuild())) return;
    Player player = e.getPlayer();
    CBlock conBlock = (CBlock)this.parent.map.get(player);
    if (conBlock == null) return;

    if ((this.parent.getConfigu().getInt(Config.Option.MaxBlocksPerController).intValue() != 0) && 
      (conBlock.numBlocks() >= this.parent.getConfigu().getInt(Config.Option.MaxBlocksPerController).intValue()) && 
      (!this.parent.getPerm().isAdminPlayer(player)))
    {
      player.sendMessage("Controller block is full " + Util.formatBlockCount(conBlock));
      return;
    }

    if ((this.parent.getConfigu().getInt(Config.Option.MaxDistanceFromController).intValue() != 0) && 
      (conBlock.getType().equals(e.getBlock().getType())) && 
      (!this.parent.getPerm().isAdminPlayer(player)) && 
      (Util.getDistanceBetweenLocations(conBlock.getLoc(), e.getBlock().getLocation()) > this.parent.getConfigu().getInt(Config.Option.MaxDistanceFromController).intValue()))
    {
      player.sendMessage("This block is too far away from the controller block to be controlled");
      return;
    }

    if (conBlock.addBlock(e.getBlock()))
    {
      player.sendMessage("Added block to controller " + Util.formatBlockCount(conBlock));
    }
  }

  public void onBlockRedstoneChange(BlockRedstoneEvent e) {
    CBlock conBlock = null;
    if (this.parent.getConfigu().getBool(Config.Option.QuickRedstoneCheck)) {
      conBlock = this.parent.getCBlock(e.getBlock().getRelative(BlockFace.DOWN).getLocation());
    }
    if (conBlock == null) return;

    BlockState s = e.getBlock().getState();
    if (s.getType().equals(Material.REDSTONE_WIRE)) {
      MaterialData m = s.getData();
      m.setData((byte)e.getNewCurrent());
      s.setData(m);
    }
    conBlock.doRedstoneCheck(s);
  }

  public void onBlockPhysics(BlockPhysicsEvent e) {
    if (e.isCancelled()) return;
    CBlock conBlock = this.parent.getControllerBlockFor(null, e.getBlock().getLocation(), null, Boolean.valueOf(true));
    if (conBlock == null)
    {
      return;
    }
    if (conBlock.isBeingEdited())
    {
      if (!this.parent.blockPhysicsEditCheck) return;

      if ((e.getBlock().getType().equals(Material.FENCE)) || (e.getBlock().getType().equals(Material.THIN_GLASS))) return;

      Player player = getPlayerEditing(conBlock);

      if (!Util.typeEquals(conBlock.getType(), e.getChangedType()))
      {
        this.parent.log.debug("Block at " + Util.formatLocation(e.getBlock().getLocation()) + " was changed to " + e.getChangedType() + " but is supposed to be " + conBlock.getType() + ", dupe!");
        conBlock.delBlock(e.getBlock());
        player.sendMessage("Removing block due to changed type while editing " + Util.formatBlockCount(conBlock));
      }
    } else {
      BlockProtectMode protect = (BlockProtectMode)this.parent.getConfigu().getOpt(Config.Option.BlockPhysicsProtectMode);
      if (protect.equals(BlockProtectMode.protect))
        e.setCancelled(true);
      else if (protect.equals(BlockProtectMode.remove))
        conBlock.delBlock(e.getBlock());
    }
  }

  public void onBlockFromTo(BlockFromToEvent e)
  {
    if (e.isCancelled()) return;
    CBlock conBlock = this.parent.getControllerBlockFor(null, e.getToBlock().getLocation(), null, Boolean.valueOf(true));
    if (conBlock == null) return;
    if (conBlock.isBeingEdited())
    {
      if (!this.parent.blockPhysicsEditCheck) return;
      Player player = getPlayerEditing(conBlock);
      this.parent.log.debug("Block at " + Util.formatLocation(e.getToBlock().getLocation()) + " was drowned while editing and removed from a controller");
      conBlock.delBlock(e.getToBlock());
      player.sendMessage("Removing block due to change while editing " + Util.formatBlockCount(conBlock));
    } else {
      BlockProtectMode protect = (BlockProtectMode)this.parent.getConfigu().getOpt(Config.Option.BlockFlowProtectMode);
      if (protect.equals(BlockProtectMode.protect))
        e.setCancelled(true);
      else if (protect.equals(BlockProtectMode.remove))
        conBlock.delBlock(e.getBlock());
    }
  }

  public void onBlockPistonExtend(BlockPistonExtendEvent event)
  {
    if (event.isCancelled())
    {
      return;
    }

    if (((Boolean)this.parent.getConfigu().getOpt(Config.Option.PistonProtection)).booleanValue())
    {
      Block b = event.getBlock();
      CBlock conBlock = this.parent.getCBlock(b.getLocation());
      if (conBlock != null)
      {
        event.setCancelled(true);
        return;
      }
      List pblocks = event.getBlocks();
      for (int i = 0; i < pblocks.size(); i++)
      {
        Block block = (Block)pblocks.get(i);
        if ((!this.parent.isControlledBlock(block.getLocation(), block.getType())) || (this.parent.isUnprotectedMaterial(block.getType())))
          continue;
        event.setCancelled(true);
      }
    }
  }

  public void onBlockPistonRetract(BlockPistonRetractEvent event)
  {
    if (event.isCancelled())
    {
      return;
    }

    if (((Boolean)this.parent.getConfigu().getOpt(Config.Option.PistonProtection)).booleanValue())
    {
      Block b = event.getBlock();
      CBlock conBlock = this.parent.getCBlock(b.getLocation());
      if (conBlock != null)
      {
        event.setCancelled(true);
        return;
      }
      if (event.isSticky())
      {
        Block block = b.getWorld().getBlockAt(event.getRetractLocation()).getRelative(event.getDirection());
        if ((this.parent.isControlledBlock(block.getLocation(), block.getType())) && (!this.parent.isUnprotectedMaterial(block.getType())))
        {
          event.setCancelled(true);
        }
      }
    }
  }

  public void run() {
    if (!this.parent.getConfigu().getBool(Config.Option.DisableEditDupeProtection)) {
      for (Map.Entry e : this.parent.map.entrySet()) {
        Iterator i = ((CBlock)e.getValue()).getBlocks();
        while (i.hasNext()) {
          Block b = Util.getBlockAtLocation(((BlockDesc)i.next()).blockLoc);
          if (!Util.typeEquals(b.getType(), ((CBlock)e.getValue()).getType())) {
            this.parent.log.debug("Block at " + Util.formatLocation(b.getLocation()) + " was " + b.getType() + " but expected " + ((CBlock)e.getValue()).getType() + ", dupe!");
            i.remove();
            ((Player)e.getKey()).sendMessage("Removing block due to changed while editing " + Util.formatBlockCount((CBlock)e.getValue()));
            return;
          }
        }
      }
    }
    for (Map.Entry e : this.parent.map.entrySet()) {
      Iterator i = ((CBlock)e.getValue()).getBlocks();
      while (i.hasNext()) {
        BlockDesc d = (BlockDesc)i.next();
        Block b = Util.getBlockAtLocation(d.blockLoc);
        d.blockData = b.getState().getData().getData();
      }
    }
  }
}