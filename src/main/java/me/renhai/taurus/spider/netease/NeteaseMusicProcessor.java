package me.renhai.taurus.spider.netease;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import me.renhai.taurus.spider.rottentomatoes.RottenTomatoesProcessor;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

public class NeteaseMusicProcessor implements PageProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(RottenTomatoesProcessor.class);

	
	private Site site = Site.me().setDomain("music.163.com").setRetryTimes(5).setTimeOut(10000).setSleepTime(2000)
			.setUserAgent(
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");


	@Override
	public void process(Page page) {
//		String intro = page.getHtml().xpath("//div[@class='n-artdesc']/html()").get();
//		LOG.info(intro);
		
		if (page.getStatusCode() == HttpStatus.OK.value()) {
			if (page.getUrl().get().contains(".com/artist/album?")) {
				List<String> allLinks = page.getHtml().xpath("//ul[@id='m-song-module']").links().all();
				page.addTargetRequests(allLinks, 100);
				page.setSkip(true);
			} else if(page.getUrl().get().contains(".com/album?")) {
				page.putField("link", page.getUrl().get());
				page.putField("songList", page.getHtml().xpath("//div[@id='song-list-pre-cache']//textarea/text()").get());
				page.putField("releaseDate", page.getHtml().$("p:contains(发行时间)", "text").get());
				page.putField("releaseCompany", page.getHtml().$("p:contains(发行公司)", "text").get());
				page.putField("singer", page.getHtml().$("p:contains(歌手) > a", "text").get());
				page.putField("singerLink", page.getHtml().$("p:contains(歌手) > a", "href").get());
			} else {
				page.setSkip(true);
			}
		}
		
		


	}

	@Override
	public Site getSite() {
		return site;
	}

}
