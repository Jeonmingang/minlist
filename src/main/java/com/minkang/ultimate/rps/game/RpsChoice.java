
package com.minkang.ultimate.rps.game;

public enum RpsChoice {
    ROCK("바위"),
    PAPER("보"),
    SCISSORS("가위");

    private final String kr;
    RpsChoice(String kr) { this.kr = kr; }
    public String getKr() { return kr; }

    public static int compare(RpsChoice a, RpsChoice b) {
        if (a == b) return 0;
        switch (a) {
            case ROCK: return (b == SCISSORS) ? 1 : -1;
            case PAPER: return (b == ROCK) ? 1 : -1;
            case SCISSORS: return (b == PAPER) ? 1 : -1;
        }
        return 0;
    }
}
