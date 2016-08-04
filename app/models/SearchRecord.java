package models;


import com.avaje.ebean.Model;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by cailianjie on 2016-7-22.
 */

@Entity
public class SearchRecord extends Model {


    @Id
    Long recordId;

    Date createDate;
    Boolean isBase;

    double score;

    @OneToMany(mappedBy = "record")
    List<SearchRecordDetail> detailList;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Boolean getBase() {
        return isBase;
    }

    public void setBase(Boolean base) {
        isBase = base;
    }

    public List<SearchRecordDetail> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<SearchRecordDetail> detailList) {
        this.detailList = detailList;
    }

    public static Finder<Long,SearchRecord> finder = new Finder<Long, SearchRecord>(SearchRecord.class);

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
