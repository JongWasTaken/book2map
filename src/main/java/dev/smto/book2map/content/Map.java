/*
Parts of this file (mainly the rendering part) were taken from image2map (https://github.com/Patbox/Image2Map/blob/1.20.2/src/main/java/space/essem/image2map/renderer/MapRenderer.java)
 */

package dev.smto.book2map.content;

import dev.smto.book2map.Book2Map;
import dev.smto.book2map.api.CanvasDimensions;
import dev.smto.book2map.api.CompositeEffect;
import dev.smto.book2map.api.ConfiguredEffect;
import eu.pb4.mapcanvas.api.core.CanvasImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

@SuppressWarnings({"MagicConstant", "DataFlowIssue"})
public class Map {
    public static CanvasImage render(BufferedImage image, Boolean dither, int width, int height) {
        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage resized = Map.convertToBufferedImage(resizedImage);
        if (dither) {
            return CanvasImage.fromWithFloydSteinbergDither(resized);
        } else return CanvasImage.from(resized);
    }

    public static List<ItemStack> toVanillaItems(CanvasImage image, ServerLevel world) {
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

    public static BufferedImage convertToBufferedImage(Image image) {
        BufferedImage newImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    private static BufferedImage compositeImage(ServerPlayer player, CanvasDimensions d, List<ConfiguredEffect> effects) {
        BufferedImage newImage = new BufferedImage(d.width(), d.height(),
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = newImage.createGraphics();

        // init
        g.setBackground(Color.BLACK);
        g.setColor(Color.BLACK);
        g.fillRect(0,0, d.width(), d.height());

        // user effects
        String r;
        for (ConfiguredEffect effect : effects) {
            r = effect.effect().apply(g, d, effect.data());
            if (!r.isEmpty()) {
                player.sendSystemMessage(Component.literal(ChatFormatting.RED + r), false);
            }
        }
        g.dispose();
        return newImage;
    }

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
                pages.add(Map.convertTextCompound(pair.raw()).replace("@@","§"));
            }));

            ArrayList<ConfiguredEffect> effects = new ArrayList<>();

            // READ DATA FROM BOOK HERE
            // defaults

            String bundleLore;
            if (!pages.isEmpty()) bundleLore = pages.getFirst().trim().substring(0, 32).replace('\n', ' ') + "...";
            else {
                bundleLore = "(Empty book)";
            }

            String font = Fonts.getAvailableFonts().getFirst().getFontName();
            for (Font xfont : Fonts.getAvailableFonts()) {
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
            effects.add(new ConfiguredEffect(CompositeEffects.BACKGROUND_RANDOM, new ArrayList<>(List.of("brown"))));
            effects.add(new ConfiguredEffect(CompositeEffects.FRAME, new ArrayList<>(List.of("black"))));
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
                        switch (line[0]) {
                            case "font", "font-name", "fontname", "f" -> {
                                font = line[1];
                            }
                            case "size", "font-size", "fontsize", "s" -> {
                                try {
                                    lineSize = Integer.parseInt(line[1]);
                                } catch (Exception ignored) {}
                            }
                            case "top", "top-offset", "topoffset", "t" -> {
                                try {
                                    topOffset = Integer.parseInt(line[1]);
                                } catch (Exception ignored) {}
                            }
                            case "dither", "d" -> {
                                dither = line[1].trim().equals("true") || line[1].trim().equals("yes") || line[1].trim().equals("1");
                            }
                            case "aa", "a", "anti-aliasing", "antialiasing" -> {
                                aa = !line[1].trim().equals("false") && !line[1].trim().equals("off") && !line[1].trim().equals("0");
                            }
                            case "width", "w" -> {
                                try {
                                    width = Integer.parseInt(line[1]) * 128;
                                } catch (Exception ignored) {}
                            }
                            case "height", "h" -> {
                                try {
                                    height = Integer.parseInt(line[1]) * 128;
                                } catch (Exception ignored) {}
                            }
                            case "left", "left-offset", "leftoffset", "l" -> {
                                try {
                                    leftOffset = Integer.parseInt(line[1]);
                                } catch (Exception ignored) {}
                            }
                            case "color", "textcolor", "text-color", "font-color", "fontcolor", "c" -> {
                                try {
                                    color = Colors.fromString(line[1]);
                                } catch (Exception ignored) {}
                            }
                            case "effect", "e" -> {
                                if (presetSettings) {
                                    effects.clear();
                                    presetSettings = false;
                                }
                                try {
                                    if (line[1].trim().contains(",")) {
                                        String effect = line[1].trim().split(",")[0];
                                        String arguments = line[1].trim().replace(effect, "");
                                        arguments = arguments.substring(1);
                                        effects.add(new ConfiguredEffect(
                                                CompositeEffects.get(effect),
                                                new ArrayList<>(List.of(arguments.split(",")))
                                        ));
                                    } else {
                                        effects.add(ConfiguredEffect.unconfigured(CompositeEffects.get(line[1])));
                                    }
                                } catch (Exception ignored) {
                                    player.sendSystemMessage(Component.literal("§cInvalid effect settings: " + line[1].trim()), false);
                                }
                            }
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

            var bookEffect = new CompositeEffect() {
                public String getIdentifier() {return "book-content";}

                public String getDescription() {return "book-content";}
                private int currentLine = 0;
                public String apply(Graphics2D g, CanvasDimensions d, List<String> unused) {
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
                                        //Book2Map.Logger.warn("TAG IS NOT COLOR: " + temp.getName());
                                        switch (temp.toString()) {
                                            case "§l" -> {
                                                if (currentFontType == 0) {
                                                    currentFontType = 1;
                                                }
                                                if (currentFontType == 2) {
                                                    currentFontType = 3;
                                                }
                                            }
                                            case "§o" -> {
                                                if (currentFontType == 0) {
                                                    currentFontType = 2;
                                                }
                                                if (currentFontType == 1) {
                                                    currentFontType = 3;
                                                }
                                            }
                                            case "§r" -> {
                                                currentColor = finalColor;
                                                currentFontType = 0;
                                            }
                                            case "§n","§m","§k" -> {
                                                // ignore
                                            }
                                            default -> {
                                                // only colors should remain at this point
                                                var tempC = TextColor.fromLegacyFormat(temp);
                                                if (tempC != null) currentColor = new Color((tempC.getValue() / 256 / 256) % 256, (tempC.getValue() / 256) % 256, tempC.getValue() % 256);
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
                if (e.effect().getIdentifier().equals("book-content")) {
                    replaced.set(true);
                    return new ConfiguredEffect(
                            bookEffect,
                            new ArrayList<>()
                    );
                }
                return e;
            });
            if (!replaced.get()) {
                effects.add(new ConfiguredEffect(
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
            for (ConfiguredEffect effect : effects) {
                player.sendSystemMessage(Component.literal(" -> " + effect.effect().getIdentifier() + ", " + String.join(",", effect.data())), false);
            }
            player.sendSystemMessage(Component.literal("§6Generating..."), false);

            BufferedImage m = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_4BYTE_ABGR);
            try {
                m = Map.compositeImage(player, new CanvasDimensions(finalWidth, finalHeight), effects);
            } catch (Exception e) {
                Book2Map.LOGGER.warn("Map.compositeImage() failed: {}", e.toString());
                player.sendSystemMessage(Component.literal("§cFailed to generate map! Check your settings!"), false);
                return;
            }

            BufferedImage finalM = m;
            CompletableFuture.supplyAsync(() -> Map.render(finalM, finalDither, finalWidth, finalHeight)).thenAcceptAsync(mapImage -> {
                var items = Map.toVanillaItems(mapImage, player.level());
                Map.giveToPlayer(player, items, bundleLore, finalWidth, finalHeight);
                player.sendSystemMessage(Component.literal("§6Done!"), false);
            }, player.level().getServer());
        }
        else player.sendSystemMessage(Component.nullToEmpty("§6You need to hold a book to use this command!"), false);
    }

    public static void giveToPlayer(Player player, List<ItemStack> items, String loreText, int width, int height) {
        if (items.size() == 1) {
            player.addItem(items.getFirst());
        } else {
            var bundle = new ItemStack(Items.BUNDLE);
            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
            bundle.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(loreText), Component.literal("Use this bundle on a wall of item frames!").withColor(TextColor.GOLD))));
            bundle.set(DataComponents.CUSTOM_NAME, Component.literal("Converted Book").withStyle(ChatFormatting.GOLD));
            CompoundTag n = new CompoundTag();
            n.putBoolean("image2map:quick_place", true);
            n.putInt("image2map:width", Mth.ceil(width / 128.0d));
            n.putInt("image2map:height", Mth.ceil(height / 128.0d));
            bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(n));
            player.addItem(bundle);
        }
    }

    public static String convertTextCompound(String raw) {
        return raw;
        // TODO: debug this, wait for sgui update
        /*
        CompoundTag n = new CompoundTag();
        JsonObject rootObj;
        try {
            rootObj = JsonParser.parseString(n.asString().get()).getAsJsonObject();
        } catch (Exception ignored) { return n.asString().get(); }
        JsonArray extra = rootObj.getAsJsonArray("extra");
        StringBuilder out = new StringBuilder();

        for (var i = 0; i < extra.size(); i++)
        {
            JsonElement o = extra.get(i);

            String color = "";
            try {
                color = o.getAsJsonObject().get("color").getAsString();
                if (color == null) color = "";
            } catch (Exception ignored) {}

            boolean bold = false;
            try {
                bold = o.getAsJsonObject().get("bold").getAsBoolean();
            } catch (Exception ignored) {}

            boolean italic = false;
            try {
                italic = o.getAsJsonObject().get("italic").getAsBoolean();
            } catch (Exception ignored) {}

            String text = "";
            try {
                text = o.getAsJsonObject().get("text").getAsString();
                if (text == null) text = "";
            } catch (Exception ignored) {}

            out.append(bold ? ChatFormatting.BOLD : "").append(italic ? ChatFormatting.ITALIC : "");

            if (!color.isEmpty()) {
                out.append(ChatFormatting.getByName(color.toUpperCase(Locale.ROOT)));
            }

            out.append(text);
        }

        String defaultText = "";
        try {
            defaultText = rootObj.get("text").getAsString();
            if (defaultText == null) defaultText = "";
        } catch (Exception ignored) {}
        out.append(defaultText);

         */

        /*
            {
                "extra": [
                    {
                        "text": "Bold "
                    },
                    {
                        "color": "dark_green",
                        "text": "C"
                    },
                    {
                        "text": "o"
                    },
                    {
                        "color": "yellow",
                        "text": "l"
                    },
                    {
                        "color": "green",
                        "text": "o"
                    },
                    {
                        "color": "gold",
                        "text": "r"
                    },
                    {
                        "color": "dark_red",
                        "text": "s"
                    },
                    {
                        "text": "\n"
                    },
                    {
                        "italic": true,
                        "text": "Italic"
                    },
                    {
                        "bold": true,
                        "italic": true,
                        "text": " BoldItalic"
                    },
                    {
                        "bold": true,
                        "text": "\n1\n2\n3\n4\n5\n6\n7\n8"
                    }
                ],
                "text": ""
            }
         */
        //return out.toString();
    }
}


