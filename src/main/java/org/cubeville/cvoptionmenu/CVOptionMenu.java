package org.cubeville.cvoptionmenu;

import java.util.HashMap;
import java.util.Map;
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

    Map<UUID, ActiveMenu> playerMenu;
    
    private class ActiveMenu {
        
        public ActiveMenu(String[] optionCommands, Location location, String leaveMessage, double leaveRadius) {
            this.optionCommands = optionCommands;
            this.location = location;
            this.leaveMessage = leaveMessage;
            this.leaveRadius = leaveRadius;
        }

        public String getOptionCommand(int Nr) { return optionCommands[Nr]; }
        public boolean hasOption(int Nr) { return Nr >= 0 && Nr < optionCommands.length; }
        public Location getLocation() { return location; }
        public String getLeaveMessage() { return leaveMessage; }
        public double getLeaveRadius() { return leaveRadius; }
        
        private String[] optionCommands;
        private Location location;
        private String leaveMessage;
        private double leaveRadius;
    }
    
    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);

        playerMenu = new HashMap<>();

        new BukkitRunnable() {
            public void run() {
                for(UUID playerId: playerMenu.keySet()) {
                    Player player = Bukkit.getServer().getPlayer(playerId);
                    if(player == null) {
                        removePlayer(playerId);
                        continue;
                    }

                    ActiveMenu am = playerMenu.get(playerId);
                    Location startLoc = am.getLocation();
                    Location playerLoc = player.getLocation();
                    
                    if(!playerLoc.getWorld().getUID().equals(startLoc.getWorld().getUID())) {
                        removePlayer(playerId);
                        continue;
                    }

                    if(playerLoc.distance(startLoc) > am.getLeaveRadius()) {
                        player.sendMessage(am.getLeaveMessage());
                        removePlayer(playerId);
                    }
                }
            }
        }.runTaskTimer(this, 5, 5);
    }
    
    private void removePlayer(UUID playerId) {
        playerMenu.remove(playerId);
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("omenu")) {
            if(args.length < 2) {
                sender.sendMessage("§cUsage: /omenu <player> <command|leaveradius|leavemessage|command1|command2|...");
                return true;
            }

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

            if(cstr.equals("cancel")) {
                removePlayer(player.getUniqueId());
                return true;
            }

            String[] cstrtk = cstr.split("\\\\s");

            String text = ChatColor.translateAlternateColorCodes('&', cstrtk[0]);

            double leaveRadius = Double.parseDouble(cstrtk[1]);
            String leaveMessage = ChatColor.translateAlternateColorCodes('&', cstrtk[2]);
            
            String[] texttk = text.split("\\\\r");
            for(int i = 0; i < texttk.length; i++) {
                player.sendMessage(texttk[i]);
            }
            
            String[] options = new String[cstrtk.length - 3];
            for(int i = 0; i < options.length; i++) {
                options[i] = cstrtk[i + 3];
            }

            ActiveMenu am = new ActiveMenu(options, player.getLocation(), leaveMessage, leaveRadius);
            playerMenu.put(player.getUniqueId(), am);
            
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
                ActiveMenu am = playerMenu.get(playerId);
                if(am.hasOption(idx)) {
                    playerMenu.remove(playerId);
                    Server server = Bukkit.getServer();
                    server.dispatchCommand(server.getConsoleSender(), am.getOptionCommand(idx));
                }
                else {
                    event.getPlayer().sendMessage("§cInvalid option.");
                }
            }
            else {
                event.getPlayer().sendMessage("§cInvalid option. Are you in a conversation right now?");
            }
        }
        
    }
    
}
