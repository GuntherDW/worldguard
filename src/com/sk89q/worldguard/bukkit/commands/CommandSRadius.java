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
public class CommandSRadius extends WgCommand {
    
    @Override
    public boolean handle(CommandSender sender, String senderName,
            String command, String[] args, GlobalConfiguration cfg, WorldGuardPlugin plugin)
            throws CommandHandlingException {
        plugin.checkPermission(sender, "sradius");
        CommandHandler.checkArgs(args, 0, 1);
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this command");
            return true;
        }

        Player player = (Player) sender;

            Integer rad;
            if(args.length>0)
            {
                try {
                    rad = Integer.parseInt(args[0]);
                    if(rad>cfg.getWorldConfig(player.getWorld().toString()).maxSpongeRadius||rad<1)
                    {
                        rad=cfg.getWorldConfig(player.getWorld().toString()).maxSpongeRadius;
                    }
                    cfg.getWorldConfig(player.getWorld().toString()).spongeRadius = rad-1;
                    player.sendMessage(ChatColor.YELLOW + "Sponge radius has been set to "+rad+"!");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.YELLOW + "Invalid stuff!");
                }

            } else {
                 player.sendMessage(ChatColor.YELLOW + "Current sponge radius is : " +(cfg.getWorldConfig(player.getWorld().toString()).spongeRadius+1));
            }
        return true;
    }
}
