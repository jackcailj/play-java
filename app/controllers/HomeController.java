package controllers;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.netty.util.concurrent.Promise;
import models.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.CollectionUtils;
import play.db.ebean.Transactional;
import play.libs.Time;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.*;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import views.html.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    @Inject  WSClient wsClient;

    private  static Logger logger = LoggerFactory.getLogger(HomeController.class);

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(index.render("Your new application is ready."));
    }


    /*
    调用搜索接口，记录搜索数据
     */
    public Result getSearchData(String hotkeyword,Long recordId,String env) throws Exception {

        if(StringUtils.isBlank(hotkeyword) && recordId !=null){
            throw new Exception("提供recordId参数必须提供hotkeyword参数");
        }


        SearchRecord searchRecord=null;
        if(recordId ==null ) {
            searchRecord = new SearchRecord();
            searchRecord.setCreateDate(new Date());
            searchRecord.setDetailList(new ArrayList<SearchRecordDetail>());
            searchRecord.save();
        }
        else{
            searchRecord = SearchRecord.finder.byId(recordId);
        }

        List<SearchKeyword> keywords;
        if(StringUtils.isNotBlank(hotkeyword)){
            keywords = new ArrayList<SearchKeyword>();


            String[] keys =hotkeyword.split(",");
            String inStr = "";
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

            if(StringUtils.isNotBlank(inStr)) {
                inStr = inStr.substring(0,inStr.length()-1);
                //删除recordId关键词对应的detail信息
                SqlUpdate deleteSql = Ebean.createSqlUpdate("DELETE  from search_record_detail  where record_id=" + searchRecord.getRecordId() + " and search_media_id in(select id from search_media where keyword in(" + inStr + "))");

                deleteSql.execute();
            }
        }
        else {
            keywords = SearchKeyword.finder.all();
        }

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(5,10,5,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(200),new ThreadPoolExecutor.CallerRunsPolicy());
        SearchThread.errorKeys.clear();
        //String[] hotkewords = {"南海"};

        //List<CompletableFuture> futures = new ArrayList<CompletableFuture>();
        //CompletableFuture[] futures = new CompletableFuture[keywords.size()];
        int i =0;
        for(SearchKeyword keyword : keywords) {

            SearchThread searchThread=new SearchThread();
            searchThread.setKeyword(keyword.getKeyword());
            searchThread.setSearchRecord(searchRecord);

            if(StringUtils.isNotBlank(env) && "a".equals(env.toLowerCase().trim())){
                SearchThread.setAction("searchMedia");
            }

            logger.info("当前执行搜索Action:"+SearchThread.action);

            threadPool.execute(searchThread);


            /*CompletableFuture<WSResponse> wsreponse = WS.url("http://e.dangdang.com/media/api2.go?action=searchMedia&keyword="+keyword.getKeyword()+"&start=0&end=20&stype=media&enable_f=1&returnType=json&deviceType=Android&channelId=30000&clientVersionNo=5.8.0&serverVersionNo=1.2.1&permanentId=20160621114933033507290850633545953&deviceSerialNo=863151026834264&macAddr=38%3Abc%3A1a%3Aa0%3Ab4%3A74&resolution=1080*1800&clientOs=5.0.1&platformSource=DDDS-P&channelType=&token=673180c17d884cb12512f137e214b902").get().toCompletableFuture();
            wsreponse.thenAccept((reponse)-> {
                JsonNode jsonNode = reponse.asJson();
                //JsonNode medialist = jsonNode.get("data").get("searchMediaPaperList");

                System.out.println(Thread.currentThread().getName());
                System.out.println(reponse.getUri());

            }).toCompletableFuture();

            futures[i++]=wsreponse;*/



        }

        //等待所有搜索完成。
        //CompletableFuture.allOf(futures).join();

        //等待所有线程完成
        threadPool.shutdown();

        boolean isEnd = true;
        do{
            isEnd = !threadPool.awaitTermination(2, TimeUnit.SECONDS);
        }while(isEnd);

        if(SearchThread.errorKeys.size()>0){
            return ok("如下key搜索失败:"+StringUtils.join(SearchThread.errorKeys,","));
        }
        return ok("执行成功");
    }

}
