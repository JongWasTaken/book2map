/*
Parts of this file (mainly the rendering part) were taken from image2map (https://github.com/Patbox/Image2Map/blob/1.20.2/src/main/java/space/essem/image2map/renderer/MapRenderer.java)
 */

package dev.smto.book2map;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class Map {
    private static final double[] shadeCoeffs = { 0.71, 0.86, 1.0, 0.53 };

    public static CanvasImage render(BufferedImage image, Boolean dither, int width, int height) {
        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage resized = Map.convertToBufferedImage(resizedImage);
        int[][] pixels = Map.convertPixelArray(resized);

        var state = new CanvasImage(width, height);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (dither) {
                    state.set(i, j, Map.floydDither(pixels, i, j, pixels[j][i]));
                } else {
                    state.set(i, j, CanvasUtils.findClosestColorARGB(pixels[j][i]));
                }
            }
        }

        return state;
    }

    public static List<ItemStack> toVanillaItems(CanvasImage image, ServerLevel world, String url) {
        var xSections = Mth.ceil(image.getWidth() / 128.0d);
        var ySections = Mth.ceil(image.getHeight() / 128.0d);

        var xDelta = (xSections * 128 - image.getWidth()) / 2;
        var yDelta = (ySections * 128 - image.getHeight()) / 2;

        var items = new ArrayList<ItemStack>();

        for (int ys = 0; ys < ySections; ys++) {
            for (int xs = 0; xs < xSections; xs++) {
                var id = world.getFreeMapId();
                var state = MapItemSavedData.createFresh(
                        0, 0, (byte) 0,
                        false, false,
                        ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("image2map", "generated"))
                );

                for (int xl = 0; xl < 128; xl++) {
                    for (int yl = 0; yl < 128; yl++) {
                        var x = xl + xs * 128 - xDelta;
                        var y = yl + ys * 128 - yDelta;

                        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
                            state.colors[xl + yl * 128] = image.getRaw(x, y);
                        }
                    }
                }
                world.setMapData(id, state);

                var stack = new ItemStack(Items.FILLED_MAP);
                stack.set(DataComponents.MAP_ID, id);
                //stack.getOrCreateNbt().putInt("map", id);
                CompoundTag n = new CompoundTag();
                n.putInt("image2map:x", xs);
                n.putInt("image2map:y", ys);
                n.putInt("image2map:width", xSections);
                n.putInt("image2map:height", ySections);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(n));
                items.add(stack);
            }
        }

        return items;
    }

    private static int mapColorToRGBColor(CanvasColor color) {
        var mcColor = color.getRgbColor();
        double[] mcColorVec = { (double) ARGB.red(mcColor), (double) ARGB.green(mcColor), (double) ARGB.blue(mcColor) };
        double coeff = Map.shadeCoeffs[color.getColor().id & 3];
        return ARGB.color(0, (int) (mcColorVec[0] * coeff), (int) (mcColorVec[1] * coeff), (int) (mcColorVec[2] * coeff));
    }

    private static CanvasColor floydDither(int[][] pixels, int x, int y, int imageColor) {
        var closestColor = CanvasUtils.findClosestColorARGB(imageColor);
        var palletedColor = Map.mapColorToRGBColor(closestColor);

        var errorR = ARGB.red(imageColor) - ARGB.red(palletedColor);
        var errorG = ARGB.green(imageColor) - ARGB.green(palletedColor);
        var errorB = ARGB.blue(imageColor) - ARGB.blue(palletedColor);
        if (pixels[0].length > x + 1) {
            pixels[y][x + 1] = Map.applyError(pixels[y][x + 1], errorR, errorG, errorB, 7.0 / 16.0);
        }
        if (pixels.length > y + 1) {
            if (x > 0) {
                pixels[y + 1][x - 1] = Map.applyError(pixels[y + 1][x - 1], errorR, errorG, errorB, 3.0 / 16.0);
            }
            pixels[y + 1][x] = Map.applyError(pixels[y + 1][x], errorR, errorG, errorB, 5.0 / 16.0);
            if (pixels[0].length > x + 1) {
                pixels[y + 1][x + 1] = Map.applyError(pixels[y + 1][x + 1], errorR, errorG, errorB, 1.0 / 16.0);
            }
        }

        return closestColor;
    }

    private static int applyError(int pixelColor, int errorR, int errorG, int errorB, double quantConst) {
        int pR = Map.clamp( ARGB.red(pixelColor) + (int) ((double) errorR * quantConst), 0, 255);
        int pG = Map.clamp(ARGB.green(pixelColor) + (int) ((double) errorG * quantConst), 0, 255);
        int pB = Map.clamp(ARGB.blue(pixelColor) + (int) ((double) errorB * quantConst), 0, 255);
        return ARGB.color(ARGB.alpha(pixelColor), pR, pG, pB);
    }

    private static int clamp(int i, int min, int max) {
        if (min > max)
            throw new IllegalArgumentException("max value cannot be less than min value");
        if (i < min)
            return min;
        if (i > max)
            return max;
        return i;
    }

    private static int[][] convertPixelArray(BufferedImage image) {

        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();

        int[][] result = new int[height][width];
        final int pixelLength = 4;
        for (int pixel = 0, row = 0, col = 0; pixel + 3 < pixels.length; pixel += pixelLength) {
            int argb = 0;
            argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
            argb += ((int) pixels[pixel + 1] & 0xff); // blue
            argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
            argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
            result[row][col] = argb;
            col++;
            if (col == width) {
                col = 0;
                row++;
            }
        }

        return result;
    }

    public static BufferedImage convertToBufferedImage(Image image) {
        BufferedImage newImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    private static BufferedImage compositeImage(ServerPlayer player, CompositeEffects.CanvasData d, List<EffectDataPair> effects) {
        BufferedImage newImage = new BufferedImage(d.width(), d.height(),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = newImage.createGraphics();

        // init
        g.setBackground(Color.BLACK);
        g.setColor(Color.BLACK);
        g.fillRect(0,0, d.width(), d.height());

        // user effects
        String r = "";
        for (int i = 0; i < effects.size(); i++) {
            r = effects.get(i).effect().apply(g, d, effects.get(i).data());
            if (!r.isEmpty()) {
                player.sendSystemMessage(Component.literal(TextHelper.RED +r), false);
            }
        }
        g.dispose();
        return newImage;
    }

    private record EffectDataPair(CompositeEffects.CompositeEffect effect, List<String> data) {}

    public static void createByCommand(ServerPlayer player) {
        var offhandStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (offhandStack.isEmpty()) {
            player.sendSystemMessage(Component.nullToEmpty("§6You need to hold a book to use this command!"), false);
            return;
        }
        if(offhandStack.getItem().equals(Items.WRITABLE_BOOK) || offhandStack.getItem().equals(Items.WRITTEN_BOOK)) {
            var x = offhandStack.getComponents().get(DataComponents.WRITABLE_BOOK_CONTENT).pages();
            if(x.isEmpty()) {
                player.sendSystemMessage(Component.literal("§6Book is empty!"), false);
                return;
            }

            List<String> pages = new ArrayList<>();
            x.forEach((pair -> {
                pages.add(TextHelper.convertTextCompound(pair.raw()).replace("@@","§"));
            }));

            ArrayList<EffectDataPair> effects = new ArrayList<EffectDataPair>();

            // READ DATA FROM BOOK HERE
            // defaults
            String font = Fonts.LIST.getFirst().getFontName();
            for (Font xfont : Fonts.LIST) {
                if (xfont.getFontName().equals("Minecraft")) {
                    font = xfont.getFontName();
                    break;
                }
            }
            int width = 256;
            int height = 256;
            int lineSize = 20;
            int leftOffset = 8;
            int topOffset = 10;
            boolean dither = false;
            boolean aa = true;
            Color color = Color.WHITE;
            effects.add(new EffectDataPair(CompositeEffects.BACKGROUND_RANDOM, new ArrayList<>(List.of("brown"))));
            effects.add(new EffectDataPair(CompositeEffects.FRAME, new ArrayList<>(List.of("black"))));
            boolean presetSettings = true;

            // check for custom settings
            var t2 = String.join("\n", pages).split("book2map");
            if (t2.length == 1) {
                t2 = String.join("\n", pages).split("b2m");
            }
            String settingsPage = "";
            if (t2.length > 1) {
                settingsPage = "book2map\n" + t2[1].trim();
                pages.clear();
                pages.add(t2[0].trim());
            }
            var settings = new ArrayList<>(List.of(settingsPage.split("\n")));
            if (settings.getFirst().trim().equals("book2map")) {
                player.sendSystemMessage(Component.literal("§6Using custom settings from book!"), false);
                settings.removeFirst();
                for (String s : settings) {
                    var line = s.trim().split(":");
                    if (line[0].startsWith("!") || line[0].startsWith("#")) {
                        continue;
                    }
                    if (line.length == 2) {
                        if (line[0].equals("font") || line[0].equals("font-name") || line[0].equals("fontname") || line[0].equals("f")) {
                            font = line[1];
                            continue;
                        }
                        if (line[0].equals("size") || line[0].equals("font-size") || line[0].equals("fontsize") || line[0].equals("s")) {
                            try {
                                lineSize = Integer.parseInt(line[1]);
                            } catch (Exception ignored) {}
                            continue;
                        }
                        if (line[0].equals("top") || line[0].equals("top-offset") || line[0].equals("topoffset") || line[0].equals("t")) {
                            try {
                                topOffset = Integer.parseInt(line[1]);
                            } catch (Exception ignored) {}
                            continue;
                        }
                        if (line[0].equals("dither") || line[0].equals("d")) {
                            if (line[1].trim().equals("true") || line[1].trim().equals("yes") || line[1].trim().equals("1")) {
                                dither = true;
                            }
                            else dither = false;
                            continue;
                        }
                        if (line[0].equals("aa") || line[0].equals("a") || line[0].equals("anti-aliasing") || line[0].equals("antialiasing")) {
                            if (line[1].trim().equals("false") || line[1].trim().equals("off") || line[1].trim().equals("0")) {
                                aa = false;
                            }
                            else aa = true;
                            continue;
                        }
                        if (line[0].equals("width") || line[0].equals("w")) {
                            try {
                                width = Integer.parseInt(line[1]) * 128;
                            } catch (Exception ignored) {}
                            continue;
                        }
                        if (line[0].equals("height") || line[0].equals("h")) {
                            try {
                                height = Integer.parseInt(line[1]) * 128;
                            } catch (Exception ignored) {}
                            continue;
                        }
                        if (line[0].equals("left") || line[0].equals("left-offset") || line[0].equals("leftoffset") || line[0].equals("l")) {
                            try {
                                leftOffset = Integer.parseInt(line[1]);
                            } catch (Exception ignored) {}
                            continue;
                        }
                        if (line[0].equals("color") || line[0].equals("textcolor") || line[0].equals("text-color") || line[0].equals("font-color") || line[0].equals("fontcolor") || line[0].equals("c")) {
                            try {
                                color = Colors.fromString(line[1]);
                            } catch (Exception ignored) {}
                            continue;
                        }
                        if (line[0].equals("effect") || line[0].equals("e")) {
                            if (presetSettings) {
                                effects.clear();
                                presetSettings = false;
                            }
                            try {
                                if(line[1].trim().contains(",")) {
                                    String effect = line[1].trim().split(",")[0];
                                    String arguments = line[1].trim().replace(effect, "");
                                    arguments = arguments.substring(1);
                                    effects.add(new EffectDataPair(
                                            CompositeEffects.getByIdentifier(effect),
                                            new ArrayList<>(List.of(arguments.split(",")))
                                    ));
                                }
                                else {
                                    effects.add(new EffectDataPair(
                                            CompositeEffects.getByIdentifier(line[1]),
                                            new ArrayList<>()
                                    ));
                                }
                            } catch (Exception ignored) {
                                player.sendSystemMessage(Component.literal("§cInvalid effect settings: " + line[1].trim()), false);
                            }
                            continue;
                        }
                    }
                }
            }


            int finalLineSize = lineSize;
            int finalLeftOffset = leftOffset;
            int finalTopOffset = topOffset;
            Font finalFont = new Font(font, Font.PLAIN, finalLineSize);
            Color finalColor = color;
            int finalWidth = width;
            int finalHeight = height;
            boolean finalDither = dither;
            boolean finalAa = aa;

            var bookEffect = new CompositeEffects.CompositeEffect() {
                public String getIdentifier() {return "book-content";}

                public String getDescription() {return "book-content";}
                private int currentLine = 0;
                public String apply(Graphics2D g, CompositeEffects.CanvasData d, List<String> unused) {
                    g.setColor(Color.WHITE);
                    boolean nextIsTag = false;
                    Color currentColor;
                    int fontSizeModifier = 0;
                    int currentFontType = Font.PLAIN;
                    Font currentFont = finalFont.deriveFont(Font.PLAIN, finalLineSize);
                    int maxCharacterHeight = g.getFontMetrics(finalFont.deriveFont(Font.BOLD, finalLineSize)).getHeight();
                    int currentXPosition = 0;
                    int currentYPosition = finalTopOffset;
                    master: for (String page : pages) {
                        for (String line : page.split("\n")) {
                            currentColor = finalColor;
                            this.currentLine++;
                            currentXPosition = finalLeftOffset;
                            if (this.currentLine * finalLineSize > d.height()) {
                                break master;
                            }
                            if (line.startsWith("^^")) {
                                line = line.substring(2);
                                var values = line.substring(0, 2).toCharArray();
                                int val1 = -1;
                                int val2 = -1;
                                try {
                                    val1 = Integer.parseInt(String.valueOf(values[0]));
                                } catch (Exception ignored) {}
                                try {
                                    val2 = Integer.parseInt(String.valueOf(values[1]));
                                } catch (Exception ignored) {}
                                if (val1 != -1 && val2 != -1) {
                                    fontSizeModifier = Integer.parseInt(String.valueOf(val1) + String.valueOf(val2));
                                    line = line.substring(2);
                                }
                                else if (val1 != -1){
                                    line = line.substring(1);
                                    fontSizeModifier = val1;
                                }
                            } else fontSizeModifier = 0;

                            currentFont = finalFont.deriveFont(currentFontType, finalLineSize + fontSizeModifier);
                            maxCharacterHeight = g.getFontMetrics(currentFont.deriveFont(Font.BOLD)).getHeight();
                            currentYPosition = currentYPosition + maxCharacterHeight;
                            for (char c : line.toCharArray()) {
                                //Book2Map.Logger.warn("currentColor: " + currentColor);
                                //Book2Map.Logger.warn("currentFontType: " + currentFontType);
                                //Book2Map.Logger.warn("Current char: " + c);

                                if (!nextIsTag) {
                                    if (c == '§') {
                                        nextIsTag = true;
                                    }
                                    else {
                                        currentFont = finalFont.deriveFont(currentFontType, finalLineSize + fontSizeModifier);
                                        g.setColor(currentColor);
                                        g.setFont(currentFont);
                                        if (finalAa) {
                                            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                                        } else g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                                        g.drawString(String.valueOf(c), currentXPosition, currentYPosition);
                                        currentXPosition = currentXPosition + g.getFontMetrics(currentFont).charWidth(c);
                                    }
                                } else {
                                    var temp = ChatFormatting.getByCode(c);
                                    if (temp != null) {
                                        if (temp.isColor()) {
                                            // 256*256*red+256*green+blue
                                            currentColor = new Color((temp.getColor() / 256 / 256) % 256, (temp.getColor() / 256) % 256, temp.getColor() % 256);
                                        }
                                        else {
                                            //Book2Map.Logger.warn("TAG IS NOT COLOR: " + temp.getName());
                                            if(temp.getChar() == 'l') {
                                                if (currentFontType == 0) {
                                                    currentFontType = 1;
                                                }
                                                if (currentFontType == 2) {
                                                    currentFontType = 3;
                                                }
                                            }
                                            else if (temp.getChar() == 'o') {
                                                if (currentFontType == 0) {
                                                    currentFontType = 2;
                                                }
                                                if (currentFontType == 1) {
                                                    currentFontType = 3;
                                                }
                                            }
                                            else if (temp.getChar() == 'r') {
                                                currentColor = finalColor;
                                                currentFontType = 0;
                                            }
                                        }
                                    }
                                    nextIsTag = false;
                                }
                            }
                        }
                        //currentLine++;
                    }
                    return "";
                }
            };
            AtomicBoolean replaced = new AtomicBoolean(false);
            effects.replaceAll(e -> {
                if (e.effect.getIdentifier().equals("book-content")) {
                    replaced.set(true);
                    return new EffectDataPair(
                            bookEffect,
                            new ArrayList<>()
                    );
                }
                return e;
            });
            if (!replaced.get()) {
                effects.add(new EffectDataPair(
                        bookEffect,
                        new ArrayList<>()
                ));
            }

            player.sendSystemMessage(Component.literal("§6Font: §r" + font), false);
            player.sendSystemMessage(Component.literal("§6Font size: §r" + lineSize), false);
            player.sendSystemMessage(Component.literal("§6Font color: §rR" + color.getRed() + " G" + color.getGreen() + " B" + color.getBlue()), false);
            player.sendSystemMessage(Component.literal("§6Width in blocks: §r" + (width/128)), false);
            player.sendSystemMessage(Component.literal("§6Height in blocks: §r" + (width/128)), false);
            player.sendSystemMessage(Component.literal("§6Left side offset: §r" + leftOffset), false);
            player.sendSystemMessage(Component.literal("§6Top side offset: §r" + leftOffset), false);
            player.sendSystemMessage(Component.literal("§6Use dithering: §r" + dither), false);
            player.sendSystemMessage(Component.literal("§6Procedure (top to bottom): §r"), false);
            for (EffectDataPair effect : effects) {
                player.sendSystemMessage(Component.literal(" -> " + effect.effect().getIdentifier() + ", " + String.join(",", effect.data())), false);
            }
            player.sendSystemMessage(Component.literal("§6Generating..."), false);

            BufferedImage m = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_4BYTE_ABGR);
            try {
                m = Map.compositeImage(player, new CompositeEffects.CanvasData(finalWidth, finalHeight), effects);
            } catch (Exception e) {
                Book2Map.LOGGER.warn("Map.compositeImage() failed: " + e.toString());
                player.sendSystemMessage(Component.literal("§cFailed to generate map! Check your settings!"), false);
                return;
            }

            BufferedImage finalM = m;
            CompletableFuture.supplyAsync(() -> Map.render(finalM, finalDither, finalWidth, finalHeight)).thenAcceptAsync(mapImage -> {
                var items = Map.toVanillaItems(mapImage, player.level(), "");
                Map.giveToPlayer(player, items, "Generated from book", finalWidth, finalHeight);
                player.sendSystemMessage(Component.literal("§6Done!"), false);
            }, player.level().getServer());
        }
        else player.sendSystemMessage(Component.nullToEmpty("§6You need to hold a book to use this command!"), false);
        return;
    }

    public static void giveToPlayer(Player player, List<ItemStack> items, String loreText, int width, int height) {
        if (items.size() == 1) {
            player.addItem(items.getFirst());
        } else {
            var bundle = new ItemStack(Items.BUNDLE);
            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
            bundle.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(loreText))));
            bundle.set(DataComponents.CUSTOM_NAME, Component.literal("Maps").withStyle(ChatFormatting.GOLD));
            CompoundTag n = new CompoundTag();
            n.putBoolean("image2map:quick_place", true);
            n.putInt("image2map:width", Mth.ceil(width / 128.0d));
            n.putInt("image2map:height", Mth.ceil(height / 128.0d));
            bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(n));
            player.addItem(bundle);
        }
    }
}


