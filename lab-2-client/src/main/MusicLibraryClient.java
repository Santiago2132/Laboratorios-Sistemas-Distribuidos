package main;

import classes.MusicClient;

public class MusicLibraryClient {
    
    public static void main(String[] args) {
        MusicClient client = new MusicClient("127.0.0.1", 1804);
        client.startClient();
    }
}   