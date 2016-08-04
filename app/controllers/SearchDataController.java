package controllers;

import com.alibaba.fastjson.JSONObject;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.SqlUpdate;
import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool;
import com.sun.org.apache.xpath.internal.operations.Bool;
import models.*;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.OBJ_ADAPTER;
import org.springframework.util.CollectionUtils;
import play.Application;
import play.cache.CacheApi;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import views.html.search_result;

import javax.inject.Inject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cailianjie on 2016-7-25.
 */
public class SearchDataController extends Controller {

    CacheApi cache;
    @Inject  FormFactory formFactory;

    private  static Form<SearchData> searchDataForm;

    @Inject
    public SearchDataController(CacheApi cache){
        this.cache=cache;
    }

    public Result index(){

        searchDataForm = formFactory.form(SearchData.class);
        //Form<SearchDataForm> form = Form.form(SearchDataForm.class).bindFromRequest();
        //SearchDataForm searchDataForm = form.get();

        SearchData searchData = new SearchData();

        List<SearchKeyword> searchKeywords = SearchKeyword.finder.all();

        List<String> keywords = new ArrayList<>();
        for(SearchKeyword keyword:searchKeywords){
            keywords.add(keyword.getKeyword());
        }

        searchData.setKeywordOptions(keywords);

        //Form<SearchRecord> recordForm = Form.form(SearchRecord.class).bindFromRequest();
       // SearchRecord selectRecord=recordForm.get();
        //System.out.println(selectRecord.getRecordId());

        List<String> recordIds = new ArrayList<>();
        List<SearchRecord> searchRecords = SearchRecord.finder.all();
        for(SearchRecord record:searchRecords){
            recordIds.add(record.getRecordId().toString());
        }

        searchData.setRecordOptions(recordIds);

        searchData.setData(new ArrayList<SearchRecordDetail>());

        searchDataForm.fill(searchData);

        cache.set("keywords",keywords,60*24);
        cache.set("recordIds",recordIds,60*24);
        /*session("keywords", StringUtils.join(keywords,","));
        session("recordIds", StringUtils.join(recordIds,","));*/

        return ok(search_result.render(searchData.getKeywordOptions(),searchData.getRecordOptions(),searchDataForm));
    }


    public Result submit(){
        String[] postAction = request().body().asFormUrlEncoded().get("action");
        if("查询".equals(postAction[0])){
            return query();
        }
        else if("保存".equals(postAction[0])){
            return save();
        }
        else{
            return badRequest("请求不能被处理["+postAction[0]+"]");
        }
    }

    public Result query(){

        Form<SearchData> form = formFactory.form(SearchData.class).bindFromRequest();
        //searchDataForm = formFactory.form(SearchData.class).bindFromRequest();
        SearchData formData = form.get();

        //System.out.println(selectKey.getKeyword());


        /*Form<SearchRecord> recordForm = Form.form(SearchRecord.class).bindFromRequest();
        SearchRecord selectRecord=recordForm.get();
        System.out.println(selectRecord.getRecordId());

        *//*Map<String,Object> param = new HashMap<>();
        param.put("record_id",selectRecord.getRecordId());
        SearchRecordDetail.finder.where().allEq(param);*/

        List<SearchRecordDetail> searchRecordDetails = SearchRecordDetail.getRecordDetails(formData.getSelectKeyword(),formData.getSelectRecord());

        List<String> keywords = cache.get("keywords");
        if(keywords==null){
            List<SearchKeyword> searchKeywords = SearchKeyword.finder.all();

            keywords = new ArrayList<>();
            for(SearchKeyword keyword:searchKeywords){
                keywords.add(keyword.getKeyword());
            }
        }

        List<String> recordIds = cache.get("recordIds");
        if(recordIds==null) {
            List<SearchRecord> searchRecords = SearchRecord.finder.all();

            recordIds= new ArrayList<>();
            for (SearchRecord record : searchRecords) {
                recordIds.add(record.getRecordId().toString());
            }
        }


        formData.setData(searchRecordDetails);
        return ok(search_result.render(keywords,recordIds,form));

    }

    public Result save(){
        //searchDataForm = formFactory.form(SearchData.class).bindFromRequest();
        Map<String,String[]> medias = request().body().asFormUrlEncoded();


        List<SearchMedia> searchMedias = new ArrayList<>();
        for(String key:medias.keySet()){
            if(StringUtils.isBlank(key)){
                continue;
            }

            SearchMedia searchMedia = new SearchMedia();
            try {
                searchMedia.setId(Integer.parseInt(key));
            }catch (Exception e){
                continue;
            }

            searchMedia.setScore(Integer.parseInt(medias.get(key)[0]));

            searchMedias.add(searchMedia);
        }

        if(searchMedias.size()>0) {
            SearchMedia.db().updateAll(searchMedias);
        }
        return  query();
    }


    public Result calc(Long recordId,String hotkey){

        try {


            SearchRecord record = SearchRecord.finder.byId(recordId);

            List<SearchKeyword> keywords;
            if(StringUtils.isBlank(hotkey)) {
                keywords = SearchKeyword.finder.all();
            }else{
                keywords = new ArrayList<>();
                SearchKeyword searchKeyword = new SearchKeyword();
                searchKeyword.setKeyword(hotkey);
                keywords.add(searchKeyword);
            }
            /*List<SearchKeyword> keywords = new ArrayList<>();
            SearchKeyword keyword1 = new SearchKeyword();
            keyword1.setKeyword("围城");
            keywords.add(keyword1);*/

            SqlUpdate deleteSql = Ebean.createSqlUpdate("DELETE from search_score WHERE record_id ="+recordId);
            deleteSql.execute();

            List<String> errorKeys = new ArrayList<>();
            List<SearchScore> searchScores = new ArrayList<>();
            //Map<Long,SearchRecordDetail> recordDetailUpdateMap = new HashMap<>();
            List<SearchRecordDetail> recordDetails = new ArrayList<>();

            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5,10,5, TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(200),new ThreadPoolExecutor.CallerRunsPolicy());

            for (SearchKeyword keyword : keywords) {

                CalcThread calcThread = new CalcThread(keyword.getKeyword(),record);
                threadPoolExecutor.execute(calcThread);
               /* String sql = "SELECT * from search_record_detail d\n" +
                        "left join search_media m on d.search_media_id=m.id where record_id=" + record.getRecordId() + " and m.keyword='" + keyword.getKeyword() + "'";

               *//* Map<String,Object> param = new HashMap<>();
                param.put("record_id",record.getRecordId());
                param.put("searchMedia.keyword",keyword.getKeyword());
*//*
                //List<SearchRecordDetail> searchRecordDetails =SearchRecordDetail.finder.fetch("searchMedia", new FetchConfig().query()).where().allEq(param).findList();
               // System.out.println("searchRecordDetails size:"+searchRecordDetails.size());
                List<SqlRow> details = Ebean.createSqlQuery(sql).findList();

                sql = "SELECT * from search_record_detail d\n" +
                        "left join search_media m on d.search_media_id=m.id\n" +
                        "WHERE d.record_id="+record.getRecordId()+"  and m.keyword='"+keyword.getKeyword()+"' ORDER BY  m.score desc;\n";
                List<SqlRow> expectList = Ebean.createSqlQuery(sql).findList();

                //List<SearchRecordDetail> expectRecordDetails =SearchRecordDetail.finder.fetch("searchMedia", new FetchConfig().query()).where().allEq(param).orderBy("searchMedia.score desc ").findList();

                SearchScore searchScore = new SearchScore();
                searchScore.setRecord(record);
                searchScore.setKeyword(keyword.getKeyword());



                Double sumScore = 0d;
                Long zeroNum=0l;
                Long oneNum=0l;
                Boolean bCalc=true;

                Double lastScore= 0d;
                Double maxDcgLastScore = 0d;
                for(int i=0;i<details.size();i++){
                    SqlRow row = details.get(i);
                    SqlRow expectRow = expectList.get(i);

                    Integer position = row.getInteger("position");
                    Integer mScore = row.getInteger("score");
                    if(mScore == null){
                        bCalc=false;
                        errorKeys.add(keyword.getKeyword());
                        break;
                    }
                    if(mScore==0){
                        zeroNum++;
                    }
                    if(mScore==3){
                        oneNum++;
                    }
                    //Double score = mScore.doubleValue()/Math.log(position + 2);


                    //计算DCG
                    Double score = lastScore + mScore.doubleValue() * (Math.log(2)/Math.log(position + 2));

                    System.out.println("lastScore:"+lastScore+" manual:"+mScore.doubleValue() +" result:"+score);
                    //计算Max DCG;
                    Double maxScore = maxDcgLastScore + expectRow.getInteger("score").doubleValue() * (Math.log(2)/Math.log(i + 2));
                    System.out.println("maxDcgLastScore:"+maxDcgLastScore+" expect manual:"+expectRow.getInteger("score").doubleValue() +" maxDcgLastScore result:"+maxScore);

                    //归一
                    Double ndcg=0d;
                    if(maxScore>0d) {
                        ndcg = score / maxScore;
                    }

                    lastScore=score;
                    maxDcgLastScore=maxScore;
                    sumScore += ndcg;

                    SearchRecordDetail searchRecordDetail = new SearchRecordDetail();
                    searchRecordDetail.setRecordDetailId(row.getLong("record_detail_id"));
                    searchRecordDetail.setNdcg(ndcg);

                    SearchMedia media = new SearchMedia();
                    media.setId(row.getInteger("search_media_id"));
                    searchRecordDetail.setSearchMedia(media);
                    recordDetails.add(searchRecordDetail);
                    System.out.println("record_detail_id:"+searchRecordDetail.getRecordDetailId()+"    ndcg:"+searchRecordDetail.getNdcg());
                    //recordDetailUpdateMap.put(row.getLong("record_detail_id"),ndcg);
                    //System.out.println(JSONObject.toJSONString(row));
                }

                searchScore.setScore(sumScore);
                searchScore.setZeroNum(zeroNum);
                searchScore.setOneNum(oneNum);
                searchScore.setRecordNum(Long.valueOf(details.size()));

                if(bCalc) {
                    searchScores.add(searchScore);
                }*/
            }

            /*for(Long key : recordDetailUpdateMap.keySet()){
                SqlUpdate sqlUpdate = Ebean.createSqlUpdate("update search_record_detail set score="+recordDetailUpdateMap.get(key)+" where record_detail_id="+key);
                sqlUpdate.execute();
            }*/

            //批量保存
           /* System.out.println("recordDetails size:"+recordDetails.size());
            SearchRecordDetail.db().updateAll(recordDetails);
            SearchScore.db().insertAll(searchScores);*/
            /*for (SearchScore searchScore : searchScores) {
                searchScore.save();
            }*/


            threadPoolExecutor.shutdown();

            boolean isEnd = true;
            do{
                isEnd = !threadPoolExecutor.awaitTermination(2, TimeUnit.SECONDS);
                System.out.println("检测任务是否结束。。。");
            }while(isEnd);

            SqlRow row =Ebean.createSqlQuery("SELECT sum(dcg)/sum(maxdcg) as ndcg from search_score where record_id="+record.getRecordId()).findUnique();
            record.setScore(row.getDouble("ndcg"));

            record.save();

            System.out.println("任务结束");
            if(CalcThread.errors.size()>0){
                return ok("打分失败:"+StringUtils.join(CalcThread.errors,","));
            }
            return ok("打分成功");
        }
        catch (Exception e){
            e.printStackTrace();

            return ok("出现异常");
        }

    }
}
