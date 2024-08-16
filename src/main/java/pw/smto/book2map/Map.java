/*
Parts of this file (mainly the rendering part) were taken from image2map (https://github.com/Patbox/Image2Map/blob/1.20.2/src/main/java/space/essem/image2map/renderer/MapRenderer.java)
 */

package pw.smto.book2map;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Map {
    private static final double shadeCoeffs[] = { 0.71, 0.86, 1.0, 0.53 };

    public static CanvasImage render(BufferedImage image, Boolean dither, int width, int height) {
        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage resized = convertToBufferedImage(resizedImage);
        int[][] pixels = convertPixelArray(resized);

        var state = new CanvasImage(width, height);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (dither) {
                    state.set(i, j, floydDither(pixels, i, j, pixels[j][i]));
                } else {
                    state.set(i, j, CanvasUtils.findClosestColorARGB(pixels[j][i]));
                }
            }
        }

        return state;
    }

    public static List<ItemStack> toVanillaItems(CanvasImage image, ServerWorld world, String url) {
        var xSections = MathHelper.ceil(image.getWidth() / 128d);
        var ySections = MathHelper.ceil(image.getHeight() / 128d);

        var xDelta = (xSections * 128 - image.getWidth()) / 2;
        var yDelta = (ySections * 128 - image.getHeight()) / 2;

        var items = new ArrayList<ItemStack>();

        for (int ys = 0; ys < ySections; ys++) {
            for (int xs = 0; xs < xSections; xs++) {
                var id = world.increaseAndGetMapId();
                var state = MapState.of(
                        0, 0, (byte) 0,
                        false, false,
                        RegistryKey.of(RegistryKeys.WORLD, Identifier.of("image2map", "generated"))
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
                world.putMapState(id, state);

                var stack = new ItemStack(Items.FILLED_MAP);
                stack.set(DataComponentTypes.MAP_ID, id);
                //stack.getOrCreateNbt().putInt("map", id);
                NbtCompound n = new NbtCompound();
                n.putInt("image2map:x", xs);
                n.putInt("image2map:y", ys);
                n.putInt("image2map:width", xSections);
                n.putInt("image2map:height", ySections);
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
                items.add(stack);
            }
        }

        return items;
    }

    private static int mapColorToRGBColor(CanvasColor color) {
        var mcColor = color.getRgbColor();
        double[] mcColorVec = { (double) ColorHelper.Argb.getRed(mcColor), (double) ColorHelper.Argb.getGreen(mcColor), (double) ColorHelper.Argb.getBlue(mcColor) };
        double coeff = shadeCoeffs[color.getColor().id & 3];
        return ColorHelper.Argb.getArgb(0, (int) (mcColorVec[0] * coeff), (int) (mcColorVec[1] * coeff), (int) (mcColorVec[2] * coeff));
    }

    private static CanvasColor floydDither(int[][] pixels, int x, int y, int imageColor) {
        var closestColor = CanvasUtils.findClosestColorARGB(imageColor);
        var palletedColor = mapColorToRGBColor(closestColor);

        var errorR = ColorHelper.Argb.getRed(imageColor) - ColorHelper.Argb.getRed(palletedColor);
        var errorG = ColorHelper.Argb.getGreen(imageColor) - ColorHelper.Argb.getGreen(palletedColor);
        var errorB = ColorHelper.Argb.getBlue(imageColor) - ColorHelper.Argb.getBlue(palletedColor);
        if (pixels[0].length > x + 1) {
            pixels[y][x + 1] = applyError(pixels[y][x + 1], errorR, errorG, errorB, 7.0 / 16.0);
        }
        if (pixels.length > y + 1) {
            if (x > 0) {
                pixels[y + 1][x - 1] = applyError(pixels[y + 1][x - 1], errorR, errorG, errorB, 3.0 / 16.0);
            }
            pixels[y + 1][x] = applyError(pixels[y + 1][x], errorR, errorG, errorB, 5.0 / 16.0);
            if (pixels[0].length > x + 1) {
                pixels[y + 1][x + 1] = applyError(pixels[y + 1][x + 1], errorR, errorG, errorB, 1.0 / 16.0);
            }
        }

        return closestColor;
    }

    private static int applyError(int pixelColor, int errorR, int errorG, int errorB, double quantConst) {
        int pR = clamp( ColorHelper.Argb.getRed(pixelColor) + (int) ((double) errorR * quantConst), 0, 255);
        int pG = clamp(ColorHelper.Argb.getGreen(pixelColor) + (int) ((double) errorG * quantConst), 0, 255);
        int pB = clamp(ColorHelper.Argb.getBlue(pixelColor) + (int) ((double) errorB * quantConst), 0, 255);
        return ColorHelper.Argb.getArgb(ColorHelper.Argb.getAlpha(pixelColor), pR, pG, pB);
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

    private static BufferedImage compositeImage(ServerPlayerEntity player, CompositeEffects.CanvasData d, List<EffectDataPair> effects) {
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
                player.sendMessage(Text.literal(TextHelper.RED +r), false);
            }
        }
        g.dispose();
        return newImage;
    }

    private record EffectDataPair(CompositeEffects.CompositeEffect effect, List<String> data) {}

    public static void createByCommand(ServerPlayerEntity player) {
        var offhandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (offhandStack.isEmpty()) {
            player.sendMessage(Text.of("§6You need to hold a book to use this command!"), false);
            return;
        }
        if(offhandStack.getItem().equals(Items.WRITABLE_BOOK) || offhandStack.getItem().equals(Items.WRITTEN_BOOK)) {
            var x = offhandStack.getComponents().get(DataComponentTypes.WRITABLE_BOOK_CONTENT).pages();
            if(x.isEmpty()) {
                player.sendMessage(Text.literal("§6Book is empty!"), false);
                return;
            }

            List<String> pages = new ArrayList<>();
            x.forEach((pair -> {
                pages.add(TextHelper.convertTextCompound(pair.raw()).replace("@@","§"));
            }));

            ArrayList<EffectDataPair> effects = new ArrayList<EffectDataPair>();

            // READ DATA FROM BOOK HERE
            // defaults
            String font = Fonts.LIST.get(0).getFontName();
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
            if (settings.get(0).trim().equals("book2map")) {
                player.sendMessage(Text.literal("§6Using custom settings from book!"), false);
                settings.remove(0);
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
                                player.sendMessage(Text.literal("§cInvalid effect settings: " + line[1].trim()), false);
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
                            currentLine++;
                            currentXPosition = finalLeftOffset;
                            if (currentLine * finalLineSize > d.height()) {
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
                                    var temp = Formatting.byCode(c);
                                    if (temp != null) {
                                        if (temp.isColor()) {
                                            // 256*256*red+256*green+blue
                                            currentColor = new Color((temp.getColorValue() / 256 / 256) % 256, (temp.getColorValue() / 256) % 256, temp.getColorValue() % 256);
                                        }
                                        else {
                                            //Book2Map.Logger.warn("TAG IS NOT COLOR: " + temp.getName());
                                            if(temp.getCode() == 'l') {
                                                if (currentFontType == 0) {
                                                    currentFontType = 1;
                                                }
                                                if (currentFontType == 2) {
                                                    currentFontType = 3;
                                                }
                                            }
                                            else if (temp.getCode() == 'o') {
                                                if (currentFontType == 0) {
                                                    currentFontType = 2;
                                                }
                                                if (currentFontType == 1) {
                                                    currentFontType = 3;
                                                }
                                            }
                                            else if (temp.getCode() == 'r') {
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

            player.sendMessage(Text.literal("§6Font: §r" + font), false);
            player.sendMessage(Text.literal("§6Font size: §r" + lineSize), false);
            player.sendMessage(Text.literal("§6Font color: §rR" + color.getRed() + " G" + color.getGreen() + " B" + color.getBlue()), false);
            player.sendMessage(Text.literal("§6Width in blocks: §r" + (width/128)), false);
            player.sendMessage(Text.literal("§6Height in blocks: §r" + (width/128)), false);
            player.sendMessage(Text.literal("§6Left side offset: §r" + leftOffset), false);
            player.sendMessage(Text.literal("§6Top side offset: §r" + leftOffset), false);
            player.sendMessage(Text.literal("§6Use dithering: §r" + dither), false);
            player.sendMessage(Text.literal("§6Procedure (top to bottom): §r"), false);
            for (EffectDataPair effect : effects) {
                player.sendMessage(Text.literal(" -> " + effect.effect().getIdentifier() + ", " + String.join(",", effect.data())), false);
            }
            player.sendMessage(Text.literal("§6Generating..."), false);

            BufferedImage m = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_4BYTE_ABGR);
            try {
                m = Map.compositeImage(player, new CompositeEffects.CanvasData(finalWidth, finalHeight), effects);
            } catch (Exception e) {
                Book2Map.Logger.warn("Map.compositeImage() failed: " + e.toString());
                player.sendMessage(Text.literal("§cFailed to generate map! Check your settings!"), false);
                return;
            }

            BufferedImage finalM = m;
            CompletableFuture.supplyAsync(() -> Map.render(finalM, finalDither, finalWidth, finalHeight)).thenAcceptAsync(mapImage -> {
                var items = Map.toVanillaItems(mapImage, player.getServerWorld(), "");
                giveToPlayer(player, items, "Generated from book", finalWidth, finalHeight);
                player.sendMessage(Text.literal("§6Done!"), false);
            }, player.getServer());
        }
        else player.sendMessage(Text.of("§6You need to hold a book to use this command!"), false);
        return;
    }

    public static void giveToPlayer(PlayerEntity player, List<ItemStack> items, String loreText, int width, int height) {
        if (items.size() == 1) {
            player.giveItemStack(items.get(0));
        } else {
            var bundle = new ItemStack(Items.BUNDLE);
            bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(items));
            bundle.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(loreText))));
            bundle.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Maps").formatted(Formatting.GOLD));
            NbtCompound n = new NbtCompound();
            n.putBoolean("image2map:quick_place", true);
            n.putInt("image2map:width", MathHelper.ceil(width / 128d));
            n.putInt("image2map:height", MathHelper.ceil(height / 128d));
            bundle.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
            player.giveItemStack(bundle);
        }
    }
}


