package me.renhai.taurus.spider.rottentomatoes;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

public class RottenTomatoesProcessor implements PageProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(RottenTomatoesProcessor.class);
	private static Configuration conf = Configuration.defaultConfiguration()
			.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL).addOptions(Option.SUPPRESS_EXCEPTIONS);

	private Site site = Site.me().setDomain("rottentomatoes.com").setRetryTimes(5).setTimeOut(10000).setSleepTime(2000)
			.setUserAgent(
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

	private static final String MOVIE_REG = ".*/m/[^/]+/?$";
	private static final String CELEBRITY_REG = ".*/celebrity/[^/]+/?$";
//	private static final String CELEBRITY_BIO_REG = ".*/celebrity/[^/]+/biography/?$";
	
	@Override
	public void process(Page page) {
		if (page.getUrl().get().matches(MOVIE_REG)) {
			tortureMoviePage(page);
		} else if (page.getUrl().get().matches(CELEBRITY_REG)) {
			tortureCelebrityPage(page);
		} 
//		else if (page.getUrl().get().matches(CELEBRITY_BIO_REG)) {
//			tortureCelebrityBioPage(page);
//		}
		else {
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
				index = StringUtils.indexOf(link, "?");
				if (index != -1) {
					link = StringUtils.substring(link, 0, index);
				}

				link = StringUtils.removeEnd(link, "/");
				if (link.matches(MOVIE_REG)) {
					page.addTargetRequest(new Request(link).setPriority(100));
				} else if (link.matches(CELEBRITY_REG)) {
					page.addTargetRequest(new Request(link).setPriority(99));
//					page.addTargetRequest(new Request(link + "/biography").setPriority(97));
				} else {
					page.addTargetRequest(new Request(link));
				}
			}
			// page.addTargetRequests(page.getHtml().links().regex(".*www\\.rottentomatoes\\.com/.+").all());
		}
	}

	@Override
	public Site getSite() {
		return site;
	}

	private void tortureCelebrityPage(Page page) {
		page.putField("link", page.getUrl().toString());
		page.putField("actorId", page.getHtml().$("meta[name=actorID]", "content").get());
		page.putField("birthday", page.getHtml().$("td:containsOwn(Birthday) + td > time", "datetime").get());
		page.putField("birthplace", page.getHtml().$("td:containsOwn(Birthplace) + td", "text").get());
		page.putField("image", page.getHtml().xpath("//img[@class='posterImage']/@src").get());
		page.putField("bio", page.getHtml().$("div.celeb_summary_bio", "innerHTML").get());
	}
	
//	private void tortureCelebrityBioPage(Page page) {
//		page.putField("link", page.getUrl().toString());
//		page.putField("bio", page.getHtml().xpath("//div[@id='bio_box']/section[1]/div/html()").get());
//	}

	public void tortureMoviePage(Page page) {
		String json = page.getHtml().xpath("//script[@id='jsonLdSchema']/html()").get();
		ReadContext ctx = JsonPath.parse(json, conf);
		page.putField("movieId", page.getHtml().$("meta[name=movieID]", "content").get());
		page.putField("title", ctx.read("$.name"));
		page.putField("director", ctx.read("$.director"));
		page.putField("author", ctx.read("$.author"));
		page.putField("genre", joinString(ctx, "$.genre"));
		page.putField("studio", ctx.read("$.productionCompany.name"));
		page.putField("year", ctx.read("$.datePublished"));
		page.putField("mpaaRating", ctx.read("$.contentRating"));
		page.putField("image", ctx.read("$.image"));

		page.putField("link", page.getUrl().toString());
		page.putField("movieSynopsis", page.getHtml().xpath("//div[@id='movieSynopsis']/text()").get());
		page.putField("inTheaters", page.getHtml().xpath("//ul[@class='info']")
				.$("div:containsOwn(In Theaters) + div > time", "datetime").get());
		page.putField("onDvd", page.getHtml().xpath("//ul[@class='info']")
				.$("div:containsOwn(On DVD) + div > time", "datetime").get());
		page.putField("runTime", page.getHtml().xpath("//ul[@class='info']")
				.$("div:containsOwn(Runtime) + div > time", "datetime").get());
		page.putField("timestamp", System.currentTimeMillis());
		processCast(page, ctx);
		processRating(page, ctx);

		List<String> actorLinks = ctx.read("$.actors[*].sameAs");
		List<String> directorLinks = ctx.read("$.director[*].sameAs");
		List<String> authorLinks = ctx.read("$.author[*].sameAs");
		List<String> celebrityLinks = new ArrayList<>();
		celebrityLinks.addAll(actorLinks);
		celebrityLinks.addAll(directorLinks);
		celebrityLinks.addAll(authorLinks);
		List<String> fullLinks = celebrityLinks.stream().filter(Objects::nonNull)
				.map(link -> "https://www.rottentomatoes.com" + link).collect(Collectors.toList());
		page.addTargetRequests(fullLinks, 99);
//		List<String> bioLinks = fullLinks.stream().map(link -> link + "/biography").collect(Collectors.toList());
//		page.addTargetRequests(bioLinks, 98);
	}

	private String joinString(ReadContext ctx, String path) {
		List<String> names = ctx.read(path);
		return StringUtils.join(names, ",");
	}

	private void processRating(Page page, ReadContext ctx) {
		JSONObject json = new JSONObject();
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		json.put("criticRatingValue", ctx.read("$.aggregateRating.ratingValue"));
		json.put("criticReviewsCounted", ctx.read("$.aggregateRating.reviewCount"));

		Selectable scoreStats = page.getHtml().xpath("//div[@id='scoreStats']");
		String criticFresh = scoreStats.$("span:containsOwn(Fresh:) + span", "text").get();
		if (StringUtils.isNotBlank(criticFresh)) {
			try {
				json.put("criticFresh", nf.parse(criticFresh).intValue());
			} catch (ParseException e) {
				LOG.error(e.getMessage());
			}
		}
		String criticRotten = scoreStats.$("span:containsOwn(Rotten:) + span", "text").get();
		if (StringUtils.isNotBlank(criticRotten)) {
			try {
				json.put("criticRotten", nf.parse(criticRotten).intValue());
			} catch (ParseException e) {
				LOG.error(e.getMessage());
			}
		}

		String criticAvg = scoreStats.$("div > div:contains(Average Rating:)", "text").get();
		criticAvg = StringUtils.removeStart(criticAvg, "Average Rating:");
		json.put("criticAverageRating", StringUtils.trimToNull(criticAvg));

		String audienceRate = page.getHtml()
				.$("div#scorePanel a[href*=audience_reviews] div[class*=meter-value] > span", "text").get();
		if (StringUtils.isNotBlank(audienceRate)) {
			json.put("audienceRatingValue",
					Integer.parseInt(StringUtils.removeEnd(StringUtils.trimToEmpty(audienceRate), "%")));
		}

		String audienceAvg = page.getHtml()
				.$("div[class*=audience-panel] div[class*=audience-info] > div:contains(Average Rating:)", "text")
				.get();
		audienceAvg = StringUtils.removeStart(audienceAvg, "Average Rating:");
		json.put("audienceAverageRating", StringUtils.trimToEmpty(audienceAvg));

		String audienceUserRating = page.getHtml()
				.$("div[class*=audience-panel] div[class*=audience-info] > div:contains(User Ratings:)", "text").get();
		if (StringUtils.isNotBlank(audienceUserRating)) {
			audienceUserRating = StringUtils.trimToEmpty(StringUtils.removeStart(audienceUserRating, "User Ratings:"));
			try {
				json.put("audienceRatingCount", nf.parse(audienceUserRating).intValue());
			} catch (ParseException e) {
				LOG.error(e.getMessage());
			}
		}
		json.put("criticsConsensus",
				page.getHtml()
						.xpath("//div[@id='all-critics-numbers']//p[@class='critic_consensus superPageFontColor']/allText()")
						.get());
		page.putField("rating", json);
	}

	private void processCast(Page page, ReadContext ctx) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> actors = ctx.read("$.actors", List.class);
		List<String> characters = ctx.read("$.character");
		HtmlNode castSection = (HtmlNode) page.getHtml().xpath("//div[@class='castSection']");
		for (int i = 0; i < actors.size(); i++) {
			Map<String, Object> actor = actors.get(i);
			if (characters.size() == actors.size()) {
				actor.put("characters", characters.get(i));
			} else {
//				actor.put("characters",
//						castSection.$("a:contains(" + (String) actor.get("name") + ") + span", "title").get());
				actor.put("characters",
						castSection.$("a[href$=" + StringUtils.trim((String)actor.get("sameAs")) + "] + span", "title").get());
			}
		}
		page.putField("cast", actors);
	}

}
