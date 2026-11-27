package dev.cheat.client.module;

/** Module categories; each one is a panel in the ClickGUI (in this order). */
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    EXPLOIT("Exploit"),
    MISC("Misc"),
    CLIENT("Client");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
