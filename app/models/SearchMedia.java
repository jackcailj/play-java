package models;

import com.avaje.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Created by cailianjie on 2016-7-23.
 */

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"keyword","media_id"}))
public class SearchMedia extends Model{

    @Id
    Integer id;

    String keyword;

    Long mediaId;

    String mediaName;

    String author;

    String publisher;

    Integer score;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String hotKeyword) {
        this.keyword = hotKeyword;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public static Finder<Long,SearchMedia> finder = new Finder<Long, SearchMedia>(SearchMedia.class);

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
