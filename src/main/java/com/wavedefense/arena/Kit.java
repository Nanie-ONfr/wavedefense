package com.wavedefense.arena;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

public enum Kit {
    MACE("Mace", "Mace + Wind Charges + Netherite"),
    SWORD("Sword", "Diamond Sword + Diamond Armor"),
    AXE("Axe", "Diamond Axe + Shield + Diamond Armor"),
    BOW("Bow", "Power Bow + Arrows"),
    CRYSTAL("Crystal", "Crystals + Obsidian + Totems"),
    UHC("UHC", "Diamond Gear + Gapples + Buckets"),
    SHIELD("Shield", "Sword + Shield + Diamond Armor");

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

    private ItemStack enchant(ItemStack stack, ServerPlayerEntity player, Object... enchants) {
        var registry = player.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

        for (int i = 0; i < enchants.length; i += 2) {
            var key = enchants[i];
            int level = (Integer) enchants[i + 1];
            var entry = registry.getOptional((net.minecraft.registry.RegistryKey<Enchantment>) key);
            if (entry.isPresent()) {
                builder.add(entry.get(), level);
            }
        }

        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        return stack;
    }

    public void applyToPlayer(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Clear inventory
        player.getInventory().clear();

        switch (this) {
            case MACE -> {
                // MACE KIT: Full Netherite Prot4, Mace with Wind Burst + Density
                // Hotbar: Mace, Mace2, Sword, Axe, Gapples, Pearls, Wind Charges, Shield, Totem
                ItemStack mace1 = new ItemStack(Items.MACE);
                enchant(mace1, serverPlayer, Enchantments.BREACH, 4, Enchantments.UNBREAKING, 3);

                ItemStack mace2 = new ItemStack(Items.MACE);
                enchant(mace2, serverPlayer, Enchantments.WIND_BURST, 3, Enchantments.DENSITY, 5, Enchantments.UNBREAKING, 3);

                ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
                enchant(sword, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
                enchant(axe, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                ItemStack shield = new ItemStack(Items.SHIELD);
                enchant(shield, serverPlayer, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1);

                player.getInventory().setStack(0, mace1);
                player.getInventory().setStack(1, mace2);
                player.getInventory().setStack(2, sword);
                player.getInventory().setStack(3, axe);
                player.getInventory().setStack(4, new ItemStack(Items.GOLDEN_APPLE, 64));
                player.getInventory().setStack(5, new ItemStack(Items.ENDER_PEARL, 16));
                player.getInventory().setStack(6, new ItemStack(Items.WIND_CHARGE, 64));
                player.getInventory().setStack(7, shield);
                player.getInventory().setStack(8, new ItemStack(Items.TOTEM_OF_UNDYING));

                // Armor - Full Netherite Prot4
                ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
                enchant(helmet, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
                enchant(chest, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Items.NETHERITE_LEGGINGS);
                enchant(legs, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
                enchant(boots, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.FEATHER_FALLING, 4, Enchantments.UNBREAKING, 3);

                player.equipStack(EquipmentSlot.HEAD, helmet);
                player.equipStack(EquipmentSlot.CHEST, chest);
                player.equipStack(EquipmentSlot.LEGS, legs);
                player.equipStack(EquipmentSlot.FEET, boots);
                player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
            case SWORD -> {
                // SWORD KIT: Diamond Armor Prot4, Diamond Sword Sharp5
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                enchant(sword, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                player.getInventory().setStack(0, sword);
                player.getInventory().setStack(1, new ItemStack(Items.GOLDEN_APPLE, 64));
                player.getInventory().setStack(2, new ItemStack(Items.ENDER_PEARL, 16));
                player.getInventory().setStack(8, new ItemStack(Items.COOKED_BEEF, 64));

                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
                enchant(helmet, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                enchant(chest, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                enchant(legs, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                enchant(boots, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);

                player.equipStack(EquipmentSlot.HEAD, helmet);
                player.equipStack(EquipmentSlot.CHEST, chest);
                player.equipStack(EquipmentSlot.LEGS, legs);
                player.equipStack(EquipmentSlot.FEET, boots);
                player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case AXE -> {
                // AXE KIT: Diamond Armor, Diamond Axe + Sword + Shield
                ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
                enchant(axe, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                enchant(sword, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                ItemStack bow = new ItemStack(Items.BOW);
                enchant(bow, serverPlayer, Enchantments.POWER, 5, Enchantments.UNBREAKING, 3);

                player.getInventory().setStack(0, axe);
                player.getInventory().setStack(1, sword);
                player.getInventory().setStack(2, bow);
                player.getInventory().setStack(3, new ItemStack(Items.GOLDEN_APPLE, 64));
                player.getInventory().setStack(4, new ItemStack(Items.ARROW, 64));
                player.getInventory().setStack(8, new ItemStack(Items.COOKED_BEEF, 64));

                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
                enchant(helmet, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                enchant(chest, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                enchant(legs, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                enchant(boots, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);

                player.equipStack(EquipmentSlot.HEAD, helmet);
                player.equipStack(EquipmentSlot.CHEST, chest);
                player.equipStack(EquipmentSlot.LEGS, legs);
                player.equipStack(EquipmentSlot.FEET, boots);
                player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case BOW -> {
                // BOW KIT: Diamond Armor, Power Bow + Infinity
                ItemStack bow = new ItemStack(Items.BOW);
                enchant(bow, serverPlayer, Enchantments.POWER, 5, Enchantments.INFINITY, 1, Enchantments.UNBREAKING, 3);

                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                enchant(sword, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                player.getInventory().setStack(0, bow);
                player.getInventory().setStack(1, sword);
                player.getInventory().setStack(2, new ItemStack(Items.ARROW, 1)); // 1 arrow for infinity
                player.getInventory().setStack(3, new ItemStack(Items.GOLDEN_APPLE, 64));
                player.getInventory().setStack(4, new ItemStack(Items.ENDER_PEARL, 16));
                player.getInventory().setStack(8, new ItemStack(Items.COOKED_BEEF, 64));

                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
                enchant(helmet, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                enchant(chest, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                enchant(legs, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                enchant(boots, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);

                player.equipStack(EquipmentSlot.HEAD, helmet);
                player.equipStack(EquipmentSlot.CHEST, chest);
                player.equipStack(EquipmentSlot.LEGS, legs);
                player.equipStack(EquipmentSlot.FEET, boots);
            }
            case CRYSTAL -> {
                // CRYSTAL KIT: Netherite Armor Blast Prot, Crystals + Obsidian + Totems
                ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
                enchant(sword, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                player.getInventory().setStack(0, sword);
                player.getInventory().setStack(1, new ItemStack(Items.END_CRYSTAL, 64));
                player.getInventory().setStack(2, new ItemStack(Items.END_CRYSTAL, 64));
                player.getInventory().setStack(3, new ItemStack(Items.OBSIDIAN, 64));
                player.getInventory().setStack(4, new ItemStack(Items.OBSIDIAN, 64));
                player.getInventory().setStack(5, new ItemStack(Items.GOLDEN_APPLE, 64));
                player.getInventory().setStack(6, new ItemStack(Items.ENDER_PEARL, 16));
                player.getInventory().setStack(7, new ItemStack(Items.TOTEM_OF_UNDYING));
                player.getInventory().setStack(8, new ItemStack(Items.TOTEM_OF_UNDYING));

                // Extra totems in inventory
                for (int i = 9; i < 18; i++) {
                    player.getInventory().setStack(i, new ItemStack(Items.TOTEM_OF_UNDYING));
                }

                ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
                enchant(helmet, serverPlayer, Enchantments.BLAST_PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
                enchant(chest, serverPlayer, Enchantments.BLAST_PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Items.NETHERITE_LEGGINGS);
                enchant(legs, serverPlayer, Enchantments.BLAST_PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
                enchant(boots, serverPlayer, Enchantments.BLAST_PROTECTION, 4, Enchantments.UNBREAKING, 3);

                player.equipStack(EquipmentSlot.HEAD, helmet);
                player.equipStack(EquipmentSlot.CHEST, chest);
                player.equipStack(EquipmentSlot.LEGS, legs);
                player.equipStack(EquipmentSlot.FEET, boots);
                player.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
            case UHC -> {
                // UHC KIT: Diamond Armor Prot3, Sword Sharp3, Fishing Rod, Buckets
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                enchant(sword, serverPlayer, Enchantments.SHARPNESS, 3, Enchantments.UNBREAKING, 3);

                ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
                enchant(axe, serverPlayer, Enchantments.EFFICIENCY, 3);

                ItemStack bow = new ItemStack(Items.BOW);
                enchant(bow, serverPlayer, Enchantments.POWER, 3, Enchantments.UNBREAKING, 3);

                ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
                enchant(pick, serverPlayer, Enchantments.EFFICIENCY, 3);

                player.getInventory().setStack(0, sword);
                player.getInventory().setStack(1, axe);
                player.getInventory().setStack(2, new ItemStack(Items.FISHING_ROD));
                player.getInventory().setStack(3, bow);
                player.getInventory().setStack(4, new ItemStack(Items.GOLDEN_APPLE, 8));
                player.getInventory().setStack(5, new ItemStack(Items.LAVA_BUCKET));
                player.getInventory().setStack(6, new ItemStack(Items.WATER_BUCKET));
                player.getInventory().setStack(7, new ItemStack(Items.OAK_PLANKS, 64));
                player.getInventory().setStack(8, new ItemStack(Items.COOKED_BEEF, 64));

                // Second row
                player.getInventory().setStack(9, pick);
                player.getInventory().setStack(10, new ItemStack(Items.COBWEB, 8));
                player.getInventory().setStack(11, new ItemStack(Items.ARROW, 32));

                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
                enchant(helmet, serverPlayer, Enchantments.PROTECTION, 3, Enchantments.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                enchant(chest, serverPlayer, Enchantments.PROTECTION, 3, Enchantments.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                enchant(legs, serverPlayer, Enchantments.PROTECTION, 2, Enchantments.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                enchant(boots, serverPlayer, Enchantments.PROTECTION, 3, Enchantments.UNBREAKING, 3);

                player.equipStack(EquipmentSlot.HEAD, helmet);
                player.equipStack(EquipmentSlot.CHEST, chest);
                player.equipStack(EquipmentSlot.LEGS, legs);
                player.equipStack(EquipmentSlot.FEET, boots);
            }
            case SHIELD -> {
                // SHIELD KIT: Diamond Armor Prot4, Sword + Axe + Shield
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                enchant(sword, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
                enchant(axe, serverPlayer, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3);

                player.getInventory().setStack(0, sword);
                player.getInventory().setStack(1, axe);
                player.getInventory().setStack(2, new ItemStack(Items.GOLDEN_APPLE, 64));
                player.getInventory().setStack(3, new ItemStack(Items.ENDER_PEARL, 16));
                player.getInventory().setStack(8, new ItemStack(Items.COOKED_BEEF, 64));

                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
                enchant(helmet, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                enchant(chest, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                enchant(legs, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                enchant(boots, serverPlayer, Enchantments.PROTECTION, 4, Enchantments.UNBREAKING, 3);

                ItemStack shield = new ItemStack(Items.SHIELD);
                enchant(shield, serverPlayer, Enchantments.UNBREAKING, 3);

                player.equipStack(EquipmentSlot.HEAD, helmet);
                player.equipStack(EquipmentSlot.CHEST, chest);
                player.equipStack(EquipmentSlot.LEGS, legs);
                player.equipStack(EquipmentSlot.FEET, boots);
                player.equipStack(EquipmentSlot.OFFHAND, shield);
            }
        }

        // Full health
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
    }

    public void applyToBot(MobEntity bot) {
        // Clear equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            bot.equipStack(slot, ItemStack.EMPTY);
        }

        switch (this) {
            case MACE -> {
                bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.MACE));
                bot.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
                bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
            }
            case SWORD -> {
                bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                bot.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case AXE -> {
                bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
                bot.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case BOW -> {
                bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case CRYSTAL -> {
                bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
                bot.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
                bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
            }
            case UHC -> {
                bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case SHIELD -> {
                bot.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
                bot.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                bot.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                bot.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                bot.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                bot.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
        }

        // Prevent drops
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            bot.setEquipmentDropChance(slot, 0.0f);
        }
    }
}
