package pw.smto.book2map;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

public class CompositeEffects {
    public interface CompositeEffect {
        public String getIdentifier();
        public String getDescription();
        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments);
    }

    public static CompositeEffect getByIdentifier(String identifier) {
        for (CompositeEffect effect : effects) {
            if (effect.getIdentifier().equals(identifier)) {
                return effect;
            }
        }
        return NONE;
    }

    public record CanvasData(int width, int height) {}

    public static CompositeEffect BACKGROUND = new CompositeEffect() {
        public String getIdentifier() {
            return "background";
        }
        public String getDescription() {
            return "fills the background with a solid color";
        }
        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
            g.setColor(Colors.fromString(arguments.get(0)));
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
        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
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
            try {
                var c = Colors.fromString(color);
                var R = (c.getRed() / 256 / 256) % 256;
                var G = (c.getGreen() / 256) % 256;
                var B = c.getBlue() % 256;
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
        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
            // check if pack exists
            var texture = arguments.get(0);
            Path targetFile = Path.of(Book2Map.CONFIG_TEXTURES_DIR.toString(), texture + ".png");
            BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR);
            if (Files.exists(targetFile)) {
                try {
                    image = ImageIO.read(targetFile.toFile());
                } catch (Exception e) {
                    Book2Map.Logger.error(e.toString());
                    return "Error while loading texture! Please check your spelling.";
                }
            }
            else {
                return "Error while loading texture! Please check your spelling.";
            }
            Image resizedImage = image.getScaledInstance(d.width, d.height, Image.SCALE_DEFAULT);
            BufferedImage resized = Map.convertToBufferedImage(resizedImage);
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

        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
            int thickness = 4;
            Color c = Color.YELLOW;
            if (arguments.size() == 1) {
                c = Colors.fromString(arguments.get(0), Color.YELLOW);
            }
            else if (arguments.size() == 2) {
                c = Colors.fromString(arguments.get(0), Color.YELLOW);
                try {
                    thickness = Integer.parseInt(arguments.get(1));
                } catch (Exception e) {
                    return "Error while parsing thickness argument!";
                }
            }
            g.setColor(c);
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

        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
            Color color = Color.YELLOW;
            int x = 0;
            int y = 0;
            int width = d.width();
            int height = d.height();
            boolean hollow = false;
            if (!arguments.isEmpty()) {
                try {
                    if (arguments.size() == 1) {
                        color = Colors.fromString(arguments.get(0));
                    }
                    if (arguments.size() == 2) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                    }
                    if (arguments.size() == 3) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                    }
                    if (arguments.size() == 4) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                        width = Integer.parseInt(arguments.get(3));
                    }
                    if (arguments.size() == 5) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                        width = Integer.parseInt(arguments.get(3));
                        height = Integer.parseInt(arguments.get(4));
                    }
                    if (arguments.size() == 6) {
                        color = Colors.fromString(arguments.get(0));
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

        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
            Color color = Color.YELLOW;
            int x = 0;
            int y = 0;
            int width = d.width();
            int height = d.height();
            boolean hollow = false;
            if (!arguments.isEmpty()) {
                try {
                    if (arguments.size() == 1) {
                        color = Colors.fromString(arguments.get(0));
                    }
                    if (arguments.size() == 2) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                    }
                    if (arguments.size() == 3) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                    }
                    if (arguments.size() == 4) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                        width = Integer.parseInt(arguments.get(3));
                    }
                    if (arguments.size() == 5) {
                        color = Colors.fromString(arguments.get(0));
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                        width = Integer.parseInt(arguments.get(3));
                        height = Integer.parseInt(arguments.get(4));
                    }
                    if (arguments.size() == 6) {
                        color = Colors.fromString(arguments.get(0));
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

        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
            Color color = Color.YELLOW;
            int x1 = 0;
            int y1 = 0;
            int x2 = 32;
            int y2 = 32;
            if (!arguments.isEmpty()) {
                try {
                    if (arguments.size() == 1) {
                        color = Colors.fromString(arguments.get(0));
                    }
                    if (arguments.size() == 2) {
                        color = Colors.fromString(arguments.get(0));
                        x1 = Integer.parseInt(arguments.get(1));
                    }
                    if (arguments.size() == 3) {
                        color = Colors.fromString(arguments.get(0));
                        x1 = Integer.parseInt(arguments.get(1));
                        y1 = Integer.parseInt(arguments.get(2));
                    }
                    if (arguments.size() == 4) {
                        color = Colors.fromString(arguments.get(0));
                        x1 = Integer.parseInt(arguments.get(1));
                        y1 = Integer.parseInt(arguments.get(2));
                        x2 = Integer.parseInt(arguments.get(3));
                    }
                    if (arguments.size() == 5) {
                        color = Colors.fromString(arguments.get(0));
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

    public static CompositeEffect TEXTURE = new CompositeEffect() {
        public String getIdentifier() {
            return "texture";
        }
        public String getDescription() {
            return "places a texture on the image";
        }

        public String apply(Graphics2D g, CanvasData d, java.util.List<String> arguments) {
            String texture = "";
            int x = 0;
            int y = 0;
            int width = d.width();
            int height = d.height();
            if (!arguments.isEmpty()) {
                try {
                    if (arguments.size() == 1) {
                        texture = arguments.get(0);
                    }
                    if (arguments.size() == 2) {
                        texture = arguments.get(0);
                        x = Integer.parseInt(arguments.get(1));
                    }
                    if (arguments.size() == 3) {
                        texture = arguments.get(0);
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                    }
                    if (arguments.size() == 4) {
                        texture = arguments.get(0);
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                        width = Integer.parseInt(arguments.get(3));
                    }
                    if (arguments.size() == 5) {
                        texture = arguments.get(0);
                        x = Integer.parseInt(arguments.get(1));
                        y = Integer.parseInt(arguments.get(2));
                        width = Integer.parseInt(arguments.get(3));
                        height = Integer.parseInt(arguments.get(4));
                    }
                } catch (Exception ignored) {
                    return "Error while parsing texture arguments! Remember to use this format: <texture>,<x>,<y>,<width>,<height>";
                }
            }

            Path targetFile = Path.of(Book2Map.CONFIG_TEXTURES_DIR.toString(), texture + ".png");
            BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR);
            if (Files.exists(targetFile)) {
                try {
                    image = ImageIO.read(targetFile.toFile());
                } catch (Exception e) {
                    Book2Map.Logger.error(e.toString());
                    return "Error while loading texture! This might indicate a server issue.";
                }
            }
            else {
                return "Specified texture does not exist! Please check your spelling.";
            }
            Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
            BufferedImage resized = Map.convertToBufferedImage(resizedImage);
            g.drawImage(resized, x, y, width, height, null);
            return "";
        }
    };


    public static CompositeEffect BOOK_CONTENT = new CompositeEffect() {
        public String getIdentifier() {
            return "book-content";
        }
        public String getDescription() {
            return "placeholder for the actual content of the book, use this to change the layering";
        }
        public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
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
        public String apply(Graphics2D g, CanvasData d, List<String> arguments) {
            return "Unknown effect was specified! Please check your spelling.";
        }
    };

    public static final CompositeEffects.CompositeEffect[] effects = {
            BACKGROUND,
            BACKGROUND_RANDOM,
            BACKGROUND_TEXTURE,
            FRAME,
            CIRCLE,
            RECTANGLE,
            LINE,
            TEXTURE,
            BOOK_CONTENT,
            NONE
    };
}