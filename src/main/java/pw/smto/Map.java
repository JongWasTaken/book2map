/*
Parts of this file (mainly the rendering part) were taken from image2map (https://github.com/Patbox/Image2Map/blob/1.20.2/src/main/java/space/essem/image2map/renderer/MapRenderer.java)
 */

package pw.smto;

import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
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
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                var id = world.getNextMapId();
                var state = MapState.of(
                        0, 0, (byte) 0,
                        false, false,
                        RegistryKey.of(RegistryKeys.WORLD, new Identifier("image2map", "generated"))
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

                world.putMapState(FilledMapItem.getMapName(id), state);

                var stack = new ItemStack(Items.FILLED_MAP);
                stack.getOrCreateNbt().putInt("map", id);
                var lore = new NbtList();
                lore.add(NbtString.of(Text.Serializer.toJson(Text.literal(xs + " / " + ys).formatted(Formatting.GRAY))));
                lore.add(NbtString.of(Text.Serializer.toJson(Text.literal(url))));
                stack.getOrCreateNbt().putInt("image2map:x", xs);
                stack.getOrCreateNbt().putInt("image2map:y", ys);
                stack.getOrCreateNbt().putInt("image2map:width", xSections);
                stack.getOrCreateNbt().putInt("image2map:height", ySections);
                stack.getOrCreateSubNbt("display").put("Lore", lore);
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

    private static BufferedImage convertToBufferedImage(Image image) {
        BufferedImage newImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    private static BufferedImage compositeImage(ServerPlayerEntity player, CompositeEffects.CanvasData d, List<EffectDataPair> effects) {
        BufferedImage newImage = new BufferedImage(d.width, d.height,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = newImage.createGraphics();

        // init
        g.setBackground(Color.BLACK);
        g.setColor(Color.BLACK);
        g.fillRect(0,0, d.width, d.height);

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
            var x = offhandStack.getNbt();
            if(!x.contains("pages")) {
                player.sendMessage(Text.literal("Book is empty!"), false);
                return;
            }

            List<String> pages = new ArrayList<>();
            x.getList("pages", NbtElement.STRING_TYPE).copy().forEach((nbtElement -> {
                pages.add(TextHelper.convertTextCompound((NbtString)nbtElement));
            }));

            ArrayList<EffectDataPair> effects = new ArrayList<EffectDataPair>();

            // READ DATA FROM BOOK HERE
            // defaults
            String font = "Monocraft";
            int width = 256;
            int height = 256;
            int lineSize = 20;
            int leftOffset = 8;
            int topOffset = 10;
            boolean dither = false;
            Color color = Color.WHITE;
            effects.add(new EffectDataPair(Map.CompositeEffects.BACKGROUND_RANDOM, new ArrayList<>(List.of("brown"))));
            effects.add(new EffectDataPair(Map.CompositeEffects.FRAME, new ArrayList<>(List.of("black"))));
            boolean presetSettings = true;

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
                        if (line[0].equals("color") || line[0].equals("c")) {
                            try {
                                var temp = Formatting.byName(line[1].toUpperCase(Locale.ROOT));
                                color = new Color((temp.getColorValue() / 256 / 256) % 256, (temp.getColorValue() / 256) % 256, temp.getColorValue() % 256);
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
                                            Map.CompositeEffects.getByIdentifier(effect),
                                            new ArrayList<>(List.of(arguments.split(",")))
                                    ));
                                }
                                else {
                                    effects.add(new EffectDataPair(
                                            Map.CompositeEffects.getByIdentifier(line[1]),
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
            String finalFont = font;
            Color finalColor = color;
            int finalWidth = width;
            int finalHeight = height;
            boolean finalDither = dither;

            var bookEffect = new Map.CompositeEffects.CompositeEffect() {
                public String getIdentifier() {return "book-content";}

                public String getDescription() {return "book-content";}
                private int currentLine = 0;
                public String apply(Graphics2D g, Map.CompositeEffects.CanvasData d, List<String> unused) {
                    g.setColor(Color.WHITE);
                    int charWidth = finalLineSize - (int)Math.floor(finalLineSize / 3);
                    int charCounter = 0;
                    boolean nextIsTag = false;
                    Color currentColor = finalColor;
                    int currentFontType = Font.PLAIN;
                    master: for (String page : pages) {
                        for (String line : page.split("\n")) {
                            currentColor = finalColor;
                            charCounter = 0;
                            currentLine++;
                            if (currentLine * finalLineSize > d.height()) {
                                break master;
                            }
                            for (char c : line.toCharArray()) {
                                //Book2Map.Logger.warn("currentColor: " + currentColor);
                                //Book2Map.Logger.warn("currentFontType: " + currentFontType);
                                //Book2Map.Logger.warn("Current char: " + c);

                                if (!nextIsTag) {
                                    if (c == '§') {
                                        nextIsTag = true;
                                    }
                                    else {
                                        //if (charCounter > 144) {
                                        //    currentLine++;
                                        //    charCounter = 0;
                                        //}

                                        g.setColor(currentColor);
                                        g.setFont(new Font(finalFont, currentFontType, finalLineSize));
                                        g.drawString(String.valueOf(c), finalLeftOffset + (charCounter * charWidth), finalTopOffset + (currentLine * finalLineSize));
                                        charCounter++;
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
            effects.add(new EffectDataPair(
                    bookEffect,
                    new ArrayList<>(List.of("book-text"))
            ));

            player.sendMessage(Text.literal("§6Font: §r" + font), false);
            player.sendMessage(Text.literal("§6Font size: §r" + lineSize), false);
            player.sendMessage(Text.literal("§6Font color: §rR" + color.getRed() + " G" + color.getGreen() + " B" + color.getBlue()), false);
            player.sendMessage(Text.literal("§6Width in blocks: §r" + (width/128)), false);
            player.sendMessage(Text.literal("§6Height in blocks: §r" + (width/128)), false);
            player.sendMessage(Text.literal("§6Left side offset: §r" + leftOffset), false);
            player.sendMessage(Text.literal("§6Top side offset: §r" + leftOffset), false);
            player.sendMessage(Text.literal("§6Use dithering: §r" + dither), false);
            player.sendMessage(Text.literal("§6Procedure (top to bottom): §r"), false);
            for(int l= 0; l < effects.size(); l++) {
                player.sendMessage(Text.literal(" -> " + effects.get(l).effect().getIdentifier() + ", " + String.join(";", effects.get(l).data())), false);
            }
            player.sendMessage(Text.literal("§6Generating..."), false);

            BufferedImage m = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_4BYTE_ABGR);
            try {
                m = Map.compositeImage(player, new Map.CompositeEffects.CanvasData(finalWidth, finalHeight), effects);
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
            var list = new NbtList();

            for (var item : items) {
                list.add(item.writeNbt(new NbtCompound()));
            }
            bundle.getOrCreateNbt().put("Items", list);
            bundle.getOrCreateNbt().putBoolean("image2map:quick_place", true);
            bundle.getOrCreateNbt().putInt("image2map:width", MathHelper.ceil(width / 128d));
            bundle.getOrCreateNbt().putInt("image2map:height", MathHelper.ceil(height / 128d));

            var lore = new NbtList();
            lore.add(NbtString.of(Text.Serializer.toJson(Text.literal(loreText))));
            bundle.getOrCreateSubNbt("display").put("Lore", lore);
            bundle.setCustomName(Text.literal("Maps").formatted(Formatting.GOLD));

            player.giveItemStack(bundle);
        }
    }


    public static class CompositeEffects {
        public interface CompositeEffect {
            public String getIdentifier();
            public String getDescription();
            public String apply(Graphics2D g, CanvasData d, List<String> arguments);
        }

        public static CompositeEffect getByIdentifier(String identifier) {
            for (CompositeEffect effect : effects) {
                if (effect.getIdentifier().equals(identifier)) {
                    return effect;
                }
            }
            return NONE;
        }

        private static Color colorFromString(String color) {
            if (color.isEmpty())
            {
                color = "black";
            }
            var temp = Formatting.byName(color.toUpperCase(Locale.ROOT));
            if (temp == null) { temp = Formatting.BLACK; }
            return new Color((temp.getColorValue() / 256 / 256) % 256, (temp.getColorValue() / 256) % 256, temp.getColorValue() % 256);
        }

        private static Color colorFromFormatting(Formatting color) {
            if (!color.isColor()) { color = Formatting.BLACK; }
            return new Color((color.getColorValue() / 256 / 256) % 256, (color.getColorValue() / 256) % 256, color.getColorValue() % 256);
        }

        public record CanvasData(int width, int height) {}

        public static CompositeEffect BACKGROUND = new CompositeEffect() {
            public String getIdentifier() {
                return "background";
            }
            public String getDescription() {
                return "fills the background with a solid color";
            }
            public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
                var color = arguments.get(0);
                Random random = new Random();
                if (color.isEmpty())
                {
                    color = "black";
                }
                if (color.equals("brown")) {
                    g.setColor(new Color(50,40,40));
                }
                else
                {
                    var temp = Formatting.byName(color.toUpperCase(Locale.ROOT));
                    if (temp == null) { temp = Formatting.BLACK; }
                    g.setColor(new Color((temp.getColorValue() / 256 / 256) % 256, (temp.getColorValue() / 256) % 256, temp.getColorValue() % 256));
                }
                g.fillRect(0,0, d.width, d.height);
                return "";
            }
        };

        public static CompositeEffect BACKGROUND_RANDOM = new CompositeEffect() {
            public String getIdentifier() {
                return "background-random";
            }
            public String getDescription() {
                return "sets the background to a random palette of a given color (or a fully random palette)";
            }
            public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
                var color = arguments.get(0);
                Random random = new Random();
                if (color.isEmpty())
                {
                    for (int i = 0; i <= d.width-2; i++) {
                        for (int k = 0; k <= d.height-2; k++) {
                            g.setColor(new Color(random.nextInt(256),random.nextInt(256),random.nextInt(256)));
                            g.drawLine(i,k,i+1,k+1);
                        }
                    }
                    return "";
                }
                if (color.equals("brown")) {
                    for (int i = 0; i <= d.width-2; i++) {
                        for (int k = 0; k <= d.height-2; k++) {
                            g.setColor(new Color(50+random.nextInt(10),40+random.nextInt(10),40+random.nextInt(10)));
                            g.drawLine(i,k,i+1,k+1);
                        }
                    }
                    return "";
                }
                var temp = Formatting.byName(color.toUpperCase(Locale.ROOT));
                if (temp == null) temp = Formatting.BLACK;
                try {
                    var R = (temp.getColorValue() / 256 / 256) % 256;
                    var G = (temp.getColorValue() / 256) % 256;
                    var B = temp.getColorValue() % 256;
                    for (int i = 0; i <= d.width-2; i++) {
                        for (int k = 0; k <= d.height-2; k++) {
                            int Rn, Gn, Bn;
                            if (R < 127) {
                                Rn = R + random.nextInt(10);
                            } else Rn = R - random.nextInt(10);
                            if (G < 127) {
                                Gn = G + random.nextInt(10);
                            } else Gn = G - random.nextInt(10);
                            if (B < 127) {
                                Bn = B + random.nextInt(10);
                            } else Bn = B - random.nextInt(10);
                            g.setColor(new Color(Rn,Gn,Bn));
                            g.drawLine(i,k,i+1,k+1);
                        }
                    }
                } catch (Exception e) {
                    Book2Map.Logger.warn(e.toString());
                    return "Error while generating random color palette! This is probably a programming issue, so please report it!";
                }
                return "";
            }
        };

        public static CompositeEffect BACKGROUND_TEXTURE = new CompositeEffect() {
            public String getIdentifier() {
                return "background-texture";
            }
            public String getDescription() {
                return "sets the background to a texture image";
            }
            public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
                // check if pack exists
                var texture = arguments.get(0);
                Path baseDir = Path.of(FabricLoader.getInstance().getConfigDir().toString(),"survivalplus", "textures");
                Path targetFile = Path.of(baseDir.toString(), texture + ".png");
                BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR);
                if (Files.exists(baseDir)) {
                    try {
                        image = ImageIO.read(targetFile.toFile());
                    } catch (Exception ignored) {
                        return "Error while loading texture! Please check if the texture file exists and try again.";
                    }
                }
                Image resizedImage = image.getScaledInstance(d.width, d.height, Image.SCALE_DEFAULT);
                BufferedImage resized = convertToBufferedImage(resizedImage);
                g.drawImage(resized, 0, 0, d.width, d.height, null);
                return "";
            }
        };
        public static CompositeEffect FRAME = new CompositeEffect() {
            public String getIdentifier() {
                return "frame";
            }
            public String getDescription() {
                return "creates a 4 pixel border around the image";
            }

            public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
                int thickness = 4;
                g.setColor(colorFromString(arguments.get(0)));
                g.fillRect(0,0, d.width-thickness, thickness); // top left -> top right
                g.fillRect(0,0, thickness, d.height); // top left -> bottom left
                g.fillRect(0,d.height-thickness, d.width,d.height-thickness); // bottom left -> bottom right
                g.fillRect(d.width-thickness,0, d.width-thickness, d.height); // top right -> bottom right
                return "";
            }
        };

        public static CompositeEffect CIRCLE = new CompositeEffect() {
            public String getIdentifier() {
                return "circle";
            }
            public String getDescription() {
                return "places a circle on the image";
            }

            public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
                Color color = Color.YELLOW;
                int x = 0;
                int y = 0;
                int width = d.width();
                int height = d.height();
                boolean hollow = false;
                if (!arguments.isEmpty()) {
                    try {
                        if (arguments.size() == 1) {
                            color = colorFromString(arguments.get(0));
                        }
                        if (arguments.size() == 2) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                        }
                        if (arguments.size() == 3) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                        }
                        if (arguments.size() == 4) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                            width = Integer.parseInt(arguments.get(3));
                        }
                        if (arguments.size() == 5) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                            width = Integer.parseInt(arguments.get(3));
                            height = Integer.parseInt(arguments.get(4));
                        }
                        if (arguments.size() == 6) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                            width = Integer.parseInt(arguments.get(3));
                            height = Integer.parseInt(arguments.get(4));
                            hollow = true;
                        }
                    } catch (Exception ignored) {
                        return "Error while parsing circle arguments! Remember to use this format: <color>,<x>,<y>,<width>,<height>,<hollow?>";
                    }
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
                g2.setColor(color);
                var shape = new Ellipse2D.Double(x,y,width,height);
                g2.draw(shape);
                if (!hollow) {
                    g2.fill(shape);
                }
                return "";
            }
        };

        public static CompositeEffect RECTANGLE = new CompositeEffect() {
            public String getIdentifier() {
                return "rectangle";
            }
            public String getDescription() {
                return "places a rectangle on the image";
            }

            public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
                Color color = Color.YELLOW;
                int x = 0;
                int y = 0;
                int width = d.width();
                int height = d.height();
                boolean hollow = false;
                if (!arguments.isEmpty()) {
                    try {
                        if (arguments.size() == 1) {
                            color = colorFromString(arguments.get(0));
                        }
                        if (arguments.size() == 2) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                        }
                        if (arguments.size() == 3) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                        }
                        if (arguments.size() == 4) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                            width = Integer.parseInt(arguments.get(3));
                        }
                        if (arguments.size() == 5) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                            width = Integer.parseInt(arguments.get(3));
                            height = Integer.parseInt(arguments.get(4));
                        }
                        if (arguments.size() == 6) {
                            color = colorFromString(arguments.get(0));
                            x = Integer.parseInt(arguments.get(1));
                            y = Integer.parseInt(arguments.get(2));
                            width = Integer.parseInt(arguments.get(3));
                            height = Integer.parseInt(arguments.get(4));
                            hollow = true;
                        }
                    } catch (Exception ignored) {
                        return "Error while parsing rectangle arguments! Remember to use this format: <color>,<x>,<y>,<width>,<height>,<hollow?>";
                    }
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_PURE);
                g2.setColor(color);
                var shape = new Rectangle(x,y,width,height);
                g2.draw(shape);
                if (!hollow) {
                    g2.fill(shape);
                }
                return "";
            }
        };

        public static CompositeEffect LINE = new CompositeEffect() {
            public String getIdentifier() {
                return "line";
            }
            public String getDescription() {
                return "places a line on the image";
            }

            public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
                Color color = Color.YELLOW;
                int x1 = 0;
                int y1 = 0;
                int x2 = 32;
                int y2 = 32;
                if (!arguments.isEmpty()) {
                    try {
                        if (arguments.size() == 1) {
                            color = colorFromString(arguments.get(0));
                        }
                        if (arguments.size() == 2) {
                            color = colorFromString(arguments.get(0));
                            x1 = Integer.parseInt(arguments.get(1));
                        }
                        if (arguments.size() == 3) {
                            color = colorFromString(arguments.get(0));
                            x1 = Integer.parseInt(arguments.get(1));
                            y1 = Integer.parseInt(arguments.get(2));
                        }
                        if (arguments.size() == 4) {
                            color = colorFromString(arguments.get(0));
                            x1 = Integer.parseInt(arguments.get(1));
                            y1 = Integer.parseInt(arguments.get(2));
                            x2 = Integer.parseInt(arguments.get(3));
                        }
                        if (arguments.size() == 5) {
                            color = colorFromString(arguments.get(0));
                            x1 = Integer.parseInt(arguments.get(1));
                            y1 = Integer.parseInt(arguments.get(2));
                            x2 = Integer.parseInt(arguments.get(3));
                            y2 = Integer.parseInt(arguments.get(4));
                        }
                    } catch (Exception ignored) {
                        return "Error while parsing line arguments! Remember to use this format: <color>,<x1>,<y1>,<x2>,<y2>";
                    }
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_PURE);
                g2.setColor(color);
                g2.drawLine(x1, y1, x2, y2);
                return "";
            }
        };

        public static CompositeEffect NONE = new CompositeEffect() {
            public String getIdentifier() {
                return "none";
            }
            public String getDescription() {
                return "placeholder for missing effects";
            }
            public String apply(Graphics2D g, CanvasData d, List<String> arguments) { return "Placeholder effect was used, check your settings!"; }
        };

        public static final CompositeEffects.CompositeEffect[] effects = {
                BACKGROUND,
                BACKGROUND_RANDOM,
                BACKGROUND_TEXTURE,
                FRAME,
                CIRCLE,
                RECTANGLE,
                LINE,
                NONE
        };
    }
}


