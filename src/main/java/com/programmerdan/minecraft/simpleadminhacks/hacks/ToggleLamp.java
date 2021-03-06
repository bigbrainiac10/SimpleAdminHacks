package com.programmerdan.minecraft.simpleadminhacks.hacks;

import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.SimpleHack;
import com.programmerdan.minecraft.simpleadminhacks.configs.ToggleLampConfig;

import net.minecraft.server.v1_12_R1.World;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.ReinforcementManager;
import vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement;
import vg.civcraft.mc.citadel.reinforcement.Reinforcement;

public class ToggleLamp extends SimpleHack<ToggleLampConfig> implements Listener {

	public static final String NAME = "ToggleLamp";
	
	private static final String META_COOLDOWN = "ToggleLamp_NextToggle";
	private static final String META_TOGGLED = "ToggleLamp_Toggled";
	
	private ReinforcementManager rm;
	
	public ToggleLamp(SimpleAdminHacks plugin, ToggleLampConfig config) {
		super(plugin, config);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onInteract(PlayerInteractEvent event) {
		if(!config.isEnabled()) {
			return;
		}
		
		Block clickedBlock = event.getClickedBlock();
		
		if(clickedBlock == null) {
			return;
		}
		
		
		// if it wasn't a right click
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		
		if(event.getItem() == null) {
			return;
		}
		
		// if it wasn't a stick in their main hand
		if(event.getHand() != EquipmentSlot.HAND || event.getItem().getType() != Material.STICK) {
			return;
		}
		
		Player eventPlayer = event.getPlayer();
		Material clickedBlockMat = clickedBlock.getType();

		if(!(clickedBlockMat == Material.REDSTONE_LAMP_ON || clickedBlockMat == Material.REDSTONE_LAMP_OFF)) {
			return;
		}
		
		boolean toggled = clickedBlock.hasMetadata(META_TOGGLED) ? clickedBlock.getMetadata(META_TOGGLED).get(0).asBoolean() : false;
		
		if(!toggled && clickedBlockMat == Material.REDSTONE_LAMP_ON) {
			return;
		}
		
		if(clickedBlock.hasMetadata(META_COOLDOWN)){
			MetadataValue val = clickedBlock.getMetadata(META_COOLDOWN).get(0);
			if(((long)val.value()) > System.currentTimeMillis()) {
				return;
			}
		}
		
		if(rm != null) {
			if(rm.isReinforced(clickedBlock)) {
				Reinforcement rein = rm.getReinforcement(clickedBlock);
				
				if(rein instanceof PlayerReinforcement) {
					PlayerReinforcement pr = (PlayerReinforcement)rein;
					if(!pr.getGroup().isMember(eventPlayer.getUniqueId())) {
						return;
					}
				}
			}
		}
		
		try {
			switchLamp(clickedBlock, !clickedBlockMat.equals(Material.REDSTONE_LAMP_ON));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			plugin().log(Level.SEVERE, "Error when toggling lamp: ", e);
		}
		
		eventPlayer.getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5F, 1.0F);
		
	}
	
	// block update
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		Block eventBlock = event.getBlock();
		Material eventMat = eventBlock.getType();
		
		if(eventMat != Material.REDSTONE_LAMP_ON && eventMat != Material.REDSTONE_LAMP_OFF) {
			return;
		}
		
		boolean toggled = eventBlock.hasMetadata(META_TOGGLED) ? eventBlock.getMetadata(META_TOGGLED).get(0).asBoolean() : false;
		
		if(toggled) {
			event.setCancelled(true);
		}
	}

	@Override
	public void registerListeners() {
		if(config.isEnabled()) {
			plugin().log("Registering ToggleLamp listeners");
			plugin().registerListener(this);
		}
	}

	@Override
	public void registerCommands() {
	}

	@Override
	public void dataBootstrap() {
		rm = plugin().serverHasPlugin("Citadel") ? Citadel.getReinforcementManager() : null;
	}

	@Override
	public void unregisterListeners() {
	}

	@Override
	public void unregisterCommands() {
	}

	@Override
	public void dataCleanup() {
		rm = null;
	}

	@Override
	public String status() {
		return config.isEnabled() ? "ToggleLamp enabled." : "ToggleLamp disabled.";
	}
	
	public static ToggleLampConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
		return new ToggleLampConfig(plugin, config);
	}
	
	private void switchLamp(Block block, boolean lighting) throws NoSuchFieldException, IllegalAccessException
	{
		World world = ((CraftWorld)block.getWorld()).getHandle();
		
		if (lighting)
		{
			setWorldStatic(world, true);
			block.setType(Material.REDSTONE_LAMP_ON);
			setWorldStatic(world, false);
			block.setMetadata(META_TOGGLED, new FixedMetadataValue(plugin(), true));
		}
		else
		{
			block.setType(Material.REDSTONE_LAMP_OFF);
			block.setMetadata(META_TOGGLED, new FixedMetadataValue(plugin(), false)); // need to add stuff to counteract redstone
		}
		block.setMetadata(META_COOLDOWN, new FixedMetadataValue(plugin(), System.currentTimeMillis()+config.getCooldownTime()));
	}

	private void setWorldStatic(World world, boolean isStatic) throws NoSuchFieldException, IllegalAccessException  {
		java.lang.reflect.Field field = World.class.getDeclaredField("isClientSide");
		
		field.setAccessible(true);
		field.set(world, isStatic);
	}

}
