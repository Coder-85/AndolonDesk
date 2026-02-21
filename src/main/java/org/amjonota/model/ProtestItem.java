package org.amjonota.model;

public class ProtestItem {

    private int id;
    private String author;
    private String postedDate;
    private String title;
    private String eventDate;
    private String summary;
    private String description;
    private String category;
    private int memberCount;

    public ProtestItem(String author, String postedDate, String title, String eventDate, String summary, String description, String category, int memberCount) {
        this.author = author;
        this.postedDate = postedDate;
        this.title = title;
        this.eventDate = eventDate;
        this.summary = summary;
        this.description = description;
        this.category = category;
        this.memberCount = memberCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAuthor() { return author; }
    public String getPostedDate() { return postedDate; }
    public String getTitle() { return title; }
    public String getEventDate() { return eventDate; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getMemberCount() { return memberCount; }
}
