package com.thevoxelbox.voxelsniper.command.executor;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import com.thevoxelbox.voxelsniper.VoxelSniperPlugin;
import com.thevoxelbox.voxelsniper.brush.property.BrushPattern;
import com.thevoxelbox.voxelsniper.command.CommandExecutor;
import com.thevoxelbox.voxelsniper.command.TabCompleter;
import com.thevoxelbox.voxelsniper.config.VoxelSniperConfig;
import com.thevoxelbox.voxelsniper.sniper.Sniper;
import com.thevoxelbox.voxelsniper.sniper.SniperRegistry;
import com.thevoxelbox.voxelsniper.sniper.toolkit.BlockTracer;
import com.thevoxelbox.voxelsniper.sniper.toolkit.Toolkit;
import com.thevoxelbox.voxelsniper.sniper.toolkit.ToolkitProperties;
import com.thevoxelbox.voxelsniper.util.message.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VoxelExecutor implements CommandExecutor, TabCompleter {

    private final VoxelSniperPlugin plugin;

    public VoxelExecutor(VoxelSniperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void executeCommand(CommandSender sender, String[] arguments) {
        SniperRegistry sniperRegistry = this.plugin.getSniperRegistry();
        Player player = (Player) sender;
        Sniper sniper = sniperRegistry.registerAndGetSniper(player);
        if (sniper == null) {
            return;
        }
        Toolkit toolkit = sniper.getCurrentToolkit();
        if (toolkit == null) {
            return;
        }
        ToolkitProperties toolkitProperties = toolkit.getProperties();
        if (toolkitProperties == null) {
            return;
        }
        Messenger messenger = new Messenger(plugin, sender);
        VoxelSniperConfig config = this.plugin.getVoxelSniperConfig();
        List<String> liteSniperRestrictedPatterns = config.getLitesniperRestrictedMaterials();
        if (arguments.length == 0) {
            BlockTracer blockTracer = toolkitProperties.createBlockTracer(player);
            BlockVector3 targetBlock = blockTracer.getTargetBlock();
            if (targetBlock != null) {
                BlockType targetBlockType = BukkitAdapter.asBlockType(
                        player.getWorld().getBlockAt(
                                targetBlock.getX(),
                                targetBlock.getY(),
                                targetBlock.getZ()
                        ).getType());

                if (targetBlockType == null) {
                    sniper.print(Caption.of("voxelsniper.command.invalid-block"));
                    return;
                }
                if (!sender.hasPermission("voxelsniper.ignorelimitations") && liteSniperRestrictedPatterns.contains(
                        targetBlockType.getResource())) {
                    sniper.print(Caption.of("voxelsniper.command.not-allowed", targetBlockType.getId()));
                    return;
                }

                toolkitProperties.setPattern(new BrushPattern(targetBlockType));
                messenger.sendPatternMessage(toolkitProperties.getPattern());
            }
        } else {
            BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
            ParserContext parserContext = new ParserContext();
            parserContext.setSession(wePlayer.getSession());
            parserContext.setWorld(wePlayer.getWorld());
            parserContext.setActor(wePlayer);
            parserContext.setRestricted(false);
            try {
                String argument = arguments[0].toLowerCase(Locale.ROOT);
                Pattern pattern = plugin.getPatternParser().parseFromInput(argument, parserContext);

                if (!sender.hasPermission("voxelsniper.ignorelimitations") && liteSniperRestrictedPatterns.contains(argument)) {
                    sniper.print(Caption.of("voxelsniper.command.not-allowed", argument));
                    return;
                }

                toolkitProperties.setPattern(new BrushPattern(pattern, argument));
                messenger.sendPatternMessage(toolkitProperties.getPattern());
            } catch (InputParseException e) {
                sniper.print(Caption.of("voxelsniper.command.voxel-executor.invalid-pattern", arguments[0]));
            }
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] arguments) {
        if (arguments.length == 1) {
            return plugin.getPatternParser().getSuggestions(arguments[0]);
        }
        return Collections.emptyList();
    }

}
