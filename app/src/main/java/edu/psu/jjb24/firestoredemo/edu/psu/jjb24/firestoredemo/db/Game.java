package edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Game {
    private int imageIndex;
    private String user1ID;
    private String user2ID;
    private List<String> taboo;
    private List<String> user1Guess = new ArrayList<String>();
    private List<String> user2Guess = new ArrayList<String>();

    public Game() { }

    public Game(int imageIndex, String user1ID, String user2ID, List<String> taboo, List<String> user1Guess, List<String> user2Guess) {
        this.imageIndex = imageIndex;
        this.taboo = taboo;
        this.user1ID = user1ID;
        this.user2ID = user2ID;
        if (user1Guess != null) this.user1Guess = user1Guess;
        if (user2Guess != null) this.user2Guess = user2Guess;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public String getUser1ID() {
        return user1ID;
    }

    public String getUser2ID() {
        return user2ID;
    }

    public void setUser2ID(String user2ID) {
        this.user2ID = user2ID;
    }

    public List<String> getTaboo() {
        return taboo;
    }

    public List<String> getUser1Guess() {
        return user1Guess;
    }

    public List<String> getUser2Guess() {
        return user2Guess;
    }
}