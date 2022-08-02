package com.dre.brewery.utility;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TownyUtil {

    private TownyUtil() {
    }

    public static boolean isInsideTown(@NotNull Location location) {
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
        if (townBlock != null) {
            Town town = townBlock.getTownOrNull();
            return town != null;
        }
        return false;
    }

    /**
     * Returns whether a player is allowed to build Brewery props. ALLOW means
     * the player is doing this thing *inside* their town. Note that this method
     * always returns true for players who have brewery.admin permission.
     *
     * @param location the location where this action occurs
     * @param player   the player who initiates this action
     * @return true if the player is permitted, otherwise false
     */
    public static boolean canBuild(@NotNull Location location, @NotNull Player player, boolean checkInsideTown) {
        if (checkInsideTown && isInsideTown(location)) {
            return PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.BUILD);
        }
        return player.hasPermission("brewery.admin") || PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.BUILD);
    }

    /**
     * Returns whether a player is allowed to destroy Brewery props. ALLOW means
     * the player is doing this thing *inside* their town. Note that this method
     * always returns true for players who have brewery.admin permission.
     *
     * @param location the location where this action occurs
     * @param player   the player who initiates this action
     * @return true if the player is permitted, otherwise false
     */
    public static boolean canDestroy(@NotNull Location location, @NotNull Player player, boolean checkInsideTown) {
        if (checkInsideTown && isInsideTown(location)) {
            return PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.DESTROY);
        }
        return player.hasPermission("brewery.admin") || PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.DESTROY);
    }

    /**
     * Returns whether a player is allowed to use Brewery props. ALLOW means the
     * player is doing this thing *inside* their town. Note that this method
     * always returns true for players who have brewery.admin permission.
     *
     * @param location the location where this action occurs
     * @param player   the player who initiates this action
     * @return true if the player is permitted, otherwise false
     */
    public static boolean canUseItem(@NotNull Location location, @NotNull Player player, boolean checkInsideTown) {
        if (checkInsideTown && isInsideTown(location)) {
            return PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.ITEM_USE);
        }
        return player.hasPermission("brewery.admin") || PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.DESTROY);
    }

    /**
     * Returns whether a player is allowed to switch Brewery props. ALLOW means
     * the player is doing this thing *inside* their town. Note that this method
     * always returns true for players who have brewery.admin permission.
     *
     * @param location the location where this action occurs
     * @param player   the player who initiates this action
     * @return true if the player is permitted, otherwise false
     */
    public static boolean canSwitch(@NotNull Location location, @NotNull Player player, boolean checkInsideTown) {
        if (checkInsideTown && isInsideTown(location)) {
            return PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.SWITCH);
        }
        return player.hasPermission("brewery.admin") || PlayerCacheUtil.getCachePermission(player, location, location.getBlock().getType(), TownyPermission.ActionType.SWITCH);
    }

}