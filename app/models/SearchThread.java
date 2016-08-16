package models;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.ObjectArrayCodec;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.dbmigration.migration.Rollback;
import com.avaje.ebean.enhance.agent.SysoutMessageOutput;
import controllers.HttpDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.libs.concurrent.Execution;
import play.db.ebean.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * Created by cailianjie on 2016-7-23.
 */
public class SearchThread implements Runnable {
    private Logger logger = LoggerFactory.getLogger(SearchThread.class);

    public static Vector<String> errorKeys = new Vector<String>();

    String keyword;

    SearchRecord searchRecord;

    public  static String action ="searchBMedia";

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public SearchRecord getSearchRecord() {
        return searchRecord;
    }

    public void setSearchRecord(SearchRecord searchRecord) {
        this.searchRecord = searchRecord;
    }

    public static String getAction() {
        return action;
    }

    public static void setAction(String action) {
        SearchThread.action = action;
    }

    @Override
    @Transactional
    public void run() {
        logger.info("搜索:" + keyword);
        String result = null;

        JSONObject jsonObject = null;
        //final  JSONArray array = null;

        try {
            String url = "http://10.5.38.39:8082/media/api2.go?action="+action+"&keyword=" + keyword + "&start=0&end=100&stype=media&enable_f=1&returnType=json&deviceType=Android&channelId=30000&clientVersionNo=5.8.0&serverVersionNo=1.2.1&permanentId=20160621114933033507290850633545953&deviceSerialNo=863151026834264&macAddr=38%3Abc%3A1a%3Aa0%3Ab4%3A74&resolution=1080*1800&clientOs=5.0.1&platformSource=DDDS-P&channelType=&token=673180c17d884cb12512f137e214b902";
            logger.info("搜索Url:"+url);
            result = HttpDriver.doGet(url, null);
            jsonObject = JSONObject.parseObject(result);
            final JSONArray array = jsonObject.getJSONObject("data").getJSONArray("searchMediaPaperList");

            long time = System.currentTimeMillis();
            IntStream.range(0,array.size()).parallel().forEach(index->{
                JSONObject object = array.getJSONObject(index);

                SearchRecordDetail recordDetail = new SearchRecordDetail();
                recordDetail.setPosition(index);
                recordDetail.setRecord(searchRecord);

                Long mediaId = Long.parseLong(object.get("mediaId").toString());


                Map<String, Object> mapParam = new HashMap<String, Object>();
                mapParam.put("keyword", keyword);
                mapParam.put("media_id", mediaId);

                SearchMedia media = SearchMedia.finder.where().allEq(mapParam).findUnique();
                if (media == null) {
                    SearchMedia searchMedia = recordDetail.getSearchMedia();
                    searchMedia.setMediaId(mediaId);
                    searchMedia.setMediaName(object.get("title").toString());
                    searchMedia.setAuthor(object.get("author").toString());
                    searchMedia.setKeyword(keyword);
                    searchMedia.setPublisher(object.get("publisher").toString());
                    searchMedia.save();
                } else {
                    recordDetail.setSearchMedia(media);
                }

                recordDetail.save();
            });

        /*for (int i = 0; i < array.size(); i++) {
            JSONObject object = array.getJSONObject(i);

            SearchRecordDetail recordDetail = new SearchRecordDetail();
            recordDetail.setPosition(i);
            recordDetail.setRecord(searchRecord);

            Long mediaId = Long.parseLong(object.get("mediaId").toString());


            Map<String, Object> mapParam = new HashMap<String, Object>();
            mapParam.put("keyword", keyword);
            mapParam.put("media_id", mediaId);

            SearchMedia media = SearchMedia.finder.where().allEq(mapParam).findUnique();
            if (media == null) {
                SearchMedia searchMedia = recordDetail.getSearchMedia();
                searchMedia.setMediaId(mediaId);
                searchMedia.setMediaName(object.get("title").toString());
                searchMedia.setAuthor(object.get("author").toString());
                searchMedia.setKeyword(keyword);
                searchMedia.setPublisher(object.get("publisher").toString());
                searchMedia.save();
            } else {
                recordDetail.setSearchMedia(media);
            }

            recordDetail.save();
        }*/

            System.out.println("耗时："+(System.currentTimeMillis()-time));


        } catch (Exception e) {

            errorKeys.add(keyword);
            e.printStackTrace();
            return;
        }



    }

}
