package me.renhai.taurus.spider;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.downloader.HttpClientDownloader;

@Component
public class TaurusCelebritySpiderRunner implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(TaurusCelebritySpiderRunner.class);

	private Site site = Site.me().setDomain("rottentomatoes.com").setTimeOut(10000).setSleepTime(2000)
			.setUserAgent(
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");
	
	@Value("${movie.json.data.store.path}")
	private String path;
	
	private Downloader downloader = new HttpClientDownloader();
	
	@Autowired
	private RabbitTemplate rabbitTemplate;
	
	@Override
	public void run(String... args) throws Exception {
		scanJsonDir();
		LOG.info("*****Task ended, Bye*****");
//		processPage("https://www.rottentomatoes.com/celebrity/tom_hanks");
	}
	
	private void scanJsonDir() throws Exception {
		path = StringUtils.removeEnd(path, "/");
		Iterator<File> iter = FileUtils.iterateFiles(new File(path + "/rottentomatoes.com"), new String[]{"json"}, false);
		while (iter.hasNext()) {
			File file = iter.next();
			String content = FileUtils.readFileToString(file, Charset.defaultCharset());
			JSONObject json = JSON.parseObject(content);
			JSONArray list = new JSONArray();
			if (json.containsKey("author")) {
				list.addAll(json.getJSONArray("author"));
			}
			if (json.containsKey("director")) {
				list.addAll(json.getJSONArray("director"));
			}
			if (json.containsKey("cast")) {
				list.addAll(json.getJSONArray("cast"));
			}
			 
			@SuppressWarnings("rawtypes")
			Iterator listIter = list.iterator();
			while (listIter.hasNext()) {
				JSONObject cel = (JSONObject) listIter.next();
				String link = "";
				if (cel.containsKey("sameAs")) {
					link = "https://www.rottentomatoes.com" + cel.getString("sameAs");
				} else if (cel.containsKey("link")) {
					link = "https://www.rottentomatoes.com" + cel.getString("link");
				}
				link = StringUtils.removeEnd(link, "/");
				if (StringUtils.isBlank(link)) continue;
				
				try {
					processPage(link);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}
	
	private void processPage(String link) throws Exception {
		JSONObject actor = new JSONObject();
		Page page = downloader.download(new Request(link), site.toTask());
		if (page == null) {
			return;
		}

		actor.put("link", link);
		actor.put("actorId", page.getHtml().$("meta[name=actorID]", "content").get());
		actor.put("birthday", page.getHtml().xpath("//td[@itemprop='birthDate']/text()").get());
		actor.put("birthplace", page.getHtml().xpath("//td[@itemprop='birthPlace']/text()").get());
		
		String suffix = StringUtils.substring(link, StringUtils.lastIndexOf(link, "/"));
		page = downloader.download(new Request(link + "/biography"), site.toTask());
		actor.put("bio", page.getHtml().xpath("//div[@id='bio_box']/section[1]/div/html()").get());
		FileUtils.writeStringToFile(new File(path + "/celebrity/" + suffix + ".json"), actor.toJSONString(), Charset.defaultCharset());
		
    	rabbitTemplate.convertAndSend(actor);
    	LOG.info("rabbit convert and send: " + actor.getString("link"));
	}

}
