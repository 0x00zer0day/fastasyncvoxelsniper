package com.thevoxelbox.voxelsniper.brush.type.performer.splatter;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.worldedit.math.BlockVector3;
import com.thevoxelbox.voxelsniper.brush.type.performer.AbstractPerformerBrush;
import com.thevoxelbox.voxelsniper.sniper.snipe.Snipe;
import com.thevoxelbox.voxelsniper.sniper.snipe.message.SnipeMessenger;
import com.thevoxelbox.voxelsniper.sniper.toolkit.ToolkitProperties;
import com.thevoxelbox.voxelsniper.util.text.NumericParser;

import java.util.List;
import java.util.stream.Stream;

public class SplatterDiscBrush extends AbstractPerformerBrush {

    @Override
    public void handleCommand(String[] parameters, Snipe snipe) {
        SnipeMessenger messenger = snipe.createMessenger();
        String firstParameter = parameters[0];

        if (firstParameter.equalsIgnoreCase("info")) {
            messenger.sendMessage(Caption.of("voxelsniper.brush.splatter-disc.info"));
        } else {
            if (parameters.length == 2) {
                if (firstParameter.equalsIgnoreCase("s")) {
                    Integer seedPercent = NumericParser.parseInteger(parameters[1]);
                    if (seedPercent != null && seedPercent >= super.seedPercentMin && seedPercent <= super.seedPercentMax) {
                        this.seedPercent = seedPercent;
                        messenger.sendMessage(Caption.of(
                                "voxelsniper.brush.splatter-disc.set-seed-parcent",
                                DECIMAL_FORMAT.format(this.seedPercent / 100)
                        ));
                    } else {
                        messenger.sendMessage(Caption.of("voxelsniper.error.invalid-number-between", parameters[1],
                                this.seedPercentMin, this.seedPercentMax
                        ));
                    }
                } else if (firstParameter.equalsIgnoreCase("g")) {
                    Integer growthPercent = NumericParser.parseInteger(parameters[1]);
                    if (growthPercent != null && growthPercent >= super.growthPercentMin && growthPercent <= super.growthPercentMax) {
                        this.growthPercent = growthPercent;
                        messenger.sendMessage(Caption.of(
                                "voxelsniper.brush.splatter-disc.set-growth-percent",
                                DECIMAL_FORMAT.format(this.growthPercent / 100)
                        ));
                    } else {
                        messenger.sendMessage(Caption.of("voxelsniper.error.invalid-number-between", parameters[1],
                                this.growthPercentMin, this.growthPercentMax
                        ));
                    }
                } else if (firstParameter.equalsIgnoreCase("r")) {
                    Integer splatterRecursions = NumericParser.parseInteger(parameters[1]);
                    if (splatterRecursions != null && splatterRecursions >= super.splatterRecursionsMin
                            && splatterRecursions <= super.splatterRecursionsMax) {
                        this.splatterRecursions = splatterRecursions;
                        messenger.sendMessage(Caption.of(
                                "voxelsniper.brush.splatter-disc.set-splatter-recursions",
                                this.splatterRecursions
                        ));
                    } else {
                        messenger.sendMessage(Caption.of("voxelsniper.error.invalid-number-between", parameters[1],
                                this.splatterRecursionsMin, this.splatterRecursionsMax
                        ));
                    }
                } else {
                    messenger.sendMessage(Caption.of("voxelsniper.error.brush.invalid-parameters"));
                }
            } else {
                messenger.sendMessage(Caption.of("voxelsniper.error.brush.invalid-parameters-length"));
            }
        }
    }

    @Override
    public List<String> handleCompletions(String[] parameters, Snipe snipe) {
        if (parameters.length == 1) {
            String parameter = parameters[0];
            return super.sortCompletions(Stream.of("s", "g", "r"), parameter, 0);
        }
        return super.handleCompletions(parameters, snipe);
    }

    @Override
    public void handleArrowAction(Snipe snipe) {
        BlockVector3 targetBlock = getTargetBlock();
        splatterDisc(snipe, targetBlock);
    }

    @Override
    public void handleGunpowderAction(Snipe snipe) {
        BlockVector3 lastBlock = getLastBlock();
        splatterDisc(snipe, lastBlock);
    }

    private void splatterDisc(Snipe snipe, BlockVector3 targetBlock) {
        ToolkitProperties toolkitProperties = snipe.getToolkitProperties();
        SnipeMessenger messenger = snipe.createMessenger();
        if (this.seedPercent < super.seedPercentMin || this.seedPercent > super.seedPercentMax) {
            this.seedPercent = getIntegerProperty("default-seed-percent", DEFAULT_SEED_PERCENT);
            messenger.sendMessage(Caption.of(
                    "voxelsniper.brush.splatter-disc.set-seed-parcent",
                    DECIMAL_FORMAT.format(this.seedPercent / 100)
            ));
        }
        if (this.growthPercent < super.growthPercentMin || this.growthPercent > super.growthPercentMax) {
            this.growthPercent = getIntegerProperty("default-grow-percent", DEFAULT_GROWTH_PERCENT);
            messenger.sendMessage(Caption.of(
                    "voxelsniper.brush.splatter-disc.set-growth-percent",
                    DECIMAL_FORMAT.format(this.growthPercent / 100)
            ));
        }
        if (this.splatterRecursions < super.splatterRecursionsMin || this.splatterRecursions > super.splatterRecursionsMax) {
            this.splatterRecursions = getIntegerProperty("default-splatter-recursions", DEFAULT_SPLATTER_RECURSIONS);
            messenger.sendMessage(Caption.of(
                    "voxelsniper.brush.splatter-disc.set-splatter-recursions",
                    this.splatterRecursions
            ));
        }
        int brushSize = toolkitProperties.getBrushSize();
        int[][] splat = new int[2 * brushSize + 1][2 * brushSize + 1];
        // Seed the array
        for (int x = 2 * brushSize; x >= 0; x--) {
            for (int y = 2 * brushSize; y >= 0; y--) {
                if (super.generator.nextInt(super.seedPercentMax + 1) <= this.seedPercent) {
                    splat[x][y] = 1;
                }
            }
        }
        // Grow the seeds
        int gref = this.growthPercent;
        int[][] tempSplat = new int[2 * brushSize + 1][2 * brushSize + 1];
        for (int r = 0; r < this.splatterRecursions; r++) {
            this.growthPercent = gref - ((gref / this.splatterRecursions) * r);
            for (int x = 2 * brushSize; x >= 0; x--) {
                for (int y = 2 * brushSize; y >= 0; y--) {
                    tempSplat[x][y] = splat[x][y]; // prime tempsplat
                    int growcheck = 0;
                    if (splat[x][y] == 0) {
                        if (x != 0 && splat[x - 1][y] == 1) {
                            growcheck++;
                        }
                        if (y != 0 && splat[x][y - 1] == 1) {
                            growcheck++;
                        }
                        if (x != 2 * brushSize && splat[x + 1][y] == 1) {
                            growcheck++;
                        }
                        if (y != 2 * brushSize && splat[x][y + 1] == 1) {
                            growcheck++;
                        }
                    }
                    if (growcheck >= 1 && super.generator.nextInt(super.growthPercentMax + 1) <= this.growthPercent) {
                        tempSplat[x][y] = 1; // prevent bleed into splat
                    }
                }
            }
            // integrate tempsplat back into splat at end of iteration
            for (int x = 2 * brushSize; x >= 0; x--) {
                if (2 * brushSize + 1 >= 0) {
                    System.arraycopy(tempSplat[x], 0, splat[x], 0, 2 * brushSize + 1);
                }
            }
        }
        this.growthPercent = gref;
        // Fill 1x1 holes
        for (int x = 2 * brushSize; x >= 0; x--) {
            for (int y = 2 * brushSize; y >= 0; y--) {
                if (splat[Math.max(x - 1, 0)][y] == 1 && splat[Math.min(x + 1, 2 * brushSize)][y] == 1 && splat[x][Math.max(
                        0,
                        y - 1
                )] == 1 && splat[x][Math.min(2 * brushSize, y + 1)] == 1) {
                    splat[x][y] = 1;
                }
            }
        }
        // Make the changes
        int blockX = targetBlock.getX();
        int blockY = targetBlock.getY();
        int blockZ = targetBlock.getZ();
        double rSquared = Math.pow(brushSize + 1, 2);
        for (int x = 2 * brushSize; x >= 0; x--) {
            double xSquared = Math.pow(x - brushSize - 1, 2);
            for (int y = 2 * brushSize; y >= 0; y--) {
                if (splat[x][y] == 1 && xSquared + Math.pow(y - brushSize - 1, 2) <= rSquared) {
                    this.performer.perform(
                            getEditSession(),
                            blockX - brushSize + x,
                            clampY(blockY),
                            blockZ - brushSize + y,
                            clampY(blockX - brushSize + x, blockY, blockZ - brushSize + y)
                    );
                }
            }
        }
    }

    @Override
    public void sendInfo(Snipe snipe) {
        if (this.seedPercent < super.seedPercentMin || this.seedPercent > super.seedPercentMax) {
            this.seedPercent = getIntegerProperty("default-seed-percent", DEFAULT_SEED_PERCENT);
        }
        if (this.growthPercent < super.growthPercentMin || this.growthPercent > super.growthPercentMax) {
            this.growthPercent = getIntegerProperty("default-grow-percent", DEFAULT_GROWTH_PERCENT);
        }
        if (this.splatterRecursions < super.splatterRecursionsMin || this.splatterRecursions > super.splatterRecursionsMax) {
            this.splatterRecursions = getIntegerProperty("default-splatter-recursions", DEFAULT_SPLATTER_RECURSIONS);
        }
        snipe.createMessageSender()
                .brushNameMessage()
                .brushSizeMessage()
                .message(Caption.of(
                        "voxelsniper.brush.splatter-disc.set-seed-parcent",
                        DECIMAL_FORMAT.format(this.seedPercent / 100)
                ))
                .message(Caption.of(
                        "voxelsniper.brush.splatter-disc.set-growth-percent",
                        DECIMAL_FORMAT.format(this.growthPercent / 100)
                ))
                .message(Caption.of(
                        "voxelsniper.brush.splatter-disc.set-splatter-recursions",
                        this.splatterRecursions
                ))
                .send();
    }

}
