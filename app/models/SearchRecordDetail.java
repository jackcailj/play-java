package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.SqlRow;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cailianjie on 2016-7-22.
 */

@Entity
public class SearchRecordDetail extends Model{

    @Id
    Long recordDetailId;


/*    @JoinColumn
    Long recordId;*/

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "RECORD_ID")
    SearchRecord record;

    @OneToOne
    SearchMedia searchMedia = new SearchMedia();

   /* String hotKeyword;

    Long mediaId;

    String mediaName;

    String author;

    String publish;*/

    int position;

    Double ndcg;


    public Long getRecordDetailId() {
        return recordDetailId;
    }

    public void setRecordDetailId(Long recordDetailId) {
        this.recordDetailId = recordDetailId;
    }

    public SearchRecord getRecord() {
        return record;
    }

    public void setRecord(SearchRecord record) {
        this.record = record;
    }

    public Double getNdcg() {
        return ndcg;
    }

    public void setNdcg(Double ndcg) {
        this.ndcg = ndcg;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }


    public SearchMedia getSearchMedia() {
        return searchMedia;
    }

    public void setSearchMedia(SearchMedia media) {
        this.searchMedia = media;
    }

    public static Finder<Long,SearchRecordDetail> finder = new Finder<Long, SearchRecordDetail>(SearchRecordDetail.class);


    public static List<SearchRecordDetail> getRecordDetails(String keyword,Long recordId){

        String sql = "SELECT * from search_record_detail d\n" +
                "left join search_media m on d.search_media_id=m.id where record_id="+recordId+" and m.keyword='"+keyword+"' ORDER BY  d.position ";

        SearchRecord searchRecord = SearchRecord.finder.byId(recordId);

        List<SqlRow> details = Ebean.createSqlQuery(sql).findList();
        List<SearchRecordDetail> recordDetails = new ArrayList<>();
        for(SqlRow row : details){

            SearchMedia searchMedia = new SearchMedia();
            searchMedia.setId(row.getInteger("id"));
            searchMedia.setKeyword(row.getString("keyword"));
            searchMedia.setAuthor(row.getString("author"));
            searchMedia.setMediaId(row.getLong("media_id"));
            searchMedia.setMediaName(row.getString("media_name"));
            searchMedia.setPublisher(row.getString("publisher"));
            searchMedia.setScore(row.getInteger("score"));

            SearchRecordDetail detail = new SearchRecordDetail();
            detail.setSearchMedia(searchMedia);
            detail.setPosition(row.getInteger("position"));
            detail.setRecordDetailId(row.getLong("record_detail_id"));
            detail.setRecord(searchRecord);

            recordDetails.add(detail);

        }

        return recordDetails;
    }
}
