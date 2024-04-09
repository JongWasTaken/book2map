package pw.smto;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
public class Commands {
    private static boolean initialized = false;

    public static void register(MinecraftServer server) {
        if (!initialized)
        {
            initialized = true;

            server.getCommandManager().getDispatcher().register(literal("b2m")
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.of(
                                TextHelper.GOLD + TextHelper.BOLD + "Book2Map" + "\n" + TextHelper.RESET +
                        TextHelper.GOLD + "This command allows you to create a map from a book.\n" +
                                        "For basic usage, simply run " + TextHelper.GREEN + "\"/b2m generate\"" + TextHelper.GOLD + " while holding a book.\n" +
                                        "More advanced players can adjust a bunch of settings in the book itself.\n" +
                                        "For more information, run " + TextHelper.GREEN + "\"/b2m help\"" + TextHelper.GOLD + "!"
                        ), false);
                        return 1;
                    })
                    .then(literal("generate")
                            .executes(context -> {
                                Map.createByCommand(context.getSource().getPlayer());
                                return 1;
                            })
                    )
                    .then(literal("example")
                            .executes(context -> {
                                MutableText text = Text.literal(TextHelper.GOLD + TextHelper.BOLD + "Book2Map Example" + "\n" + TextHelper.RESET);
                                text.append(TextHelper.GOLD + "These are some example settings:\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "book2map\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "color:red\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "font:Monocraft\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "width:2\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "height:2\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "size:24\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "left:8\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "dither:no\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "e:background-texture,stone\n");
                                text.append(TextHelper.RESET + TextHelper.AQUA + "e:frame,blue\n\n");
                                text.append(TextHelper.RESET + TextHelper.GOLD + "Click this message for more information!");
                                text.setStyle(Style.EMPTY
                                        .withBold(true)
                                        .withColor(Formatting.GREEN)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://github.com/JongWasTaken/book2map/wiki")));

                                context.getSource().sendFeedback(() -> text, false);
                                return 1;
                            })
                    )
                    .then(literal("help")
                            .executes(context -> {
                                StringBuilder b = new StringBuilder();
                                b.append(TextHelper.GOLD + TextHelper.BOLD + "Book2Map Help" + "\n" + TextHelper.RESET);
                                b.append(TextHelper.GOLD + "To customize the resulting image, simply add a new line with the content " + TextHelper.GREEN + "\"book2map\"" + TextHelper.GOLD + " in the book.\n");
                                b.append("Any lines after that will be treated as settings.\n");
                                b.append("Simply add a line with the name of the setting you want to change, and the value(s) you want to set it to.\n\n");
                                b.append("To see a list of all available options, run " + TextHelper.GREEN + "\"/b2m options\"" + TextHelper.GOLD + ".\n");
                                b.append("To see a list of all available effects, run " + TextHelper.GREEN + "\"/b2m effects\"" + TextHelper.GOLD + ".\n");
                                b.append("\n" + TextHelper.GOLD);
                                b.append("Run " + TextHelper.GREEN + "\"/b2m example\"" + TextHelper.GOLD + " to see an example or to visit the wiki.");

                                context.getSource().sendFeedback(() -> Text.of(b.toString()), false);
                                return 1;
                            })
                    )
                    .then(literal("options")
                            .executes(context -> {
                                StringBuilder b = new StringBuilder();
                                b.append(TextHelper.GOLD + TextHelper.BOLD + "Book2Map Options" + "\n" + TextHelper.RESET + TextHelper.GOLD);
                                b.append("The following settings are available:\n");
                                b.append(TextHelper.AQUA + "font" + TextHelper.GOLD + " - "+TextHelper.GREEN+"font name\n");
                                b.append(TextHelper.AQUA + "size" + TextHelper.GOLD + " - "+TextHelper.GREEN+"text size\n");
                                b.append(TextHelper.AQUA + "dither" + TextHelper.GOLD + " - "+TextHelper.GREEN+"whether to use dithering\n");
                                b.append(TextHelper.AQUA + "color" + TextHelper.GOLD + " - "+TextHelper.GREEN+"text color\n");
                                b.append(TextHelper.AQUA + "top" + TextHelper.GOLD + " - "+TextHelper.GREEN+"offset from the top in pixels\n");
                                b.append(TextHelper.AQUA + "left" + TextHelper.GOLD + " - "+TextHelper.GREEN+"offset from the left side in pixels\n");
                                b.append(TextHelper.AQUA + "width" + TextHelper.GOLD + " - "+TextHelper.GREEN+"image width in pixels\n");
                                b.append(TextHelper.AQUA + "height" + TextHelper.GOLD + " - "+TextHelper.GREEN+"image height in pixels\n");
                                b.append(TextHelper.AQUA + "effect" + TextHelper.GOLD + " - "+TextHelper.GREEN+"effect, see /b2m effects\n");
                                b.append(TextHelper.GOLD + "\n");
                                b.append("All options can also be shortened to the first letter (ex. font: -> f:).\n");

                                context.getSource().sendFeedback(() -> Text.of(b.toString()), false);
                                return 1;
                            })
                    )
                    .then(literal("effects")
                            .executes(context -> {
                                StringBuilder b = new StringBuilder();
                                b.append(TextHelper.GOLD + TextHelper.BOLD + "Book2Map Effects" + "\n" + TextHelper.RESET + TextHelper.GOLD);
                                b.append("The following effects are available:\n");

                                var effects = new ArrayList<>(List.of(Map.CompositeEffects.effects));
                                effects.remove(effects.size()-1);
                                for (Map.CompositeEffects.CompositeEffect effect : effects) {
                                    b.append(TextHelper.AQUA + effect.getIdentifier() + TextHelper.GOLD + " - " + TextHelper.GREEN + effect.getDescription() + "\n");
                                }
                                context.getSource().sendFeedback(() -> Text.of(b.toString()), false);
                                return 1;
                            })
                    )
                    .then(literal("fonts")
                            .executes(context -> {
                                StringBuilder b = new StringBuilder();
                                b.append(TextHelper.GOLD + TextHelper.BOLD + "List of installed fonts" + "\n" + TextHelper.RESET);
                                b.append("Any of these fonts should be usable with Book2Map:\n");
                                for (Font font : Book2Map.FONTS) {
                                    b.append(TextHelper.AQUA + font.getFontName() + TextHelper.GOLD + "\n");
                                }
                                context.getSource().sendFeedback(() -> Text.of(b.toString()), false);
                                return 1;
                            })
                    )
            );

            server.getPlayerManager().getPlayerList().forEach(spe -> {
                server.getCommandManager().sendCommandTree(spe);
            });
        }
    }
}
