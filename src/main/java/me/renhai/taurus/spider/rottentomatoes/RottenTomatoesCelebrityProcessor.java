package me.renhai.taurus.spider.rottentomatoes;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

public class RottenTomatoesCelebrityProcessor implements PageProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(RottenTomatoesCelebrityProcessor.class);

	private Site site = Site.me().setDomain("rottentomatoes.com").setRetryTimes(5).setTimeOut(10000).setSleepTime(2000)
			.setUserAgent(
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

	@Override
	public void process(Page page) {
		if (page.getUrl().get().matches(".*/celebrity/[^/]+/?$")) {
			page.putField("link", page.getUrl().get());
			page.putField("actorId", page.getHtml().$("meta[name=actorID]", "content").get());
			page.putField("birthday", page.getHtml().xpath("//td[@itemprop='birthDate']/text()").get());
			page.putField("birthplace", page.getHtml().xpath("//td[@itemprop='birthPlace']/text()").get());
		} else if (page.getUrl().get().matches(".*/celebrity/[^/]+/biography/?$")) {
			page.putField("link", page.getUrl().get());
			page.putField("biography", page.getHtml().xpath("//div[@id='bio_box']/section[1]/div/tidyText()").get());
		} else {
			page.setSkip(true);
		}
//		if (page.getStatusCode() == HttpStatus.OK.value()) {
//			List<String> links = page.getHtml().links().all();
//			for (String link : links) {
//				if (!StringUtils.contains(link, "www.rottentomatoes.com/")) {
//					continue;
//				}
//				if (StringUtils.contains(link, "www.rottentomatoes.com/tv/")) {
//					continue;
//				}
//				int index = StringUtils.indexOf(link, "#");
//				if (index != -1) {
//					link = StringUtils.substring(link, 0, index);
//				}
//				link = StringUtils.removeEnd(link, "/");
//				page.addTargetRequest(link);
//			}
//		}
		
	}

	@Override
	public Site getSite() {
		return site;
	}


}
