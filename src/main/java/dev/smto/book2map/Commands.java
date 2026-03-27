package dev.smto.book2map;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.literal;
public class Commands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("b2m")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.nullToEmpty(
                            TextHelper.GOLD + TextHelper.BOLD + "Book2Map" + "\n" + TextHelper.RESET +
                    TextHelper.GOLD + "This command allows you to create a map from a book.\n" +
                                    "For basic usage, simply run " + TextHelper.GREEN + "\"/b2m generate\"" + TextHelper.GOLD + " while holding a book.\n" +
                                    "More advanced players can adjust a bunch of settings in the book itself.\n" +
                                    "For more information, run " + TextHelper.GREEN + "\"/b2m help\"" + TextHelper.GOLD + "!"
                    ), false);
                    return 0;
                })
                .then(literal("generate")
                        .executes(context -> {
                            if (context.getSource().getPlayer() != null) {
                                Map.createByCommand(context.getSource().getPlayer());
                                return 0;
                            }
                            return 1;
                        })
                )
                .then(literal("example")
                        .executes(context -> {
                            MutableComponent text = Component.literal(TextHelper.GOLD + TextHelper.BOLD + "Book2Map Example" + "\n" + TextHelper.RESET);
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
                                    .withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/JongWasTaken/book2map/wiki"))));

                            context.getSource().sendSuccess(() -> text, false);
                            return 0;
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

                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("options")
                        .executes(context -> {
                            StringBuilder b = new StringBuilder();
                            b.append(TextHelper.GOLD + TextHelper.BOLD + "Book2Map Options" + "\n" + TextHelper.RESET + TextHelper.GOLD);
                            b.append("The following options are available:\n");
                            b.append(TextHelper.AQUA + "font" + TextHelper.GOLD + " - "+TextHelper.GREEN+"font name\n");
                            b.append(TextHelper.AQUA + "size" + TextHelper.GOLD + " - "+TextHelper.GREEN+"text size\n");
                            b.append(TextHelper.AQUA + "dither" + TextHelper.GOLD + " - "+TextHelper.GREEN+"whether to use dithering\n");
                            b.append(TextHelper.AQUA + "color" + TextHelper.GOLD + " - "+TextHelper.GREEN+"text color\n");
                            b.append(TextHelper.AQUA + "top" + TextHelper.GOLD + " - "+TextHelper.GREEN+"offset from the top in pixels\n");
                            b.append(TextHelper.AQUA + "left" + TextHelper.GOLD + " - "+TextHelper.GREEN+"offset from the left side in pixels\n");
                            b.append(TextHelper.AQUA + "width" + TextHelper.GOLD + " - "+TextHelper.GREEN+"image width in blocks\n");
                            b.append(TextHelper.AQUA + "height" + TextHelper.GOLD + " - "+TextHelper.GREEN+"image height in blocks\n");
                            b.append(TextHelper.AQUA + "effect" + TextHelper.GOLD + " - "+TextHelper.GREEN+"effect, see /b2m effects\n");
                            b.append(TextHelper.GOLD + "\n");
                            b.append("All options can also be shortened to the first letter (ex. font: -> f:).\n");

                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("effects")
                        .executes(context -> {
                            StringBuilder b = new StringBuilder();
                            b.append(TextHelper.GOLD + TextHelper.BOLD + "Book2Map Effects" + "\n" + TextHelper.RESET + TextHelper.GOLD);
                            b.append("The following effects are available:\n");

                            var effects = new ArrayList<>(List.of(CompositeEffects.effects));
                            effects.removeLast();
                            for (CompositeEffects.CompositeEffect effect : effects) {
                                b.append(TextHelper.AQUA + effect.getIdentifier() + TextHelper.GOLD + " - " + TextHelper.GREEN + effect.getDescription() + "\n");
                            }
                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("fonts")
                        .executes(context -> {
                            StringBuilder b = new StringBuilder();
                            b.append(TextHelper.GOLD + TextHelper.BOLD + "List of installed fonts" + "\n" + TextHelper.RESET);
                            b.append("Any of these fonts should be usable with Book2Map:\n");
                            for (Font font : Fonts.LIST) {
                                b.append(TextHelper.AQUA + font.getFontName() + TextHelper.GOLD + "\n");
                            }
                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("reload")
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .executes(context -> {
                            Fonts.reload();
                            context.getSource().sendSuccess(() -> Component.nullToEmpty(TextHelper.GOLD + "Reloaded book2map!"), false);
                            return 0;
                        })
                )
        );
    }
}
