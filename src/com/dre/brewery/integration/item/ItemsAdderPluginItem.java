package com.dre.brewery.integration.item;

import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.recipe.PluginItem;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderPluginItem extends PluginItem {

// When implementing this, put Brewery as softdepend in your plugin.yml!
// We're calling this as server start:
// PluginItem.registerForConfig("itemsadder", ItemsAdderPluginItem::new);

	@Override
	public boolean matches(ItemStack item) {
		if (!BConfig.hasItemsAdder) return false;
		CustomStack customStack = CustomStack.byItemStack(item);
		if (customStack != null) {
			return customStack.getNamespacedID().equalsIgnoreCase(getItemId());
		} else {
			return false;
		}
	}

}
