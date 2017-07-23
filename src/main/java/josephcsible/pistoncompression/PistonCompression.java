/*
PistonCompression Minecraft Mod
Copyright (C) 2017 Joseph C. Sible

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package josephcsible.pistoncompression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.state.BlockPistonStructureHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.InvalidBlockStateException;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = PistonCompression.MODID, version = PistonCompression.VERSION, acceptedMinecraftVersions = "[1.11,)", guiFactory = "josephcsible.pistoncompression.PistonCompressionGuiFactory")
public class PistonCompression
{
	// XXX duplication with mcmod.info and build.gradle
	public static final String MODID = "pistoncompression";
	public static final String VERSION = "1.1.0";

	protected static final String[] DEFAULT_REPLACEMENTS = {
		"minecraft:ice * minecraft:packed_ice",
		"minecraft:coal_block * minecraft:diamond",
	};

	public static class Replacement {
		public final Predicate<IBlockState> predicate;
		public final IBlockState newState;
		public final ItemStack stack;

		public Replacement(Predicate<IBlockState> predicate, IBlockState newState) {
			this.predicate = predicate;
			this.newState = newState;
			this.stack = null;
		}

		public Replacement(Predicate<IBlockState> predicate, ItemStack stack) {
			this.predicate = predicate;
			this.newState = Blocks.AIR.getDefaultState();
			this.stack = stack;
		}
	}

	public static Configuration config;
	public static Map<Block, List<Replacement>> replacements;
	public static Logger log;

	@EventHandler
	public static void preInit(FMLPreInitializationEvent event) {
		log = event.getModLog();
		config = new Configuration(event.getSuggestedConfigurationFile());
		syncConfig();
	}

	@EventHandler
	public static void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(PistonCompression.class);
	}

	@SubscribeEvent
	public static void onConfigChanged(OnConfigChangedEvent eventArgs) {
		if (eventArgs.getModID().equals(MODID))
			syncConfig();
	}

	protected static void syncConfig() {
		replacements = new HashMap<Block, List<Replacement>>();
		config.setCategoryComment(Configuration.CATEGORY_GENERAL, "Blocks, predicates, items, data values, and states are specified exactly as they are with the /testforblock, /setblock, and /give commands.\nIf multiple replacements with different predicates are specified for the same block, the earliest matching one wins.");
		// Not using getStringList to avoid overly-long list of default values in the comment
		// XXX it still appears in the GUI; that needs to be fixed too
		Property prop = config.get(Configuration.CATEGORY_GENERAL, "replacements", DEFAULT_REPLACEMENTS);
		prop.setLanguageKey("replacements");
		prop.setComment("A list of entries in the following format: <block> <dataValue|-1|state|*> (<block> [dataValue|state]|<item> [data] [quantity])");
		for(String str : prop.getStringList()) {
			String[] pieces = str.split(" ");
			if(pieces.length < 3 || pieces.length > 5) {
				log.warn("Ignoring replacement with {} terms (expected 3 to 5): {}", pieces.length, str);
				continue;
			}

			ResourceLocation rl = new ResourceLocation(pieces[0]);
			if (!Block.REGISTRY.containsKey(rl)) {
				log.warn("Ignoring replacement with unknown old block: {}", str);
				continue;
			}
			Block oldBlock = Block.REGISTRY.getObject(rl);

			Predicate<IBlockState> predicate;
			try {
				predicate = CommandBase.convertArgToBlockStatePredicate(oldBlock, pieces[1]);
			} catch (InvalidBlockStateException e) {
				log.warn("Ignoring replacement with invalid predicate: {}", str);
				continue;
			}

			rl = new ResourceLocation(pieces[2]);
			if (pieces.length != 5 && Block.REGISTRY.containsKey(rl)) {
				Block newBlock = Block.REGISTRY.getObject(rl);
				IBlockState newState;
				if(pieces.length == 4) {
					try {
						newState = CommandBase.convertArgToBlockState(newBlock, pieces[3]);
					} catch (NumberInvalidException e) {
						log.warn("Ignoring replacement with out-of-range new block data value: {}", str);
						continue;
					} catch (InvalidBlockStateException e) {
						log.warn("Ignoring replacement with invalid new block state: {}", str);
						continue;
					}
				} else {
					newState = newBlock.getDefaultState();
				}

				if(!replacements.containsKey(oldBlock)) {
					replacements.put(oldBlock, new ArrayList<Replacement>());
				}
				replacements.get(oldBlock).add(new Replacement(predicate, newState));
			} else if(Item.REGISTRY.containsKey(rl)) {
				int meta, quantity;
				if(pieces.length >= 4) {
					if(pieces.length == 5) {
						try {
							quantity = Integer.parseInt(pieces[4]);
						} catch (NumberFormatException e) {
							log.warn("Ignoring replacement with invalid item quantity: {}", str);
							continue;
						}
					} else {
						quantity = 1;
					}

					try {
						meta = Integer.parseInt(pieces[3]);
					} catch (NumberFormatException e) {
						log.warn("Ignoring replacement with invalid item data: {}", str);
						continue;
					}
				} else {
					meta = 0;
					quantity = 1;
				}

				if(!replacements.containsKey(oldBlock)) {
					replacements.put(oldBlock, new ArrayList<Replacement>());
				}
				replacements.get(oldBlock).add(new Replacement(predicate, new ItemStack(Item.REGISTRY.getObject(rl), quantity, meta)));
			} else {
				log.warn("Ignoring replacement with unknown new block or item: {}", str);
			}
		}
		if (config.hasChanged())
			config.save();
	}

	public static void checkForCompression(World world, BlockPos startPos) {
		// Find the center of the piston compressor, if there is one
		IBlockState state = world.getBlockState(startPos);
		if(!(state.getBlock() instanceof BlockPistonBase)) {
			return;
		}
		startPos = startPos.offset(state.getValue(BlockPistonBase.FACING), 2);

		// Check to see if the piston compressor exists and is set up properly
		// Also, keep track of what's in the compressor for later
		List<IBlockState> states = new ArrayList<IBlockState>();
		state = world.getBlockState(startPos);
		Block block = state.getBlock();
		if(!replacements.containsKey(block)) {
			// We're not configured to compress this kind of block
			return;
		}
		states.add(state);
		for(EnumFacing facing : EnumFacing.values()) {
			// the thing being compressed
			BlockPos pos = startPos.offset(facing);
			state = world.getBlockState(pos);
			if(state.getBlock() != block) {
				// Can only compress if all blocks being compressed are the same
				return;
			}
			states.add(state);

			// the piston
			pos = pos.offset(facing);
			state = world.getBlockState(pos);
			if(!(state.getBlock() instanceof BlockPistonBase) ||
					state.getValue(BlockPistonBase.EXTENDED) ||
					state.getValue(BlockPistonBase.FACING) != facing.getOpposite() ||
					!((BlockPistonBase) state.getBlock()).shouldBeExtended(world, pos, facing.getOpposite()) ||
					(new BlockPistonStructureHelper(world, pos, facing, true)).canMove()) {
				// A piston compressor is only valid if it has 6 pistons pointing at its center,
				// all of which are trying but unable to extend
				return;
			}
		}

		// Check to see if what's in the compressor is compressible, and if so, compress it
		outerLoop:
		for(Replacement replacement : replacements.get(block)) {
			for(IBlockState stateToCompress : states) {
				if(!replacement.predicate.apply(stateToCompress)) {
					continue outerLoop;
				}
			}
			// If we get here, then we're good to compress with the current replacement
			world.setBlockState(startPos, replacement.newState);
			for(EnumFacing facing : EnumFacing.values()) {
				world.setBlockState(startPos.offset(facing), Blocks.AIR.getDefaultState());
			}
			if(replacement.stack != null && !replacement.stack.isEmpty() && !world.isRemote && !world.restoringBlockSnapshots) {
				// not using Block.spawnAsEntity because we want this to drop even with doTileDrops false
				EntityItem entityitem = new EntityItem(world,
						startPos.getX() + (world.rand.nextFloat() * 0.5D) + 0.25D,
						startPos.getY() + (world.rand.nextFloat() * 0.5D) + 0.25D,
						startPos.getZ() + (world.rand.nextFloat() * 0.5D) + 0.25D,
						replacement.stack);
				entityitem.setDefaultPickupDelay();
				world.spawnEntity(entityitem);
			}
			break;
		}
	}

	public static void checkNeighborsForCompression(BlockEvent event) {
		World world = event.getWorld();
		BlockPos pos = event.getPos();
		checkForCompression(world, pos);
		for(EnumFacing facing : EnumFacing.values()) {
			checkForCompression(world, pos.offset(facing));
		}
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.PlaceEvent event) {
		checkNeighborsForCompression(event);
	}

	@SubscribeEvent
	public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
		checkNeighborsForCompression(event);
	}
}
