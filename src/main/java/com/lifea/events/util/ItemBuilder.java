package com.lifea.events.util;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemBuilder {
   private final ItemStack stack;
   private final ItemMeta meta;

   public ItemBuilder(Material material) {
      this.stack = new ItemStack(material);
      this.meta = this.stack.getItemMeta();
      this.hideVanillaClutter();
   }

   public ItemBuilder(ItemStack base) {
      this.stack = base.clone();
      this.meta = this.stack.getItemMeta();
      this.hideVanillaClutter();
   }

   private void hideVanillaClutter() {
      this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_DYE});
   }

   public ItemBuilder name(String miniMessage) {
      Component c = MessageUtil.parse(miniMessage).decoration(TextDecoration.ITALIC, false);
      this.meta.displayName(c);
      return this;
   }

   public ItemBuilder lore(String... lines) {
      List<Component> lore = new ArrayList<>();

      for (String line : lines) {
         lore.add(MessageUtil.parse(line).decoration(TextDecoration.ITALIC, false));
      }

      this.meta.lore(lore);
      return this;
   }

   public ItemBuilder amount(int amount) {
      this.stack.setAmount(amount);
      return this;
   }

   public ItemBuilder glow(boolean glow) {
      if (glow) {
         this.meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
         this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
      }

      return this;
   }

   public ItemBuilder unbreakable(boolean unbreakable) {
      this.meta.setUnbreakable(unbreakable);
      if (unbreakable) {
         this.meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_UNBREAKABLE});
      }

      return this;
   }

   public ItemBuilder enchant(Enchantment enchantment, int level) {
      this.meta.addEnchant(enchantment, level, true);
      return this;
   }

   public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
      PersistentDataContainer container = this.meta.getPersistentDataContainer();
      container.set(key, type, value);
      return this;
   }

   public ItemStack build() {
      this.stack.setItemMeta(this.meta);
      return this.stack;
   }
}
