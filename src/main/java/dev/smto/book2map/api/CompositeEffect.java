package dev.smto.book2map.api;

import java.awt.*;
import java.util.List;

public interface CompositeEffect {
    String getIdentifier();

    String getDescription();

    String apply(Graphics2D g, CanvasDimensions d, List<String> arguments);
}
