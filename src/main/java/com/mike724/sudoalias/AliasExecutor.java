package com.mike724.sudoalias;

// Core bukkit imports
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

/**
 * Responsible for processing and executing the alias group
 * 
 * @author mike724
*/
public class AliasExecutor implements Runnable {

    /**
     * The alias for which to process
    */
    private Alias alias;
    
    /**
     * The raw command
    */
    private String command;
    
    /**
     * The sender details
    */
    private CommandSender sender;
    
    /**
     * The player name
    */
    private String playerName;

    /**
     * Class constructor
     * 
     * @param alias     The alias to process
     * @param command   The raw command string
     * @param sender    Sender details
     * @param playerName playerName
    */
    public AliasExecutor(Alias alias, String command, CommandSender sender, String playerName) {
        this.alias = alias;
        this.command = command;
        this.sender = sender;
        this.playerName = playerName;
    }

    /**
     * Thread Run method
    */
    @Override
    public void run() {
        
    	// Check for permissions first
        if (!this.sender.hasPermission(this.alias.getPermNode())) {
            
            this.sender.sendMessage(ChatColor.RED + Config.errPerm);
            return;
        }
        
        // Get arguments and number of arguments
        String[] args = this.alias.getArgs(this.command);
        int argsAmount = args.length;
        
        // Now process each command
        for (String command : this.alias.getCommandsToRun()) {
            
            // Replace all instances of a local define with the define value
            // This comes first because defines take priority over others
            // if a user makes a "player" define then defines would always
            // get to it before the $player variable, local always comes
            // before global define
            if (this.alias.localDefines != null && 
                    !this.alias.localDefines.isEmpty()) {

                for(int i = 0; i < this.alias.localDefines.size(); i++)
                {   
                    Define tmp = this.alias.localDefines.get(i);

                    if (
                            (tmp.key == null || tmp.key.equalsIgnoreCase("")) ||
                            (tmp.value == null || tmp.value.equalsIgnoreCase(""))
                        )
                    {
                        continue;
                    }

                    command = command.replace(
                                Config.spVarDef.replace("%1", tmp.key),
                                tmp.value
                            );
                }
            }
            
            // Replace all instances of a define with the define value
            // This comes first because defines take priority over others
            // if a user makes a "player" define then defines would always
            // get to it before the $player variable
            if (SudoAlias.getInstance().defines != null && 
                    !SudoAlias.getInstance().defines.isEmpty()) {

                for(int i = 0; i < SudoAlias.getInstance().defines.size(); i++)
                {   
                    Define tmp = SudoAlias.getInstance().defines.get(i);

                    if (
                            (tmp.key == null || tmp.key.equalsIgnoreCase("")) ||
                            (tmp.value == null || tmp.value.equalsIgnoreCase(""))
                        )
                    {
                        continue;
                    }

                    command = command.replace(
                                Config.spVarDef.replace("%1", tmp.key),
                                tmp.value
                            );
                }
            }
            
            // @note $wait:### Signifies a pause in execution
            // check for that pause
            if (command.startsWith(Config.spCmdWait + ":")) {
                
                // Split the pause command from the rest and ensure another
                // argument is provided or silently ignore all-together
                String[] data = command.split(":");
                if (data.length != 2) {
                    continue;
                }
                
                // If a second argument is given parse it as a number, if
                // it fails for any reason then issue a warning and revert
                // to the default
                long time = Config.cmdWaitArgDef;
                
                try {
                    time = Long.parseLong(data[1]);
                } catch (NumberFormatException ex) {
                    SudoAlias.getInstance().getLogger().log(Level.WARNING, Config.errCmdWaitArg, data[1]);
                }
                
                // Now sleep for specified time
                // If sleep fails then crash and dump crash log
                // @note a rather rude way to terminate
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                
                // Replace all instances of $player with the player name
                if (this.playerName != null && !this.playerName.isEmpty()) {
                    command = command.replace(Config.spVarPlayer, this.playerName);
                }
                
                // replace all $# with the argument id
                for (int i = 0; i < argsAmount; i++) {
                    command = command.replace(Config.varArg.replace("%1", Integer.toString(i)), args[i]);
                }
                
                // Get server handle
                Server server = SudoAlias.getInstance().getServer();
                
                // Send to executor if running as CONSOLE or otherwise PLAYER
                if(alias.getRunAs() == AliasRunAs.CONSOLE) {
                    server.getScheduler().scheduleSyncDelayedTask(SudoAlias.getInstance(),
                            new AliasCmdExecutor(server.getConsoleSender(), command));
                    
                } else if(alias.getRunAs() == AliasRunAs.PLAYER) {
                    server.getScheduler().scheduleSyncDelayedTask(SudoAlias.getInstance(),
                            new AliasCmdExecutor(sender, command));
                }
            }
        }
        
        // Now that everything is over issue the success message if there is one
        String successMsg = alias.getSuccessMessage();
        if (!successMsg.isEmpty() && successMsg != null) {
            sender.sendMessage(successMsg);
        }
    }

}
