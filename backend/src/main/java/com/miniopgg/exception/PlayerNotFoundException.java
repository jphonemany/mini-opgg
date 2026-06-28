package com.miniopgg.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String gameName, String tagLine) {
        super("No Riot account found for " + gameName + "#" + tagLine + ".");
    }
}
