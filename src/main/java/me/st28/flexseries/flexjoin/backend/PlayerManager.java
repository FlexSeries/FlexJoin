/**
 * FlexJoin - Licensed under the MIT License (MIT)
 *
 * Copyright (c) Stealth2800 <http://stealthyone.com/>
 * Copyright (c) contributors <https://github.com/FlexSeries>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.st28.flexseries.flexjoin.backend;

import me.st28.flexseries.flexcore.FlexCore;
import me.st28.flexseries.flexcore.events.PlayerJoinLoadedEvent;
import me.st28.flexseries.flexcore.events.PlayerLeaveEvent;
import me.st28.flexseries.flexcore.logging.LogHelper;
import me.st28.flexseries.flexcore.message.MessageReference;
import me.st28.flexseries.flexcore.plugin.module.FlexModule;
import me.st28.flexseries.flexcore.util.ScreenTitle;
import me.st28.flexseries.flexjoin.FlexJoin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerManager extends FlexModule<FlexJoin> implements Listener {

    private String messageFirstJoin;
    private String messageJoin;
    private String messageQuit;

    private boolean isTitleEnabled;
    private int titleDelay;
    private int titleFadeInTime;
    private int titleStayTime;
    private int titleFadeOutTime;
    private final Map<String, String> titleFirstLine = new HashMap<>();
    private final Map<String, String> titleSecondLine = new HashMap<>();

    public PlayerManager(FlexJoin plugin) {
        super(plugin, "players", "Handles player joining and leaving", false);
    }

    @Override
    protected void handleReload() {
        FileConfiguration config = getConfig();

        messageFirstJoin = config.getString("messages.first join", "&a&l+ &e{DISPNAME} &a&ojoined for the first time, welcome!");
        messageJoin = config.getString("messages.join", "&a&l+ &e{DISPNAME}");
        messageQuit = config.getString("messages.quit", "&c&l- &e{DISPNAME}");

        /* Load title config */
        isTitleEnabled = config.getBoolean("login title.enabled", true);
        titleDelay = config.getInt("login title.time.delay", 2);
        titleFadeInTime = config.getInt("login title.time.fade in", 1);
        titleStayTime = config.getInt("login title.time.stay", 3);
        titleFadeOutTime = config.getInt("login title.time.fade out", 1);

        titleFirstLine.clear();
        ConfigurationSection firstConfig = config.getConfigurationSection("login title.message.first line");
        if (firstConfig != null) {
            for (String name : firstConfig.getKeys(false)) {
                titleFirstLine.put(name.toLowerCase(), firstConfig.getString(name));
            }
        }

        titleSecondLine.clear();
        ConfigurationSection secondConfig = config.getConfigurationSection("login title.message.second line");
        if (secondConfig != null) {
            for (String name : secondConfig.getKeys(false)) {
                titleSecondLine.put(name.toLowerCase(), secondConfig.getString(name));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinLoaded(PlayerJoinLoadedEvent e) {
        Player player = e.getPlayer();

        boolean isFirstJoin = e.getCustomData().containsKey("firstJoin") && (boolean) e.getCustomData().get("firstJoin");

        if (e.getJoinMessage() != null) {
            if (isFirstJoin && messageFirstJoin != null) {
                e.setJoinMessage(MessageReference.createPlain(messageFirstJoin.replace("{NAME}", player.getName()).replace("{DISPNAME}", player.getDisplayName())));
            } else if (messageJoin != null) {
                e.setJoinMessage(MessageReference.createPlain(messageJoin.replace("{NAME}", player.getName()).replace("{DISPNAME}", player.getDisplayName())));
            }
        }

        if (!isTitleEnabled) {
            return;
        }

        String title;
        String subtitle;

        if (isFirstJoin) {
            title = titleFirstLine.get("first join");
            if (title == null) {
                title = titleFirstLine.get("default");
            }

            if (title == null) {
                title = "" + ChatColor.RED + ChatColor.ITALIC + "title undefined";
            }

            subtitle = titleSecondLine.get("first join");
            if (subtitle == null) {
                subtitle = titleSecondLine.get("default");
            }

            if (subtitle == null) {
                subtitle = "" + ChatColor.RED + ChatColor.ITALIC + "subtitle undefined";
            }
        } else {
            title = titleFirstLine.get("default");
            if (title == null) {
                title = "" + ChatColor.RED + ChatColor.ITALIC + "title undefined";
            }

            subtitle = titleSecondLine.get("default");
            if (subtitle == null) {
                subtitle = "" + ChatColor.RED + ChatColor.ITALIC + "subtitle undefined";
            }
        }

        String serverName = "Minecraft Server";
        try {
            serverName = JavaPlugin.getPlugin(FlexCore.class).getServerName();
        } catch (Exception ex) {
            LogHelper.warning(this, "Unable to retrieve server name from FlexCore.", ex);
        }

        title = title.replace("{NAME}", player.getName()).replace("{DISPNAME}", player.getDisplayName()).replace("{SERVER}", serverName);
        subtitle = subtitle.replace("{NAME}", player.getName()).replace("{DISPNAME}", player.getDisplayName()).replace("{SERVER}", serverName);

        ScreenTitle toSend = new ScreenTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                titleFadeInTime,
                titleStayTime,
                titleFadeOutTime
        );

        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (Bukkit.getPlayer(uuid) != null) {
                toSend.sendTo(Bukkit.getPlayer(uuid));
            }
        }, titleDelay);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerLeaveEvent e) {
        if (messageQuit != null && e.getLeaveMessage() != null) {
            e.setLeaveMessage(MessageReference.createPlain(messageQuit.replace("{NAME}", e.getPlayer().getName()).replace("{DISPNAME}", e.getPlayer().getDisplayName())));
        }
    }

}