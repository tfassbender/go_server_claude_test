package net.tfassbender.game.go;

public enum Stone {
    BLACK,
    WHITE;

    public Stone opposite() {
        return this == BLACK ? WHITE : BLACK;
    }

    public String toDisplayString() {
        return this == BLACK ? "black" : "white";
    }

    public static Stone fromString(String color) {
        if ("black".equalsIgnoreCase(color)) {
            return BLACK;
        } else if ("white".equalsIgnoreCase(color)) {
            return WHITE;
        }
        throw new IllegalArgumentException("Invalid color: " + color);
    }
}
