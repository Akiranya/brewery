package com.dre.brewery.recipe;

import com.dre.brewery.P;
import com.dre.brewery.utility.BUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BEffect {

	private PotionEffectType type;
	private short minLvl;
	private short maxLvl;
	private short minDuration;
	private short maxDuration;
	private boolean hidden = false;


	public BEffect(PotionEffectType type, short minLvl, short maxLvl, short minDuration, short maxDuration, boolean hidden) {
		this.type = type;
		this.minLvl = minLvl;
		this.maxLvl = maxLvl;
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
		this.hidden = hidden;
	}

	public BEffect(String effectString) {
		String[] effectSplit = effectString.split("/");
		String effect = effectSplit[0];
		if (effect.equalsIgnoreCase("WEAKNESS") ||
				effect.equalsIgnoreCase("INCREASE_DAMAGE") ||
				effect.equalsIgnoreCase("SLOW") ||
				effect.equalsIgnoreCase("SPEED") ||
				effect.equalsIgnoreCase("REGENERATION")) {
			// hide these effects as they put crap into lore
			// Dont write Regeneration into Lore, its already there storing data!
			hidden = true;
		} else if (effect.endsWith("X")) {
			hidden = true;
			effect = effect.substring(0, effect.length() - 1);
		}
		type = PotionEffectType.getByName(effect);
		if (type == null) {
			P.p.errorLog("Effect: " + effect + " does not exist!");
			return;
		}

		if (effectSplit.length == 3) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				setLvl(range);
			} else {
				setLvl(range);
				range = effectSplit[2].split("-");
				setDuration(range);
			}
		} else if (effectSplit.length == 2) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				setLvl(range);
			} else {
				setDuration(range);
				maxLvl = 3;
				minLvl = 1;
			}
		} else {
			maxDuration = 20;
			minDuration = 10;
			maxLvl = 3;
			minLvl = 1;
		}
	}

	private void setLvl(String[] range) {
		if (range.length == 1) {
			maxLvl = (short) P.p.parseInt(range[0]);
			minLvl = 1;
		} else {
			maxLvl = (short) P.p.parseInt(range[1]);
			minLvl = (short) P.p.parseInt(range[0]);
		}
	}

	private void setDuration(String[] range) {
		if (range.length == 1) {
			maxDuration = (short) P.p.parseInt(range[0]);
			minDuration = (short) (maxDuration / 8);
		} else {
			maxDuration = (short) P.p.parseInt(range[1]);
			minDuration = (short) P.p.parseInt(range[0]);
		}
	}

	public PotionEffect generateEffect(int quality) {
		int duration = calcDuration(quality);
		int lvl = calcLvl(quality);

		if (lvl < 1 || (duration < 1 && !type.isInstant())) {
			return null;
		}

		duration *= 20;
		if (!P.use1_14) {
			@SuppressWarnings("deprecation")
			double modifier = type.getDurationModifier();
			duration /= modifier;
		}
		return type.createEffect(duration, lvl - 1);
	}

	public void apply(int quality, Player player) {
		PotionEffect effect = generateEffect(quality);
		if (effect != null) {
			BUtil.reapplyPotionEffect(player, effect, true);
		}
	}

	public int calcDuration(float quality) {
		return (int) Math.round(minDuration + ((maxDuration - minDuration) * (quality / 10.0)));
	}

	public int calcLvl(float quality) {
		return (int) Math.round(minLvl + ((maxLvl - minLvl) * (quality / 10.0)));
	}

	public void writeInto(PotionMeta meta, int quality) {
		if ((calcDuration(quality) > 0 || type.isInstant()) && calcLvl(quality) > 0) {
			meta.addCustomEffect(type.createEffect(0, 0), true);
		} else {
			meta.removeCustomEffect(type);
		}
	}

	public boolean isValid() {
		return type != null && minLvl >= 0 && maxLvl >= 0 && minDuration >= 0 && maxDuration >= 0;
	}

	public boolean isHidden() {
		return hidden;
	}
}
