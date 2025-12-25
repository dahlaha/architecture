package com.example.demo.models;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class UserStats {
    private int booksRead;
    private int friendsCount;
    private Map<String, Integer> topGenres;
    private int maxGenreCount;
    private int totalBooks;
    private int readingBooks;
    private int plannedBooks;
    private int droppedBooks;
    private int commentsCount;

    public UserStats() {
        this.topGenres = new HashMap<>();
        this.booksRead = 0;
        this.friendsCount = 0;
        this.maxGenreCount = 0;
        this.totalBooks = 0;
        this.readingBooks = 0;
        this.plannedBooks = 0;
        this.droppedBooks = 0;
        this.commentsCount = 0;
    }

    public void incrementGenreCount(String genre) {
        int count = topGenres.getOrDefault(genre, 0) + 1;
        topGenres.put(genre, count);
        if (count > maxGenreCount) {
            maxGenreCount = count;
        }
    }

    public int getBooksRead() {
        return booksRead;
    }

    public void setBooksRead(int booksRead) {
        this.booksRead = booksRead;
    }

    public int getFriendsCount() {
        return friendsCount;
    }

    public void setFriendsCount(int friendsCount) {
        this.friendsCount = friendsCount;
    }

    public Map<String, Integer> getTopGenres() {
        return topGenres;
    }

    public void setTopGenres(Map<String, Integer> topGenres) {
        this.topGenres = topGenres;
    }

    public int getMaxGenreCount() {
        return maxGenreCount;
    }

    public void setMaxGenreCount(int maxGenreCount) {
        this.maxGenreCount = maxGenreCount;
    }

    public int getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(int totalBooks) {
        this.totalBooks = totalBooks;
    }

    public int getReadingBooks() {
        return readingBooks;
    }

    public void setReadingBooks(int readingBooks) {
        this.readingBooks = readingBooks;
    }

    public int getPlannedBooks() {
        return plannedBooks;
    }

    public void setPlannedBooks(int plannedBooks) {
        this.plannedBooks = plannedBooks;
    }

    public int getDroppedBooks() {
        return droppedBooks;
    }

    public void setDroppedBooks(int droppedBooks) {
        this.droppedBooks = droppedBooks;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }
}