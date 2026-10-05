package fnafrts.core;

import java.awt.Color;

public record AnimatronicView(
        String id,
        String displayName,
        String symbol,
        Color color,
        String currentNodeId,
        String lastSeenNodeId,
        boolean forcedVisible) {}