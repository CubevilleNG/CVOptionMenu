package org.cubeville.cvoptionmenu;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CVOptionMenu extends JavaPlugin implements Listener {

    Map<UUID, String[]> playerMenu;
    Map<UUID, Location> playerLocation;
    Map<UUID, String> playerLeaveMessage;
    
    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        playerMenu = new HashMap<>();
        playerLocation = new HashMap<>();
        playerLeaveMessage = new HashMap<>();

        new BukkitRunnable() {
            public void run() {
                for(UUID playerId: playerLocation.keySet()) {
                    Player player = Bukkit.getServer().getPlayer(playerId);
                    if(player == null) {
                        removePlayer(playerId);
                    }
                    else {
                        if(!player.getLocation().getWorld().getUID().equals(playerLocation.get(playerId).getWorld().getUID())) {
                            removePlayer(playerId);
                        }
                        else if(player.getLocation().distance(playerLocation.get(playerId)) > 5) {
                            player.sendMessage(playerLeaveMessage.get(playerId));
                            removePlayer(playerId);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 5, 5);
    }
    
    private void removePlayer(UUID playerId) {
        playerMenu.remove(playerId);
        playerLocation.remove(playerId);
        playerLeaveMessage.remove(playerId);
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("omenu")) {
            String playerName = args[0];
            Player player = Bukkit.getServer().getPlayer(playerName);
            if(player == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            String cstr = "";
            for(int i = 1; i < args.length; i++) {
                if(i > 1) cstr += " ";
                cstr += args[i];
            }

            StringTokenizer tk = new StringTokenizer(cstr, "|");

            String text = ChatColor.translateAlternateColorCodes('&', tk.nextToken());
            StringTokenizer texttk = new StringTokenizer(text, "\\");

            String leaveMessage = ChatColor.translateAlternateColorCodes('&', tk.nextToken());
            
            while(texttk.hasMoreTokens()) {
                player.sendMessage(texttk.nextToken());
            }
            
            String[] options = new String[tk.countTokens()];
            for(int i = 0; i < options.length; i++) {
                options[i] = tk.nextToken();
            }

            playerMenu.put(player.getUniqueId(), options);
            playerLocation.put(player.getUniqueId(), player.getLocation());
            playerLeaveMessage.put(player.getUniqueId(), leaveMessage);
            
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        if(event.isCancelled()) return;

        if(event.getMessage().equals("0")) {
            event.setCancelled(true);
            return;
        }
        
        int idx = "123456789".indexOf(event.getMessage());
        if(event.getMessage().length() == 1 && idx >= 0) {
            event.setCancelled(true);
            UUID playerId = event.getPlayer().getUniqueId();
            if(playerMenu.containsKey(playerId)) {
                String[] options = playerMenu.get(playerId);
                if(idx < options.length) {
                    Server server = Bukkit.getServer();
                    server.dispatchCommand(server.getConsoleSender(), options[idx]);
                    removePlayer(playerId);
                }
            }
        }
        
    }
    
}
