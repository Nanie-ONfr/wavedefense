package com.wavedefense.arena;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Objects;

public enum Kit {
    NODEBUFF("NoDebuff", "Standard-Meta: Heilung + Speed"),
    BUILDUHC("BuildUHC", "Strategie & Utility"),
    CRYSTAL("Crystal", "Explosionsschaden"),
    BOXING("Boxing", "Aim Training - nur Hits"),
    GAPPLE("Gapple", "Hardcore Zermürbung"),
    SUMO("Sumo", "Knockback - nur Faust"),
    COMBO("Combo", "Schneller Kampf"),
    BRIDGE("Bridge", "Bridging-Modus"),
    AXE_SHIELD("Axe/Shield", "1.9+ Meta"),
    MACE("Mace", "1.21 Update"),
    ANCHOR("Anchor", "Respawn-Anker"),
    ARCHER("Archer", "Bogen-Fokus"),
    CLASSIC("Classic", "Oldschool"),
    SOUP("Soup", "Mushroom Soup Healing"),
    DEBUFF("Debuff", "Trank-PvP");

    private final String name;
    private final String description;

    Kit(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    private ItemStack enchant(ItemStack stack, Object... enchants) {
        ItemMeta meta = stack.getItemMeta();
        for (int i = 0; i < enchants.length; i += 2) {
            Enchantment enchantment = (Enchantment) enchants[i];
            int level = (Integer) enchants[i + 1];
            meta.addEnchant(enchantment, level, true);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createPotion(PotionType potionType, int count) {
        ItemStack stack = new ItemStack(Material.POTION, count);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        meta.setBasePotionType(potionType);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createSplashPotion(PotionType potionType, int count) {
        ItemStack stack = new ItemStack(Material.SPLASH_POTION, count);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        meta.setBasePotionType(potionType);
        stack.setItemMeta(meta);
        return stack;
    }

    public void applyToPlayer(Player player) {
        player.getInventory().clear();

        switch (this) {
            case NODEBUFF -> {
                // Diamond Sword Sharp5 Fire2 Unbreaking3
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3);

                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, new ItemStack(Material.ENDER_PEARL, 16));
                player.getInventory().setItem(2, createPotion(PotionType.LONG_SWIFTNESS, 1));
                player.getInventory().setItem(3, createPotion(PotionType.STRONG_SWIFTNESS, 1));
                player.getInventory().setItem(4, createPotion(PotionType.LONG_FIRE_RESISTANCE, 1));
                // Slots 5-7: Splash Healing
                player.getInventory().setItem(5, createSplashPotion(PotionType.STRONG_HEALING, 1));
                player.getInventory().setItem(6, createSplashPotion(PotionType.STRONG_HEALING, 1));
                player.getInventory().setItem(7, createSplashPotion(PotionType.STRONG_HEALING, 1));
                player.getInventory().setItem(8, new ItemStack(Material.COOKED_BEEF, 64));

                // Rows 2-3: Fill with Splash Healing (28 total, 3 already placed = 25 more)
                for (int i = 9; i < 34 && i < 36; i++) {
                    player.getInventory().setItem(i, createSplashPotion(PotionType.STRONG_HEALING, 1));
                }

                // Diamond Armor Prot4 Unbreaking3, Boots also Feather Falling 4
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
                enchant(legs, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 3);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
            }
            case BUILDUHC -> {
                // Diamond Sword Sharp3
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 3);

                ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
                ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);

                ItemStack bow = new ItemStack(Material.BOW);
                enchant(bow, Enchantment.POWER, 2);

                // Hotbar: Sword, Axe, Rod, Bow, Gapples x3, Lava, Water, Steak
                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, axe);
                player.getInventory().setItem(2, new ItemStack(Material.FISHING_ROD));
                player.getInventory().setItem(3, bow);
                player.getInventory().setItem(4, new ItemStack(Material.GOLDEN_APPLE, 3));
                player.getInventory().setItem(5, new ItemStack(Material.LAVA_BUCKET));
                player.getInventory().setItem(6, new ItemStack(Material.LAVA_BUCKET));
                player.getInventory().setItem(7, new ItemStack(Material.WATER_BUCKET));
                player.getInventory().setItem(8, new ItemStack(Material.COOKED_BEEF, 64));

                // Row 2: Pick, Cobble, Planks, Arrows, Gapples x6, Water
                player.getInventory().setItem(9, pick);
                player.getInventory().setItem(10, new ItemStack(Material.COBBLESTONE, 64));
                player.getInventory().setItem(11, new ItemStack(Material.OAK_PLANKS, 64));
                player.getInventory().setItem(12, new ItemStack(Material.ARROW, 24));
                player.getInventory().setItem(13, new ItemStack(Material.GOLDEN_APPLE, 6));
                player.getInventory().setItem(14, new ItemStack(Material.WATER_BUCKET));

                // Diamond Armor Prot2
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 2);
                ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 2);
                ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
                enchant(legs, Enchantment.PROTECTION, 2);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 2);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
            }
            case CRYSTAL -> {
                // Netherite Sword Sharp5
                ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 5);

                ItemStack bow = new ItemStack(Material.BOW);
                enchant(bow, Enchantment.PUNCH, 2);

                // Hotbar: Sword, Crystals, Crystals, Obsidian, Anchor, Glowstone, Bow, Gapples, Totem
                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, new ItemStack(Material.END_CRYSTAL, 64));
                player.getInventory().setItem(2, new ItemStack(Material.END_CRYSTAL, 64));
                player.getInventory().setItem(3, new ItemStack(Material.OBSIDIAN, 64));
                player.getInventory().setItem(4, new ItemStack(Material.RESPAWN_ANCHOR, 1));
                player.getInventory().setItem(5, new ItemStack(Material.GLOWSTONE, 64));
                player.getInventory().setItem(6, bow);
                player.getInventory().setItem(7, new ItemStack(Material.GOLDEN_APPLE, 64));
                player.getInventory().setItem(8, new ItemStack(Material.TOTEM_OF_UNDYING));

                // Armor: Helmet BlastProt4, Chest Prot4, Legs BlastProt4, Boots BlastProt4
                ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
                enchant(helmet, Enchantment.BLAST_PROTECTION, 4);
                ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 4);
                ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
                enchant(legs, Enchantment.BLAST_PROTECTION, 4);
                ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
                enchant(boots, Enchantment.BLAST_PROTECTION, 4);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
                player.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
            case BOXING -> {
                // Diamond Sword Unbreaking 10, nothing else
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                enchant(sword, Enchantment.UNBREAKING, 10);
                player.getInventory().setItem(0, sword);
            }
            case GAPPLE -> {
                // Diamond Sword Sharp5
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 5);

                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 64));
                player.getInventory().setItem(2, createPotion(PotionType.LONG_SWIFTNESS, 1));
                player.getInventory().setItem(3, createPotion(PotionType.STRONG_STRENGTH, 1));

                // Diamond Armor Prot4
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 4);
                ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 4);
                ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
                enchant(legs, Enchantment.PROTECTION, 4);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 4);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
            }
            case SUMO -> {
                // Nothing at all - fists only
            }
            case COMBO -> {
                // Diamond Sword Sharp5 Unbreaking10
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 10);

                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 64));
                player.getInventory().setItem(2, createPotion(PotionType.STRONG_STRENGTH, 1));
                player.getInventory().setItem(3, createPotion(PotionType.STRONG_SWIFTNESS, 1));
            }
            case BRIDGE -> {
                // Iron Sword
                ItemStack sword = new ItemStack(Material.IRON_SWORD);

                // Bow with Infinity
                ItemStack bow = new ItemStack(Material.BOW);
                enchant(bow, Enchantment.INFINITY, 1);

                // Diamond Pickaxe with Efficiency
                ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
                enchant(pick, Enchantment.EFFICIENCY, 3);

                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, bow);
                player.getInventory().setItem(2, pick);
                player.getInventory().setItem(3, new ItemStack(Material.TERRACOTTA, 64));
                player.getInventory().setItem(4, new ItemStack(Material.TERRACOTTA, 64));
                player.getInventory().setItem(5, new ItemStack(Material.GOLDEN_APPLE, 1));
                player.getInventory().setItem(6, new ItemStack(Material.ARROW, 1));
            }
            case AXE_SHIELD -> {
                // Diamond Axe Sharp5
                ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
                enchant(axe, Enchantment.SHARPNESS, 5);

                // Shield Unbreaking3
                ItemStack shield = new ItemStack(Material.SHIELD);
                enchant(shield, Enchantment.UNBREAKING, 3);

                // Diamond Sword (no enchants)
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);

                // Crossbow + Arrows
                ItemStack crossbow = new ItemStack(Material.CROSSBOW);

                player.getInventory().setItem(0, axe);
                player.getInventory().setItem(1, sword);
                player.getInventory().setItem(2, crossbow);
                player.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 12));
                player.getInventory().setItem(4, new ItemStack(Material.ARROW, 64));

                // Diamond Armor Prot4
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 4);
                ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 4);
                ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
                enchant(legs, Enchantment.PROTECTION, 4);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 4);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
                player.getInventory().setItemInOffHand(shield);
            }
            case MACE -> {
                // Mace with Breach + Density
                ItemStack mace = new ItemStack(Material.MACE);
                enchant(mace, Enchantment.BREACH, 4, Enchantment.DENSITY, 5);

                player.getInventory().setItem(0, mace);
                player.getInventory().setItem(1, new ItemStack(Material.WIND_CHARGE, 16));
                player.getInventory().setItem(2, new ItemStack(Material.TOTEM_OF_UNDYING));

                // Full Netherite Prot4
                ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 4);
                ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 4);
                ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
                enchant(legs, Enchantment.PROTECTION, 4);
                ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 4);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
                player.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
            }
            case ANCHOR -> {
                // Netherite Sword Sharp5
                ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 5);

                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, new ItemStack(Material.RESPAWN_ANCHOR, 64));
                player.getInventory().setItem(2, new ItemStack(Material.GLOWSTONE, 64));
                player.getInventory().setItem(3, new ItemStack(Material.GOLDEN_CARROT, 64));

                // Netherite Armor BlastProt4
                ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
                enchant(helmet, Enchantment.BLAST_PROTECTION, 4);
                ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                enchant(chest, Enchantment.BLAST_PROTECTION, 4);
                ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
                enchant(legs, Enchantment.BLAST_PROTECTION, 4);
                ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
                enchant(boots, Enchantment.BLAST_PROTECTION, 4);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
            }
            case ARCHER -> {
                // Bow Power5 Punch2 Unbreaking3 Infinity
                ItemStack bow = new ItemStack(Material.BOW);
                enchant(bow, Enchantment.POWER, 5, Enchantment.PUNCH, 2, Enchantment.UNBREAKING, 3, Enchantment.INFINITY, 1);

                player.getInventory().setItem(0, bow);
                player.getInventory().setItem(1, new ItemStack(Material.ARROW, 1));
                player.getInventory().setItem(2, new ItemStack(Material.COOKED_BEEF, 16));

                // Only Helmet and Boots Prot4
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 4);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 4);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setBoots(boots);
            }
            case CLASSIC -> {
                // Iron Sword Sharp1
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 1);

                // Bow
                ItemStack bow = new ItemStack(Material.BOW);

                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, new ItemStack(Material.FISHING_ROD));
                player.getInventory().setItem(2, bow);
                player.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 4));
                player.getInventory().setItem(4, new ItemStack(Material.ARROW, 16));

                // Full Iron Armor Prot1
                ItemStack helmet = new ItemStack(Material.IRON_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 1);
                ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 1);
                ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
                enchant(legs, Enchantment.PROTECTION, 1);
                ItemStack boots = new ItemStack(Material.IRON_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 1);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
            }
            case SOUP -> {
                // Diamond Sword (no enchants)
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                player.getInventory().setItem(0, sword);

                // Fill slots 1-8 (hotbar) and rows 2-4 with mushroom stew (doesn't stack)
                for (int i = 1; i <= 35; i++) {
                    player.getInventory().setItem(i, new ItemStack(Material.MUSHROOM_STEW));
                }

                // Full Iron Armor
                ItemStack helmet = new ItemStack(Material.IRON_HELMET);
                ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
                ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
                ItemStack boots = new ItemStack(Material.IRON_BOOTS);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
            }
            case DEBUFF -> {
                // Diamond Sword Sharp5
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                enchant(sword, Enchantment.SHARPNESS, 5);

                player.getInventory().setItem(0, sword);

                // Splash Healing II x24 in slots 1-8 and rows 2-3
                for (int i = 1; i <= 24; i++) {
                    player.getInventory().setItem(i, createSplashPotion(PotionType.STRONG_HEALING, 1));
                }
                // Splash Poison II x2
                player.getInventory().setItem(25, createSplashPotion(PotionType.STRONG_POISON, 1));
                player.getInventory().setItem(26, createSplashPotion(PotionType.STRONG_POISON, 1));
                // Splash Slowness x2
                player.getInventory().setItem(27, createSplashPotion(PotionType.STRONG_SLOWNESS, 1));
                player.getInventory().setItem(28, createSplashPotion(PotionType.STRONG_SLOWNESS, 1));
                // Splash Harming II x2
                player.getInventory().setItem(29, createSplashPotion(PotionType.STRONG_HARMING, 1));
                player.getInventory().setItem(30, createSplashPotion(PotionType.STRONG_HARMING, 1));

                // Full Diamond Armor Prot4
                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                enchant(helmet, Enchantment.PROTECTION, 4);
                ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
                enchant(chest, Enchantment.PROTECTION, 4);
                ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
                enchant(legs, Enchantment.PROTECTION, 4);
                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                enchant(boots, Enchantment.PROTECTION, 4);

                player.getInventory().setHelmet(helmet);
                player.getInventory().setChestplate(chest);
                player.getInventory().setLeggings(legs);
                player.getInventory().setBoots(boots);
            }
        }

        // Full health and food
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
        player.setFoodLevel(20);
    }

    public void applyToBot(Mob bot) {
        bot.getEquipment().clear();

        switch (this) {
            case NODEBUFF, GAPPLE, DEBUFF -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                bot.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                bot.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                bot.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                bot.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            }
            case COMBO -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
            }
            case BUILDUHC -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                bot.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                bot.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                bot.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                bot.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            }
            case CLASSIC -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                bot.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                bot.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                bot.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                bot.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
            }
            case CRYSTAL, ANCHOR -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
                bot.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                bot.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                bot.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                bot.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            }
            case BOXING, SUMO -> {
                // No equipment
            }
            case BRIDGE -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            }
            case AXE_SHIELD -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
                bot.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
                bot.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                bot.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                bot.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                bot.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            }
            case MACE -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.MACE));
                bot.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                bot.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                bot.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                bot.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            }
            case ARCHER -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                bot.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                bot.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            }
            case SOUP -> {
                bot.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
                bot.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                bot.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                bot.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                bot.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
            }
        }

        // Prevent drops
        bot.getEquipment().setHelmetDropChance(0.0f);
        bot.getEquipment().setChestplateDropChance(0.0f);
        bot.getEquipment().setLeggingsDropChance(0.0f);
        bot.getEquipment().setBootsDropChance(0.0f);
        bot.getEquipment().setItemInMainHandDropChance(0.0f);
        bot.getEquipment().setItemInOffHandDropChance(0.0f);
    }
}
