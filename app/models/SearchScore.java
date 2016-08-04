package models;

import com.avaje.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * Created by cailianjie on 2016-7-27.
 */
@Entity
public class SearchScore extends Model{

    @Id
    Long id;

    @OneToOne
    @JoinColumn(name = "RECORD_ID")
    SearchRecord record;

    String keyword;

    Double dcg;

    Double maxdcg;

    Long zeroNum;

    Long oneNum;

    Long recordNum;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SearchRecord getRecord() {
        return record;
    }

    public void setRecord(SearchRecord record) {
        this.record = record;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Double getDcg() {
        return dcg;
    }

    public void setDcg(Double dcg) {
        this.dcg = dcg;
    }

    public Double getMaxdcg() {
        return maxdcg;
    }

    public void setMaxdcg(Double maxDcg) {
        this.maxdcg = maxDcg;
    }

    public Long getZeroNum() {
        return zeroNum;
    }

    public void setZeroNum(Long zeroNum) {
        this.zeroNum = zeroNum;
    }

    public Long getOneNum() {
        return oneNum;
    }

    public void setOneNum(Long oneNum) {
        this.oneNum = oneNum;
    }

    public Long getRecordNum() {
        return recordNum;
    }

    public void setRecordNum(Long recordNum) {
        this.recordNum = recordNum;
    }

    public static Finder<Long,SearchScore> finder = new Finder<Long, SearchScore>(SearchScore.class);
}
