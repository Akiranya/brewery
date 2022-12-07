package com.dre.brewery.api.events;

import com.dre.brewery.BCauldron;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.LegacyUtil;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Player adding an ingredient to a cauldron.
 * <p>
 * Always one item added at a time.
 * <p>
 * If needed, use the cauldrons add method to manually add more items.
 */
public class IngredientAddEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Block block;
	private final BCauldron cauldron;
	private ItemStack ingredient;
	private RecipeItem rItem;
	private boolean cancelled;
	private boolean takeItem = true;

	public IngredientAddEvent(Player who, Block block, BCauldron bCauldron, ItemStack ingredient, RecipeItem rItem) {
		super(who);
		this.block = block;
		cauldron = bCauldron;
		this.rItem = rItem;
		this.ingredient = ingredient;
	}

	// Required by Bukkit
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Block getBlock() {
		return block;
	}

	public BCauldron getCauldron() {
		return cauldron;
	}

	/**
	 * The Recipe item that matches the ingredient.
	 *
	 * <p>This might not be the only recipe item that will match the
	 * ingredient.
	 *
	 * <p>Will be recalculated if {@link #ingredient} is changed
	 * with the {@link #setIngredient(org.bukkit.inventory.ItemStack)} method.
	 */
	public RecipeItem getRecipeItem() {
		return rItem;
	}

	/**
	 * Get the item currently being added to the cauldron by the player.
	 *
	 * <p>Can be changed directly (mutable) or with {@link
	 * #setIngredient(org.bukkit.inventory.ItemStack)} method.
	 *
	 * <p>The amount is ignored and always one added.
	 *
	 * @return The item being added
	 */
	public ItemStack getIngredient() {
		return ingredient;
	}

	/**
	 * Set the ingredient added to the cauldron to something else.
	 *
	 * <p>Will always be accepted, even when not in a recipe or the cooked
	 * list.
	 *
	 * <p>The amount is ignored and always one added.
	 *
	 * <p>This also recalculates the recipeItem {@link #rItem}!
	 *
	 * @param ingredient The item to add instead
	 */
	public void setIngredient(ItemStack ingredient) {
		this.ingredient = ingredient;
		// The Ingredient has been changed. Recalculate RecipeItem!
		rItem = RecipeItem.getMatchingRecipeItem(ingredient, true);
	}

	/**
	 * If the amount of the item in the players hand should be decreased.
	 *
	 * <p>(Default: true)
	 */
	public boolean willTakeItem() {
		return takeItem;
	}

	/**
	 * Set if the amount of the item in the players hand should be decreased.
	 *
	 * @param takeItem if the item amount in the hand should be decreased
	 */
	public void setTakeItem(boolean takeItem) {
		this.takeItem = takeItem;
	}

	/**
	 * Get the BlockData of the Cauldron.
	 *
	 * <p>May be null if the Cauldron does not exist anymore.
	 *
	 * @return The {@link org.bukkit.block.data.BlockData} of the cauldron
	 */
	@Nullable
	public Levelled getCauldronData() {
		BlockData data = block.getBlockData();
		if (data instanceof Levelled) {
			return (Levelled) data;
		}
		return null;
	}

	/**
	 * Get the water fill level of the Cauldron.
	 *
	 * <p>0 = empty, 1 = something in, 2 = full.
	 *
	 * <p>Can use {@code BCauldron.EMPTY}, {@code BCauldron.SOME}, {@code
	 * BCauldron.FULL}.
	 *
	 * @return The fill level as a byte 0-2
	 */
	public byte getFillLevel() {
		return LegacyUtil.getFillLevel(block);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If the event is cancelled, no item will be added or taken from the
	 * player.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
