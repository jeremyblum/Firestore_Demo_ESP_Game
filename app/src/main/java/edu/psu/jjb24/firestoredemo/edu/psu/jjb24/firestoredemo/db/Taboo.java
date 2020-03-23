package edu.psu.jjb24.firestoredemo.edu.psu.jjb24.firestoredemo.db;

import java.util.List;

public class Taboo {
    private List<String> list;

    public Taboo() { }

    public Taboo(List<String> list) {
        this.list = list;
    }

    public List<String> list() {
        return list;
    }
}
