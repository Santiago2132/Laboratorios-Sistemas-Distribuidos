package interfaces;

import java.util.List;

import classes.Song;

public interface InterfaceSong {
    List<Song> searchByTitle(String title);
    List<Song> searchByGenre(String genre);
    List<Song> searchByAuthor(String author);
    List<Song> searchByMultipleCriteria(String title, String genre, String author);
}