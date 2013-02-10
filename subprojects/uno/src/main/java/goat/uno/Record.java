/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno;

/**
 * A record in the scores table, as a Javabean
 *
 * @author bc
 */
public class Record {
    private String name;
    private int gamesWon;
    private int gamesEntered;
    private int hiScore;
    private int totalScore;

    public Record() {
        name = "";
        gamesWon = 0;
        gamesEntered = 0;
        hiScore = 0;
        totalScore = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGamesEntered() {
        return gamesEntered;
    }

    public void setGamesEntered(int gamesEntered) {
        this.gamesEntered = gamesEntered;
    }

    public int getHiScore() {
        return hiScore;
    }

    public void setHiScore(int hiScore) {
        this.hiScore = hiScore;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public boolean equals(Record record) {
        if (name.equals(record.getName()))
            return true;
        return false;
    }
}
