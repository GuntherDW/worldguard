package com.sk89q.worldguard.bukkit.commands;

import com.sk89q.worldguard.bukkit.GlobalConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.commands.CommandHandler.CommandHandlingException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author GuntherDW
 */
public class CommandSLava extends WgCommand {

        @Override
    public boolean handle(CommandSender sender, String senderName,
            String command, String[] args, GlobalConfiguration cfg, WorldGuardPlugin plugin)
            throws CommandHandlingException {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this command");
            return true;
        }

        Player player = (Player) sender;
        plugin.checkPermission(sender, "slava");
        CommandHandler.checkArgs(args, 0, 1);

            if(args.length>0)
            {
                if(args[0].equalsIgnoreCase("true"))
                {
                    cfg.getWorldConfig(player.getWorld().toString()).simulateSpongeLava = true;
                    player.sendMessage(ChatColor.YELLOW + "You have enabled the lava sponge!");
                } else if(args[0].equalsIgnoreCase("false")) {
                    cfg.getWorldConfig(player.getWorld().toString()).simulateSpongeLava = false;
                    player.sendMessage(ChatColor.YELLOW + "You have disabled the lava sponge!");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /slava (true|false)");
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "Lava sponge is " +
                        (cfg.getWorldConfig(player.getWorld().toString()).simulateSpongeLava ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
            }
            return true;
    }
}
