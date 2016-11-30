package me.renhai.taurus.spider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import me.renhai.taurus.spider.rottentomatoes.RottenTomatoesProcessor;
import me.renhai.taurus.spider.rottentomatoes.TaurusPipline;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;

@Component
public class TaurusSpiderRunner implements CommandLineRunner {
	
	@Autowired
	private ApplicationContext context;
	
	@Override
	public void run(String... args) throws Exception {
		String path = "/Users/andy/Downloads";
		Spider.create(new RottenTomatoesProcessor())
		   .addUrl("https://www.rottentomatoes.com/")
		   .addPipeline(new JsonFilePipeline(path))
		   .addPipeline(new TaurusPipline(context))
		   .thread(5)
		   .start();
		
	}
	


}
