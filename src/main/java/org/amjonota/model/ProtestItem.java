package org.amjonota.model;

public class ProtestItem {

    private int id;
    private String author;
    private int authorID;
    private String postedDate;
    private String title;
    private String eventDate;
    private String summary;
    private String description;
    private String category;
    private int memberCount;
    private int bookmarkedCount;
    private String imgName;

    public ProtestItem(String author, int authorID, String postedDate, String title, String eventDate, String summary, String description, String category, int memberCount, int bookmarkedCount, String imgName) {
        this.author = author;
        this.authorID = authorID;
        this.postedDate = postedDate;
        this.title = title;
        this.eventDate = eventDate;
        this.summary = summary;
        this.description = description;
        this.category = category;
        this.memberCount = memberCount;
        this.bookmarkedCount = bookmarkedCount;
        this.imgName = imgName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAuthor() { return author; }
    public int getAuthorID() {return authorID;}
    public String getPostedDate() { return postedDate; }
    public String getTitle() { return title; }
    public String getEventDate() { return eventDate; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getMemberCount() { return memberCount; }
    public int getBookmarkedCount() {return bookmarkedCount;}
    public String getImgName() { return imgName;}
}
