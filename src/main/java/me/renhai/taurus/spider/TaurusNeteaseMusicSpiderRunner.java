package me.renhai.taurus.spider;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.renhai.taurus.spider.netease.NeteaseMusicProcessor;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.scheduler.PriorityScheduler;

//@Component
public class TaurusNeteaseMusicSpiderRunner implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		
		Spider spider = Spider.create(new NeteaseMusicProcessor())
		   .setScheduler(new PriorityScheduler())
		   .addUrl(genUrls(10001))
		   .addPipeline(new JsonFilePipeline("/Users/andy/Downloads"))
		   .thread(10);
		
		 spider.start();		
		
//		Spider.create(new NeteaseMusicProcessor()).test("http://music.163.com/album?id=112468");
//		Spider.create(new NeteaseMusicProcessor()).test("http://music.163.com/artist/desc?id=2111");
//		Spider.create(new NeteaseMusicProcessor()).test("http://music.163.com/artist/album?id=29323&limit=1000&offset=0");

	}
	
	private String[] genUrls(int start) {
		String[] urls = new String[10000];
		for (int i = 0; i < urls.length; i++) {
			urls[i] = "http://music.163.com/artist/album?id="+(i + start)+"&limit=1000&offset=0";
		}
		return urls;
	}

}
