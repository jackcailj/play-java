package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by cailianjie on 2016-8-1.
 */
public class CalcThread implements Runnable{

    public  static Vector<String> errors =new Vector<>();

    String keyword;
    SearchRecord record;

    public CalcThread(String keyword,SearchRecord record){
        this.keyword=keyword;
        this.record=record;
    }

    @Override
    public void run() {

        try {

            //List<SearchScore> searchScores = new ArrayList<>();
            //Map<Long,SearchRecordDetail> recordDetailUpdateMap = new HashMap<>();
            List<SearchRecordDetail> recordDetails = new ArrayList<>();

            String sql = "SELECT * from search_record_detail d\n" +
                    "left join search_media m on d.search_media_id=m.id where record_id=" + record.getRecordId() + " and m.keyword='" + keyword + "'";

               /* Map<String,Object> param = new HashMap<>();
                param.put("record_id",record.getRecordId());
                param.put("searchMedia.keyword",keyword.getKeyword());
*/
            //List<SearchRecordDetail> searchRecordDetails =SearchRecordDetail.finder.fetch("searchMedia", new FetchConfig().query()).where().allEq(param).findList();
            // System.out.println("searchRecordDetails size:"+searchRecordDetails.size());
            List<SqlRow> details = Ebean.createSqlQuery(sql).findList();

            sql = "SELECT * from search_record_detail d\n" +
                    "left join search_media m on d.search_media_id=m.id\n" +
                    "WHERE d.record_id=" + record.getRecordId() + "  and m.keyword='" + keyword + "' ORDER BY  m.score desc;\n";
            List<SqlRow> expectList = Ebean.createSqlQuery(sql).findList();

            //List<SearchRecordDetail> expectRecordDetails =SearchRecordDetail.finder.fetch("searchMedia", new FetchConfig().query()).where().allEq(param).orderBy("searchMedia.score desc ").findList();

            SearchScore searchScore = new SearchScore();
            searchScore.setRecord(record);
            searchScore.setKeyword(keyword);


            Double sumDcgScore = 0d;
            Double sumMaxDctScore = 0d;
            Long zeroNum = 0l;
            Long oneNum = 0l;
            Boolean bCalc = true;

            //Double lastScore = 0d;
            //Double maxDcgLastScore = 0d;
            for (int i = 0; i < details.size(); i++) {
                SqlRow row = details.get(i);
                SqlRow expectRow = expectList.get(i);

                Integer position = row.getInteger("position");
                Integer mScore = row.getInteger("score");
                if (mScore == null) {
                    bCalc = false;
                    break;
                }
                if (mScore == 0) {
                    zeroNum++;
                }
                if (mScore == 3) {
                    oneNum++;
                }
                //Double score = mScore.doubleValue()/Math.log(position + 2);


                //计算DCG
                Double score = mScore.doubleValue() * (Math.log(2) / Math.log(position + 2));

                System.out.println( " manual:" + mScore.doubleValue() + " result:" + score);
                //计算Max DCG;
                Double maxScore = expectRow.getInteger("score").doubleValue() * (Math.log(2) / Math.log(i + 2));
                System.out.println(" expect manual:" + expectRow.getInteger("score").doubleValue() + " maxDcgLastScore result:" + maxScore);

                //归一
        /*        Double ndcg = 0d;
                if (maxScore > 0d) {
                    ndcg = score / maxScore;
                }
*/
                //lastScore = score;
                //maxDcgLastScore = maxScore;
                sumDcgScore += score;
                sumMaxDctScore +=maxScore;

                SearchRecordDetail searchRecordDetail = new SearchRecordDetail();
                searchRecordDetail.setRecordDetailId(row.getLong("record_detail_id"));
                searchRecordDetail.setNdcg(score);

                SearchMedia media = new SearchMedia();
                media.setId(row.getInteger("search_media_id"));
                searchRecordDetail.setSearchMedia(media);
                recordDetails.add(searchRecordDetail);
                System.out.println("record_detail_id:" + searchRecordDetail.getRecordDetailId() + "    ndcg:" + searchRecordDetail.getNdcg());
                //recordDetailUpdateMap.put(row.getLong("record_detail_id"),ndcg);
                //System.out.println(JSONObject.toJSONString(row));
            }

/*            Double ndcg =0d;
            if(sumMaxDctScore!=0d) {
                ndcg = sumDcgScore / sumMaxDctScore;
            }*/
            searchScore.setDcg(sumDcgScore);
            searchScore.setMaxdcg(sumMaxDctScore);
            searchScore.setZeroNum(zeroNum);
            searchScore.setOneNum(oneNum);
            searchScore.setRecordNum(Long.valueOf(details.size()));

            if (bCalc) {

                //searchScores.add(searchScore);
                SearchRecordDetail.db().updateAll(recordDetails);
                SearchScore.db().insert(searchScore);
            }
            else{
                errors.add(keyword);
            }

        }catch (Exception e){
            e.printStackTrace();
            errors.add(keyword);
            throw e;
        }
    }
}
