package dev.smto.book2map.api;

import java.util.ArrayList;
import java.util.List;

public record ConfiguredEffect(CompositeEffect effect, List<String> data) {
    public static ConfiguredEffect unconfigured(CompositeEffect effect) {
        return new ConfiguredEffect(effect, new ArrayList<>());
    }
}
