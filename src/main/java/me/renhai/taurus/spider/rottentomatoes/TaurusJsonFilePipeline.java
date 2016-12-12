package me.renhai.taurus.spider.rottentomatoes;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

public class TaurusJsonFilePipeline extends FilePersistentBase implements Pipeline {

//	private static Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL).addOptions(Option.SUPPRESS_EXCEPTIONS);

    private Logger logger = LoggerFactory.getLogger(getClass());

    public TaurusJsonFilePipeline(String path) {
        setPath(path);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        String path = this.path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR;
        try {
        	if (resultItems.get("link") == null) return;
        	ReadContext ctx = JsonPath.parse(resultItems.<String>get("songList"));
        	String albumName = ctx.read("$[0].album.name");
        	String singerName = resultItems.get("singer");
        	File file = getFile(path + singerName + " - " + albumName + ".json");
        	FileUtils.writeStringToFile(file, JSON.toJSONString(resultItems.getAll()), Charset.defaultCharset());
        } catch (Exception e) {
            logger.warn("write file error", e);
        }
    }

}
