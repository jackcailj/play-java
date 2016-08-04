package models;

import com.avaje.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;


/**
 * Created by cailianjie on 2016-7-22.
 */

@Entity
public class SearchKeyword extends Model{

    @Id
    Long id;

    String keyword;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public static Finder<Long,SearchKeyword> finder = new Finder<Long, SearchKeyword>(SearchKeyword.class);
}
