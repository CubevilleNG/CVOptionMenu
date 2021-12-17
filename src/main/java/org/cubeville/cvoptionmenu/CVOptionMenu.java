package org.cubeville.cvoptionmenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        public ActiveMenu(Location location) {
            messages = new ArrayList<>();
            commands = new ArrayList<>();
            this.location = location;
            leaveMessage = null;
            leaveRadius = 7;
            header = null;
            footer = null;
        }

        public void addCommand(String command, String message) {
            commands.add(command);
            messages.add(message);
        }

        public void setLeaveMessage(String message) {
            this.leaveMessage = message;
        }

        public void setLeaveRadius(String radius) {
            this.leaveRadius = Double.parseDouble(radius);
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public void setFooter(String footer) {
            this.footer = footer;
        }
        
        public List<String> getMessages() { return messages; }
        public String getOptionCommand(int Nr) { return commands.get(Nr - 1); }
        public boolean hasOption(int Nr) { return Nr >= 1 && Nr <= commands.size(); }
        public Location getLocation() { return location; }
        public String getLeaveMessage() { return leaveMessage; }
        public double getLeaveRadius() { return leaveRadius; }
        public String getHeader() { return header; }
        public String getFooter() { return footer; }
        
        private Location location;
        private List<String> messages;
        private List<String> commands;
        String header;
        String footer;
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
                        if(am.getLeaveMessage() != null) player.sendMessage(am.getLeaveMessage());
                        removePlayer(playerId);
                    }
                }
            }
        }.runTaskTimer(this, 5, 5);
    }
    
    private void removePlayer(UUID playerId) {
        playerMenu.remove(playerId);
    }

    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage("§cUsage: /omenu <player> <clear|command|leaveradius|leavemessage|show> <parameter>");
    }

    private ActiveMenu getMenu(Player player) {
        UUID pid = player.getUniqueId();
        if(!playerMenu.containsKey(pid)) {
            ActiveMenu menu = new ActiveMenu(player.getLocation());
            playerMenu.put(pid, menu);
            return menu;
        }
        else {
            return playerMenu.get(pid);
        }
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("omenu")) {
            if(args.length < 2) {
                sendUsageMessage(sender);
                return true;
            }

            String playerName = args[0];
            Player player = Bukkit.getServer().getPlayer(playerName);
            if(player == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            String par = args[1];

            if(par.equals("clear")) {
                removePlayer(player.getUniqueId());
                return true;
            }

            if(par.equals("show")) {
                ActiveMenu menu = playerMenu.get(player.getUniqueId());
                if(menu == null) {
                    sender.sendMessage("§cNo menu available.");
                    return true;
                }
                if(menu.getMessages().size() == 0) {
                    sender.sendMessage("§cMenu has no options.");
                    return true;
                }
                if(menu.getHeader() != null) player.sendMessage(menu.getHeader());
                for(String message: menu.getMessages()) {
                    player.sendMessage(message);
                }
                if(menu.getFooter() != null) player.sendMessage(menu.getFooter());
                return true;
            }
            
            if(args.length < 3) {
                sendUsageMessage(sender);
                return true;
            }
            
            String pdata = "";
            for(int i = 2; i < args.length; i++) {
                if(i > 2) pdata += " ";
                pdata += args[i];
            }

            if(par.equals("command")) {
                int delim = pdata.indexOf("/");
                if(delim == -1) {
                    sendUsageMessage(sender);
                    return true;
                }
                String cmd = pdata.substring(0, delim);
                String message = pdata.substring(delim + 1);
                getMenu(player).addCommand(cmd, ChatColor.translateAlternateColorCodes('&', message));
            }

            else if(par.equals("leavemessage")) {
                getMenu(player).setLeaveMessage(ChatColor.translateAlternateColorCodes('&', pdata));
            }

            else if(par.equals("leaveradius")) {
                getMenu(player).setLeaveRadius(pdata);
            }

            else if(par.equals("header")) {
                getMenu(player).setHeader(ChatColor.translateAlternateColorCodes('&', pdata));
            }

            else if(par.equals("footer")) {
                getMenu(player).setFooter(ChatColor.translateAlternateColorCodes('&', pdata));
            }
            
            else {
                sendUsageMessage(sender);
            }

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

	int nr = 0;

	if(event.getMessage().length() == 1) {
	    int idx = "123456789".indexOf(event.getMessage());
	    if(idx >= 0) {
		nr = idx + 1;
	    }
	}

	else if(event.getMessage().length() == 2 && event.getMessage().charAt(0) == '1') {
	    int idx = "0123456789".indexOf(event.getMessage().charAt(1));
	    if(idx >= 0) {
		nr = 10 + idx; 
	    }
	}


        if(nr > 0) {
            event.setCancelled(true);
            UUID playerId = event.getPlayer().getUniqueId();
            if(playerMenu.containsKey(playerId)) {
                ActiveMenu am = playerMenu.get(playerId);
                if(am.hasOption(nr)) {
                    playerMenu.remove(playerId);
                    Server server = Bukkit.getServer();
                    server.dispatchCommand(server.getConsoleSender(), am.getOptionCommand(nr));
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
