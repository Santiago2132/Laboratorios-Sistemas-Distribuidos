public class Song {
    private String title;
    private String genre;
    private String author;
    private String language;
    private int year;
    
    public Song() {}
    
    public Song(String title, String genre, String author, String language, int year) {
        this.title = title;
        this.genre = genre;
        this.author = author;
        this.language = language;
        this.year = year;
    }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}