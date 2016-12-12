package me.renhai.taurus.spider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import me.renhai.taurus.spider.rottentomatoes.RottenTomatoesProcessor;
import me.renhai.taurus.spider.rottentomatoes.TaurusRabbitMQPipline;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;

@Component
public class TaurusMovieSpiderRunner implements CommandLineRunner {
	
	@Autowired
	private ApplicationContext context;
	
	@Value("${movie.json.data.store.path}")
	private String path;
	
	@Override
	public void run(String... args) throws Exception {
		Spider.create(new RottenTomatoesProcessor())
		   .addUrl("https://www.rottentomatoes.com/")
		   .addPipeline(new JsonFilePipeline(path))
		   .addPipeline(new TaurusRabbitMQPipline(context))
		   .thread(8)
		   .start();
		
	}

}
