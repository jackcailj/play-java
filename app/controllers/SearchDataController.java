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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by cailianjie on 2016-7-25.
 */
public class SearchDataController extends Controller {

    private Logger logger = LoggerFactory.getLogger(SearchDataController.class);

    CacheApi cache;
    @Inject  FormFactory formFactory;

    private  static Form<SearchData> searchDataForm;

    @Inject
    public SearchDataController(CacheApi cache){
        this.cache=cache;
    }

    /*
    显示搜索页面
     */
    public Result index(){

        searchDataForm = formFactory.form(SearchData.class);
        //Form<SearchDataForm> form = Form.form(SearchDataForm.class).bindFromRequest();
        //SearchDataForm searchDataForm = form.get();

        SearchData searchData = new SearchData();

        List<SearchKeyword> searchKeywords = SearchKeyword.finder.all();

        List<String> keywords= searchKeywords.stream().map(searchKeyword -> searchKeyword.getKeyword()).collect(Collectors.toList());

        searchData.setKeywordOptions(keywords);

        List<SearchRecord> searchRecords = SearchRecord.finder.all();
        List<String> recordIds =searchRecords.stream().map(record -> record.getRecordId().toString()).collect(Collectors.toList());

        searchData.setRecordOptions(recordIds);

        searchData.setData(new ArrayList<SearchRecordDetail>());

        searchDataForm.fill(searchData);

        cache.set("keywords",keywords,60*24);
        cache.set("recordIds",recordIds,60*24);

        return ok(search_result.render(searchData.getKeywordOptions(),searchData.getRecordOptions(),searchDataForm));
    }


    /*
    查询或者保存提交
     */
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

    /*
    查询逻辑
     */
    public Result query(){

        Form<SearchData> form = formFactory.form(SearchData.class).bindFromRequest();

        SearchData formData = form.get();

        List<SearchRecordDetail> searchRecordDetails = SearchRecordDetail.getRecordDetails(formData.getSelectKeyword(),formData.getSelectRecord());

        List<String> keywords = cache.get("keywords");
        if(keywords==null){
            List<SearchKeyword> searchKeywords = SearchKeyword.finder.all();
            keywords = searchKeywords.stream().map(searchKeyword -> searchKeyword.getKeyword()).collect(Collectors.toList());
        }

        List<String> recordIds = cache.get("recordIds");
        if(recordIds==null) {
            List<SearchRecord> searchRecords = SearchRecord.finder.all();

            recordIds= searchRecords.stream().map(searchRecord-> searchRecord.getRecordId().toString()).collect(Collectors.toList());

        }


        formData.setData(searchRecordDetails);
        return ok(search_result.render(keywords,recordIds,form));

    }

    /*
    保存数据
     */
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


    /*
    计算结果，并将结果保存到数据库

     */
    public Result calc(Long recordId,String hotkey){

        try {


            SearchRecord record = SearchRecord.finder.byId(recordId);

            List<SearchKeyword> keywords;
            String inStr = "";
            if(StringUtils.isBlank(hotkey)) {
                keywords = SearchKeyword.finder.all();
            }else{

                keywords = new ArrayList<>();

                String[] keys =hotkey.split(",");
                Stream<String> stream = Stream.of(keys);

                for(String key :keys){
                    SearchKeyword searchKeyword = new SearchKeyword();
                    searchKeyword.setKeyword(key);
                    keywords.add(searchKeyword);

                    //新的关键词,要更新到search_keyword表
                    Map<String, Object> keyParam = new HashMap<String, Object>();
                    keyParam.put("keyword", searchKeyword.getKeyword());
                    SearchKeyword keyword = SearchKeyword.finder.where().allEq(keyParam).findUnique();
                    if(keyword==null){
                        searchKeyword.save();
                    }

                    inStr+="'"+key+"',";
                }


            }

            if(StringUtils.isNotBlank(inStr)) {
                inStr = inStr.substring(0, inStr.length() - 1);
            }

            SqlUpdate deleteSql = Ebean.createSqlUpdate("DELETE from search_score WHERE record_id ="+recordId + (StringUtils.isBlank(inStr)?"":" and keyword in ("+inStr+")"));
            deleteSql.execute();

            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5,10,5, TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(200),new ThreadPoolExecutor.CallerRunsPolicy());

            for (SearchKeyword keyword : keywords) {

                CalcThread calcThread = new CalcThread(keyword.getKeyword(),record);
                threadPoolExecutor.execute(calcThread);
            }

            threadPoolExecutor.shutdown();

            boolean isEnd = true;
            do{
                isEnd = !threadPoolExecutor.awaitTermination(2, TimeUnit.SECONDS);
                logger.info("检测任务是否结束。。。");
            }while(isEnd);

            SqlRow row =Ebean.createSqlQuery("SELECT sum(dcg)/sum(maxdcg) as ndcg from search_score where record_id="+record.getRecordId()).findUnique();
            record.setScore(row.getDouble("ndcg"));

            record.save();

            logger.info("任务结束");
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
