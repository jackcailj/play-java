package models;

import com.avaje.ebean.Model;

import java.util.List;

/**
 * Created by cailianjie on 2016-7-28.
 */
public class SearchData extends Model{

    String selectKeyword;

    Long  selectRecord;

    List<String> recordOptions;

    List<String> keywordOptions;

    public  List<SearchRecordDetail> data;

    public String getSelectKeyword() {
        return selectKeyword;
    }

    public void setSelectKeyword(String selectKeyword) {
        this.selectKeyword = selectKeyword;
    }

    public Long getSelectRecord() {
        return selectRecord;
    }

    public void setSelectRecord(Long selectRecord) {
        this.selectRecord = selectRecord;
    }

    public List<String> getRecordOptions() {
        return recordOptions;
    }

    public void setRecordOptions(List<String> recordOptions) {
        this.recordOptions = recordOptions;
    }

    public List<String> getKeywordOptions() {
        return keywordOptions;
    }

    public void setKeywordOptions(List<String> keywordOptions) {
        this.keywordOptions = keywordOptions;
    }

    public List<SearchRecordDetail> getData() {
        return data;
    }

    public void setData(List<SearchRecordDetail> data) {
        this.data = data;
    }

    public void init(){

    }
}
