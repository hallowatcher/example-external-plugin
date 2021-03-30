package com.hallowatcher.dialogscraper;

public enum DialogType {
    Player("player"),
    NPC("npc"),
    Options("options");

    private final String dialogType;

    DialogType(String dialogType) {
        this.dialogType = dialogType;
    }

    public String getDialogType() {
        return this.dialogType;
    }
}
