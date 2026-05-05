package dev.smto.book2map.api;

import java.util.List;

public record ConfiguredEffect(CompositeEffect effect, List<String> data) {
}
