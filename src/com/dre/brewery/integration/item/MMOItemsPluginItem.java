package com.dre.brewery.integration.item;

import com.dre.brewery.P;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.recipe.PluginItem;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

public class MMOItemsPluginItem extends PluginItem {

// When implementing this, put Brewery as softdepend in your plugin.yml!
// We're calling this as server start:
// PluginItem.registerForConfig("mmoitems", MMOItemsPluginItem::new);

	@Override
	public boolean matches(ItemStack item) {
		if (!BConfig.hasMMOItems) return false;
		try {
			NBTItem nbtItem = NBTItem.get(item);
			if (!nbtItem.hasType()) return false;
			String[] typeAndId = getItemId().split(":");
			return nbtItem.getType().equalsIgnoreCase(typeAndId[0])
					&& nbtItem.getString("MMOITEMS_ITEM_ID").equalsIgnoreCase(typeAndId[1]);
		} catch (Throwable e) {
			e.printStackTrace();
			P.p.errorLog("Could not check MMOItems for Item ID: " + getItemId());
			BConfig.hasMMOItems = false;
			return false;
		}
	}

}
