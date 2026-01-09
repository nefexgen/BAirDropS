package org.by1337.bairdrop.ItemUtil;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SkullUtils {
    @NotNull
    public static ItemStack getSkull(@NotNull String skinUrl) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (skinUrl.isEmpty())
            return head;
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null)
            return head;

        PlayerProfile profile = createProfile(skinUrl);
        if (profile != null) {
            headMeta.setPlayerProfile(profile);
        }
        head.setItemMeta(headMeta);
        return head;
    }

    @NotNull
    public static PlayerProfile createProfile(@NotNull String textureValue) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", textureValue));
        return profile;
    }
}