package dev.smto.book2map.content;

import com.mojang.brigadier.CommandDispatcher;
import dev.smto.book2map.api.CompositeEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permissions;
import java.awt.*;
import java.net.URI;

import static net.minecraft.commands.Commands.literal;
public class Commands {
    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "StringBufferReplaceableByString"})
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("b2m")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.nullToEmpty(
                            ChatFormatting.GOLD.toString() + ChatFormatting.BOLD + "Book2Map" + "\n" + ChatFormatting.RESET +
                    ChatFormatting.GOLD + "This command allows you to create a map from a book.\n" +
                                    "For basic usage, simply run " + ChatFormatting.GREEN + "\"/b2m generate\"" + ChatFormatting.GOLD + " while holding a book.\n" +
                                    "More advanced players can adjust a bunch of settings in the book itself.\n" +
                                    "For more information, run " + ChatFormatting.GREEN + "\"/b2m help\"" + ChatFormatting.GOLD + "!"
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
                            MutableComponent text = Component.literal(ChatFormatting.GOLD.toString() + ChatFormatting.BOLD + "Book2Map Example" + "\n" + ChatFormatting.RESET);
                            text.append(ChatFormatting.GOLD + "These are some example settings:\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "book2map\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "color:red\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "font:Monocraft\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "width:2\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "height:2\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "size:24\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "left:8\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "dither:no\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "e:background-texture,stone\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.AQUA + "e:frame,blue\n\n");
                            text.append(ChatFormatting.RESET.toString() + ChatFormatting.GOLD + "Click this message for more information!");
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
                            b.append(ChatFormatting.GOLD.toString() + ChatFormatting.BOLD + "Book2Map Help" + "\n" + ChatFormatting.RESET);
                            b.append(ChatFormatting.GOLD + "To customize the resulting image, simply add a new line with the content " + ChatFormatting.GREEN + "\"book2map\"" + ChatFormatting.GOLD + " in the book.\n");
                            b.append("Any lines after that will be treated as settings.\n");
                            b.append("Simply add a line with the name of the setting you want to change, and the value(s) you want to set it to.\n\n");
                            b.append("To see a list of all available options, run " + ChatFormatting.GREEN + "\"/b2m options\"" + ChatFormatting.GOLD + ".\n");
                            b.append("To see a list of all available effects, run " + ChatFormatting.GREEN + "\"/b2m effects\"" + ChatFormatting.GOLD + ".\n");
                            b.append("\n" + ChatFormatting.GOLD);
                            b.append("Run " + ChatFormatting.GREEN + "\"/b2m example\"" + ChatFormatting.GOLD + " to see an example or to visit the wiki.");

                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("options")
                        .executes(context -> {
                            StringBuilder b = new StringBuilder();
                            b.append(ChatFormatting.GOLD.toString() + ChatFormatting.BOLD + "Book2Map Options" + "\n" + ChatFormatting.RESET + ChatFormatting.GOLD);
                            b.append("The following options are available:\n");
                            b.append(ChatFormatting.AQUA + "font" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"font name\n");
                            b.append(ChatFormatting.AQUA + "size" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"text size\n");
                            b.append(ChatFormatting.AQUA + "dither" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"whether to use dithering\n");
                            b.append(ChatFormatting.AQUA + "color" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"text color\n");
                            b.append(ChatFormatting.AQUA + "top" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"offset from the top in pixels\n");
                            b.append(ChatFormatting.AQUA + "left" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"offset from the left side in pixels\n");
                            b.append(ChatFormatting.AQUA + "width" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"image width in blocks\n");
                            b.append(ChatFormatting.AQUA + "height" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"image height in blocks\n");
                            b.append(ChatFormatting.AQUA + "effect" + ChatFormatting.GOLD + " - "+ChatFormatting.GREEN+"effect, see /b2m effects\n");
                            b.append(ChatFormatting.GOLD + "\n");
                            b.append("All options can also be shortened to the first letter (ex. font: -> f:).\n");

                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("effects")
                        .executes(context -> {
                            StringBuilder b = new StringBuilder();
                            b.append(ChatFormatting.GOLD.toString() + ChatFormatting.BOLD + "Book2Map Effects" + "\n" + ChatFormatting.RESET + ChatFormatting.GOLD);
                            b.append("The following built-in effects are available:\n");

                            for (CompositeEffect effect : CompositeEffects.getBuiltinEffects()) {
                                b.append(ChatFormatting.AQUA + effect.getIdentifier() + ChatFormatting.GOLD + " - " + ChatFormatting.GREEN + effect.getDescription() + "\n");
                            }
                            var otherEffects = CompositeEffects.getOtherEffects();
                            if (!otherEffects.isEmpty()) {
                                b.append("The following third-party effects are available:\n");
                                for (CompositeEffect effect : otherEffects) {
                                    b.append(ChatFormatting.AQUA + effect.getIdentifier() + ChatFormatting.GOLD + " - " + ChatFormatting.GREEN + effect.getDescription() + "\n");
                                }
                            }
                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("fonts")
                        .executes(context -> {
                            StringBuilder b = new StringBuilder();
                            b.append(ChatFormatting.GOLD.toString() + ChatFormatting.BOLD + "List of installed fonts" + "\n" + ChatFormatting.RESET);
                            b.append("Any of these fonts should be usable with Book2Map:\n");
                            for (Font font : Fonts.getAvailableFonts()) {
                                b.append(ChatFormatting.AQUA + font.getFontName() + ChatFormatting.GOLD + "\n");
                            }
                            context.getSource().sendSuccess(() -> Component.nullToEmpty(b.toString()), false);
                            return 0;
                        })
                )
                .then(literal("reload")
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .executes(context -> {
                            Fonts.reload();
                            context.getSource().sendSuccess(() -> Component.nullToEmpty(ChatFormatting.GOLD + "Reloaded book2map!"), false);
                            return 0;
                        })
                )
        );
    }
}
