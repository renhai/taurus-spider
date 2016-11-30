package me.renhai.taurus.spider.rottentomatoes;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

public class RottenTomatoesProcessor implements PageProcessor {

	private Site site = Site.me().setDomain("rottentomatoes.com").setRetryTimes(5).setTimeOut(10000).setSleepTime(2000)
			.setUserAgent(
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

	@Override
	public void process(Page page) {
		if (page.getUrl().get().matches(".*/m/[^/]+/?$")) {
			RottenTomatoesTorturer.torturePage(page);
		} else {
			page.setSkip(true);
		}
		if (page.getStatusCode() == HttpStatus.OK.value()) {
			List<String> links = page.getHtml().links().all();
			for (String link : links) {
				if (!StringUtils.contains(link, "www.rottentomatoes.com/")) {
					continue;
				}
				if (StringUtils.contains(link, "www.rottentomatoes.com/tv/")) {
					continue;
				}
				int index = StringUtils.indexOf(link, "#");
				if (index != -1) {
					link = StringUtils.substring(link, 0, index);
				}
				link = StringUtils.removeEnd(link, "/");
				page.addTargetRequest(link);
			}
			// page.addTargetRequests(page.getHtml().links().regex(".*www\\.rottentomatoes\\.com/.+").all());
		}
	}

	@Override
	public Site getSite() {
		return site;
	}

	// public static void main(String[] args) {
	// Spider.create(new RottenTomatoesProcessor())
	// .addPipeline(new JsonFilePipeline("/Users/andy/Downloads"))
	// .addUrl("https://www.rottentomatoes.com/")
	// .thread(5)
	// .run();
	// }

}
