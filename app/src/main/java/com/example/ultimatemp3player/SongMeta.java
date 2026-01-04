package com.example.ultimatemp3player;

import java.io.Serializable;

// Adding Serializable allows you to pass this object between Activity and Service
public class SongMeta implements Serializable {
    public String title;
    public String artist;
    public byte[] albumArt;

    public SongMeta(String title, String artist, byte[] albumArt) {
        this.title = title;
        this.artist = artist;
        this.albumArt = albumArt;
    }
}
