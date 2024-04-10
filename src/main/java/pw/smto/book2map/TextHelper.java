package pw.smto.book2map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class TextHelper {
    // Formatting
    public static final String BOLD = "§l";
    public static final String ITALIC = "§o";
    public static final String RESET = "§r";
    public static final String STRIKETHROUGH = "§m";
    public static final String UNDERLINE = "§n";
    public static final String MTS = "§k";

    // Colors
    public static final String BLACK = "§0";
    public static final String DARK_BLUE = "§1";
    public static final String DARK_GREEN = "§2";
    public static final String DARK_AQUA = "§3";
    public static final String DARK_RED = "§4";
    public static final String DARK_PURPLE = "§5";
    public static final String GOLD = "§6";
    public static final String GRAY = "§7";
    public static final String DARK_GRAY = "§8";
    public static final String BLUE = "§9";
    public static final String GREEN = "§a";
    public static final String AQUA = "§b";
    public static final String RED = "§c";
    public static final String LIGHT_PURPLE = "§d";
    public static final String YELLOW = "§e";
    public static final String WHITE = "§f";


    public static String convertTextCompound(NbtString n) {
        JsonObject rootObj;
        try {
            rootObj = JsonParser.parseString(n.asString()).getAsJsonObject();
        } catch (Exception ignored) { return n.asString(); }
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

            out.append(bold ? BOLD : "").append(italic ? ITALIC : "");

            if (!color.isEmpty()) {
                out.append(Formatting.byName(color.toUpperCase(Locale.ROOT)));
            }

            out.append(text);
        }

        String defaultText = "";
        try {
            defaultText = rootObj.get("text").getAsString();
            if (defaultText == null) defaultText = "";
        } catch (Exception ignored) {}
        out.append(defaultText);

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
        return out.toString();
    }
}
