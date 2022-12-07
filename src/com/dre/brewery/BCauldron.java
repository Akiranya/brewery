package com.dre.brewery;

import com.dre.brewery.api.events.IngredientAddEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.LegacyUtil;
import com.dre.brewery.utility.TownyUtil;
import com.dre.brewery.utility.Tuple;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BCauldron {
	public static final byte EMPTY = 0, SOME = 1, FULL = 2;
	public static final int PARTICLE_PAUSE = 15;
	public static final Random particleRandom = new Random();
	public static final Map<Block, BCauldron> bCauldrons = new HashMap<>(); // All active cauldrons. Mapped to their block for fast retrieve
	private static final Set<UUID> plInteracted = new HashSet<>(); // Interact Event helper

	private final Block block;
	private final Location particleLocation;
	private BIngredients ingredients = new BIngredients();
	private int state = 0;
	private boolean changed = false; // Not really needed anymore
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private Optional<BCauldronRecipe> particleRecipe; // null if we haven't checked, empty if there is none
	private Color particleColor;

	public BCauldron(Block block) {
		this.block = block;
		particleLocation = block.getLocation().add(0.5, 0.9, 0.5);
	}

	// loading from file
	public BCauldron(Block block, BIngredients ingredients, int state) {
		this.block = block;
		this.state = state;
		this.ingredients = ingredients;
		particleLocation = block.getLocation().add(0.5, 0.9, 0.5);
	}

	// get cauldron by Block
	@Nullable
	public static BCauldron get(Block block) {
		return bCauldrons.get(block);
	}

	/**
	 * Get cauldron from block and add given ingredient.
	 * <p>
	 * Calls the IngredientAddEvent and may be cancelled or changed.
	 *
	 * @param block      the cauldron block
	 * @param ingredient the ingredient to be added to the cauldron
	 * @param player     the player who adds the ingredient
	 * @return true if the item should be consumed (ie, taken away)
	 */
	public static boolean ingredientAdd(Block block, ItemStack ingredient, Player player) {
		// if not empty
		if (LegacyUtil.getFillLevel(block) != EMPTY) {

			// Towny integration starts
			if (BConfig.useTowny && !TownyUtil.canSwitch(block.getLocation(), player, true)) {
				P.p.msg(player, P.p.languageReader.get("Towny_BrewingInForeignTown"));
				return false;
			}
			// Towny integration ends

			if (!BCauldronRecipe.acceptedMaterials.contains(ingredient.getType()) && !ingredient.hasItemMeta()) {
				// Extremely fast way to check for most items
				return false;
			}
			// If the Item is on the list, or customized, we have to do more checks
			RecipeItem rItem = RecipeItem.getMatchingRecipeItem(ingredient, false);
			if (rItem == null) {
				return false;
			}

			BCauldron bCauldron = get(block);
			if (bCauldron == null) {
				bCauldron = new BCauldron(block);
				BCauldron.bCauldrons.put(block, bCauldron);
			}

			IngredientAddEvent event = new IngredientAddEvent(player, block, bCauldron, ingredient.clone(), rItem);
			P.p.getServer().getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				bCauldron.add(event.getIngredient(), event.getRecipeItem());
				return event.willTakeItem();
			} else {
				return false;
			}
		}
		return false;
	}

	// prints the current cooking time to the player
	public static void printTime(Player player, Block block) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			P.p.msg(player, P.p.languageReader.get("Error_NoPermissions"));
			return;
		}
		BCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (bcauldron.state >= 1) {
				P.p.msg(player, P.p.languageReader.get("Player_CauldronInfo1", "" + bcauldron.state));
			} else {
				P.p.msg(player, P.p.languageReader.get("Player_CauldronInfo2"));
			}
		}
	}

	public static void processCookEffects() {
		if (!BConfig.enableCauldronParticles) return;
		if (bCauldrons.isEmpty()) {
			return;
		}
		final float chance = 1f / PARTICLE_PAUSE;

		for (BCauldron cauldron : bCauldrons.values()) {
			if (particleRandom.nextFloat() < chance) {
				cauldron.cookEffect();
			}
		}
	}

	public static void clickCauldron(PlayerInteractEvent event) {
		Material materialInHand = event.getMaterial();
		ItemStack item = event.getItem();
		Player player = event.getPlayer();
		Block clickedBlock = event.getClickedBlock();
		assert clickedBlock != null;

		if (materialInHand == Material.AIR || materialInHand == Material.BUCKET) {
			return;
		}

		// Towny integration starts
		if (BConfig.useTowny && !TownyUtil.canSwitch(clickedBlock.getLocation(), player, true)) {
			P.p.msg(player, P.p.languageReader.get("Towny_TakeBrewFromForeignTown"));
			return;
		}
		// Towny integration ends

		if (materialInHand == LegacyUtil.CLOCK) {
			printTime(player, clickedBlock);
			return;

		} else if (materialInHand == Material.GLASS_BOTTLE) {
			// fill a glass bottle with potion

			assert item != null;
			if (player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
				BCauldron bCauldron = get(clickedBlock);
				if (bCauldron != null) {
					if (bCauldron.fill(player, clickedBlock)) {
						event.setCancelled(true);
						if (player.hasPermission("brewery.cauldron.fill")) {
							if (item.getAmount() > 1) {
								item.setAmount(item.getAmount() - 1);
							} else {
								BUtil.setItemInHand(event, Material.AIR, false);
							}
						}
					}
				}
			} else {
				event.setCancelled(true);
			}
			return;

		} else if (materialInHand == Material.WATER_BUCKET) {
			// Ignore Water Buckets

			if (!P.use1_9) {
				// reset < 1.9 cauldron when refilling to prevent unlimited source of potions
				// We catch >=1.9 cases in the Cauldron Listener
				if (LegacyUtil.getFillLevel(clickedBlock) == 1) {
					// will only remove when existing
					BCauldron.remove(clickedBlock);
				}
			}
			return;
		}

		// Check if fire alive below cauldron when adding ingredients
		Block down = clickedBlock.getRelative(BlockFace.DOWN);
		if (LegacyUtil.isCauldronHeatsource(down)) {

			event.setCancelled(true);
			boolean handSwap = false;

			// Interact event is called twice!!!?? in 1.9, once for each hand.
			// Certain Items in Hand cause one of them to be cancelled or not called at all sometimes.
			// We mark if a player had the event for the main hand
			// If not, we handle the main hand in the event for the off hand
			if (P.use1_9) {
				if (event.getHand() == EquipmentSlot.HAND) {
					final UUID id = player.getUniqueId();
					plInteracted.add(id);
					P.p.getServer().getScheduler().runTask(P.p, () -> plInteracted.remove(id));
				} else if (event.getHand() == EquipmentSlot.OFF_HAND) {
					if (!plInteracted.remove(player.getUniqueId())) {
						item = player.getInventory().getItemInMainHand();
						if (item.getType() != Material.AIR) {
							handSwap = true;
						} else {
							item = BConfig.useOffhandForCauldron ? event.getItem() : null;
						}
					}
				}
			}
			if (item == null) return;

			if (!player.hasPermission("brewery.cauldron.insert")) {
				P.p.msg(player, P.p.languageReader.get("Perms_NoCauldronInsert"));
				return;
			}
			if (ingredientAdd(clickedBlock, item, player)) {
				boolean isBucket = item.getType().name().endsWith("_BUCKET");
				boolean isBottle = LegacyUtil.isBottle(item.getType());
				if (item.getAmount() > 1) {
					item.setAmount(item.getAmount() - 1);

					if (isBucket) {
						giveItem(player, new ItemStack(Material.BUCKET));
					} else if (isBottle) {
						giveItem(player, new ItemStack(Material.GLASS_BOTTLE));
					}
				} else {
					if (isBucket) {
						BUtil.setItemInHand(event, Material.BUCKET, handSwap);
					} else if (isBottle) {
						BUtil.setItemInHand(event, Material.GLASS_BOTTLE, handSwap);
					} else {
						BUtil.setItemInHand(event, Material.AIR, handSwap);
					}
				}
			}
		}
	}

	/**
	 * Recalculate the Cauldron Particle Recipe
	 */
	public static void reload() {
		for (BCauldron cauldron : bCauldrons.values()) {
			//noinspection OptionalAssignedToNull
			cauldron.particleRecipe = null;
			cauldron.particleColor = null;
			if (BConfig.enableCauldronParticles) {
				if (BUtil.isChunkLoaded(cauldron.block) && LegacyUtil.isCauldronHeatsource(cauldron.block.getRelative(BlockFace.DOWN))) {
					cauldron.getParticleColor();
				}
			}
		}
	}

	/**
	 * Reset to normal cauldron.
	 */
	public static boolean remove(Block block) {
		return bCauldrons.remove(block) != null;
	}

	/**
	 * Are any Cauldrons in that World.
	 */
	public static boolean hasDataInWorld(World world) {
		return bCauldrons.keySet().stream().anyMatch(block -> block.getWorld().equals(world));
	}

	// unloads cauldrons that are in an unloading world
	// as they were written to file just before, this is safe to do
	public static void onUnload(World world) {
		bCauldrons.keySet().removeIf(block -> block.getWorld().equals(world));
	}

	/**
	 * Unload all Cauldrons that are in an unloaded World.
	 */
	public static void unloadWorlds() {
		List<World> worlds = P.p.getServer().getWorlds();
		bCauldrons.keySet().removeIf(block -> !worlds.contains(block.getWorld()));
	}

	public static void save(ConfigurationSection config, ConfigurationSection oldData) {
		BUtil.createWorldSections(config);

		if (!bCauldrons.isEmpty()) {
			int id = 0;
			for (BCauldron cauldron : bCauldrons.values()) {
				String worldName = cauldron.block.getWorld().getName();
				String prefix;

				if (worldName.startsWith("DXL_")) {
					prefix = BUtil.getDxlName(worldName) + "." + id;
				} else {
					prefix = cauldron.block.getWorld().getUID() + "." + id;
				}

				config.set(prefix + ".block", cauldron.block.getX() + "/" + cauldron.block.getY() + "/" + cauldron.block.getZ());
				if (cauldron.state != 0) {
					config.set(prefix + ".state", cauldron.state);
				}
				config.set(prefix + ".ingredients", cauldron.ingredients.serializeIngredients());
				id++;
			}
		}
		// copy cauldrons that are not loaded
		if (oldData != null) {
			for (String uuid : oldData.getKeys(false)) {
				if (!config.contains(uuid)) {
					config.set(uuid, oldData.get(uuid));
				}
			}
		}
	}

	// Bukkit bug:
	// not updating the inventory while executing event,
	// have to schedule the give
	public static void giveItem(final Player player, final ItemStack item) {
		P.p.getServer().getScheduler().runTaskLater(P.p, () -> player.getInventory().addItem(item), 1L);
	}

	/**
	 * Updates this Cauldron, increasing the cook time and checking for HeatSource.
	 *
	 * @return false if Cauldron needs to be removed
	 */
	public boolean onUpdate() {
		// add a minute to cooking time
		if (!BUtil.isChunkLoaded(block)) {
			increaseState();
		} else {
			if (!LegacyUtil.isWaterCauldron(block.getType())) {
				// Catch any WorldEdit etc. removal
				return false;
			}
			// Check if fire still alive
			if (LegacyUtil.isCauldronHeatsource(block.getRelative(BlockFace.DOWN))) {
				increaseState();
			}
		}
		return true;
	}

	/**
	 * Will add a minute to the cooking time
	 */
	public void increaseState() {
		state++;
		if (changed) {
			ingredients = ingredients.copy();
			changed = false;
		}
		particleColor = null;
	}

	// add an ingredient to the cauldron
	public void add(ItemStack ingredient, RecipeItem rItem) {
		if (ingredient == null || ingredient.getType() == Material.AIR) return;
		if (changed) {
			ingredients = ingredients.copy();
			changed = false;
		}

		//noinspection OptionalAssignedToNull
		particleRecipe = null;
		particleColor = null;
		ingredients.add(ingredient, rItem);
		block.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
		if (state > 0) {
			state--;
		}
		if (BConfig.enableCauldronParticles && !BConfig.minimalParticles) {
			// Few little sparks and lots of water splashes. Offset by 0.2 in x and z
			block.getWorld().spawnParticle(Particle.SPELL_INSTANT, particleLocation, 2, 0.2, 0, 0.2);
			block.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLocation, 10, 0.2, 0, 0.2);
		}
	}

	/**
	 * Get the Block that this BCauldron represents.
	 */
	public Block getBlock() {
		return block;
	}

	/**
	 * Get the State (Time in Minutes) that this Cauldron currently has.
	 */
	public int getState() {
		return state;
	}

	// fills players bottle with cooked brew
	public boolean fill(Player player, Block block) {
		if (!player.hasPermission("brewery.cauldron.fill")) {
			P.p.msg(player, P.p.languageReader.get("Perms_NoCauldronFill"));
			return true;
		}
		ItemStack potion = ingredients.cook(state);
		if (potion == null) return false;

		if (P.use1_13) {
			BlockData data = block.getBlockData();
			if (!(data instanceof Levelled)) {
				bCauldrons.remove(block);
				return false;
			}
			Levelled cauldron = ((Levelled) data);
			if (cauldron.getLevel() <= 0) {
				bCauldrons.remove(block);
				return false;
			}

			// If the Water_Cauldron type exists and the cauldron is on last level
			if (LegacyUtil.WATER_CAULDRON != null && cauldron.getLevel() == 1) {
				// Empty Cauldron
				block.setType(Material.CAULDRON);
				bCauldrons.remove(block);
			} else {
				cauldron.setLevel(cauldron.getLevel() - 1);

				// Update the new Level to the Block
				// We have to use the BlockData variable "data" here instead of the cast "cauldron"
				// otherwise < 1.13 crashes on plugin load for not finding the BlockData Class
				block.setBlockData(data);

				if (cauldron.getLevel() <= 0) {
					bCauldrons.remove(block);
				} else {
					changed = true;
				}
			}

		} else {
			@SuppressWarnings("deprecation")
			byte data = block.getData();
			if (data > 3) {
				data = 3;
			} else if (data <= 0) {
				bCauldrons.remove(block);
				return false;
			}
			data -= 1;
			LegacyUtil.setData(block, data);

			if (data == 0) {
				bCauldrons.remove(block);
			} else {
				changed = true;
			}
		}
		if (P.use1_9) {
			block.getWorld().playSound(block.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
		}

		// Bukkit Bug, inventory not updating while in event so this will delay the give
		// but could also just use deprecated updateInventory()
		giveItem(player, potion);
		// player.getInventory().addItem(potion);
		// player.getInventory().updateInventory();

		return true;
	}

	public void cookEffect() {
		if (BUtil.isChunkLoaded(block) && LegacyUtil.isCauldronHeatsource(block.getRelative(BlockFace.DOWN))) {
			Color color = getParticleColor();
			// Colorable spirally spell, 0 count enables color instead of the offset variables
			// Configurable RGB color. The last parameter seems to control the hue and motion, but I couldn't find
			// how exactly in the client code. 1025 seems to be the best for color brightness and upwards motion
			block.getWorld().spawnParticle(Particle.SPELL_MOB, getRandParticleLoc(), 0,
					((double) color.getRed()) / 255.0,
					((double) color.getGreen()) / 255.0,
					((double) color.getBlue()) / 255.0,
					1025.0);

			if (BConfig.minimalParticles) {
				return;
			}

			if (particleRandom.nextFloat() > 0.85) {
				// Dark pixely smoke cloud at 0.4 random in x and z
				// 0 count enables direction, send to y = 1 with speed 0.09
				block.getWorld().spawnParticle(Particle.SMOKE_LARGE, getRandParticleLoc(), 0, 0, 1, 0, 0.09);
			}
			if (particleRandom.nextFloat() > 0.2) {
				// A Water Splash with 0.2 offset in x and z
				block.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLocation, 1, 0.2, 0, 0.2);
			}

			if (P.use1_13 && particleRandom.nextFloat() > 0.4) {
				// Two hovering pixel dust clouds, a bit of offset and with DustOptions to give some color and size
				block.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 2, 0.15, 0.2, 0.15, new Particle.DustOptions(color, 1.5f));
			}
		}
	}

	private Location getRandParticleLoc() {
		return new Location(particleLocation.getWorld(),
				particleLocation.getX() + (particleRandom.nextDouble() * 0.8) - 0.4,
				particleLocation.getY(),
				particleLocation.getZ() + (particleRandom.nextDouble() * 0.8) - 0.4);
	}

	/**
	 * Get or calculate the particle color from the current best Cauldron Recipe.
	 * Also calculates the best Cauldron Recipe if not yet done.
	 *
	 * @return the Particle Color, after potentially calculating it
	 */
	@NotNull
	public Color getParticleColor() {
		if (state < 1) {
			return Color.fromRGB(153, 221, 255); // Bright Blue
		}
		if (particleColor != null) {
			return particleColor;
		}
		//noinspection OptionalAssignedToNull
		if (particleRecipe == null) {
			// Check for Cauldron Recipe
			particleRecipe = Optional.ofNullable(ingredients.getCauldronRecipe());
		}

		List<Tuple<Integer, Color>> colorList = null;
		if (particleRecipe.isPresent()) {
			colorList = particleRecipe.get().getParticleColor();
		}

		if (colorList == null || colorList.isEmpty()) {
			// No color List configured, or no recipe found
			colorList = new ArrayList<>(1);
			colorList.add(new Tuple<>(10, Color.fromRGB(77, 166, 255))); // Dark Aqua kind of Blue
		}
		int index = 0;
		while (index < colorList.size() - 1 && colorList.get(index).a() < state) {
			// Find the first index where the colorList Minute is higher than the state
			index++;
		}

		int minute = colorList.get(index).a();
		if (minute > state) {
			// going towards the minute
			int prevPos;
			Color prevColor;
			if (index > 0) {
				// has previous colours
				prevPos = colorList.get(index - 1).a();
				prevColor = colorList.get(index - 1).b();
			} else {
				prevPos = 0;
				prevColor = Color.fromRGB(153, 221, 255); // Bright Blue
			}

			particleColor = BUtil.weightedMixColor(prevColor, prevPos, state, colorList.get(index).b(), minute);
		} else if (minute == state) {
			// reached the minute
			particleColor = colorList.get(index).b();
		} else {
			// passed the last minute configured
			if (index > 0) {
				// We have more than one color, just use the last one
				particleColor = colorList.get(index).b();
			} else {
				// Only have one color, go towards a Gray
				Color nextColor = Color.fromRGB(138, 153, 168); // Dark Teal, Gray
				int nextPos = (int) (minute * 2.6f);

				if (nextPos <= state) {
					// We are past the next color (Gray) as well, keep using it
					particleColor = nextColor;
				} else {
					particleColor = BUtil.weightedMixColor(colorList.get(index).b(), minute, state, nextColor, nextPos);
				}
			}
		}
		//P.p.log("RGB: " + particleColor.getRed() + "|" + particleColor.getGreen() + "|" + particleColor.getBlue());
		return particleColor;
	}

}
