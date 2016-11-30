package me.renhai.taurus.spider.rottentomatoes;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

public class RottenTomatoesTorturer {
	private static Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL).addOptions(Option.SUPPRESS_EXCEPTIONS);
	private static final Logger LOG = LoggerFactory.getLogger(RottenTomatoesTorturer.class);

	public static void torturePage(Page page) {
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
		page.putField("inTheaters", page.getHtml().xpath("//div[@class='info']").$("div:containsOwn(In Theaters) + div > time", "datetime").get());
		page.putField("onDvd", page.getHtml().xpath("//div[@class='info']").$("div:containsOwn(On DVD) + div > time", "datetime").get());
		page.putField("runTime", page.getHtml().xpath("//div[@class='info']").$("div:containsOwn(Runtime) + div > time", "datetime").get());
		page.putField("timestamp", System.currentTimeMillis());
		processCast(page, ctx);
		processRating(page, ctx);	
	}
	
	private static String joinString(ReadContext ctx, String path) {
		List<String> names = ctx.read(path);
		return StringUtils.join(names, ",");
	}
	
	private static void processRating(Page page, ReadContext ctx) {
//		RTRating res = new RTRating();
		JSONObject json = new JSONObject();
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
//		res.setCriticRatingValue(ctx.read("$.aggregateRating.ratingValue"));
//		res.setCriticReviewsCounted(ctx.read("$.aggregateRating.reviewCount"));
		json.put("criticRatingValue", ctx.read("$.aggregateRating.ratingValue"));
		json.put("criticReviewsCounted", ctx.read("$.aggregateRating.reviewCount"));
		
		Selectable scoreStats = page.getHtml().xpath("//div[@id='scoreStats']");
		String criticFresh = scoreStats.$("span:containsOwn(Fresh:) + span", "text").get();
		if (StringUtils.isNotBlank(criticFresh)) {
			try {
//				res.setCriticFresh(nf.parse(criticFresh).intValue());
				json.put("criticFresh", nf.parse(criticFresh).intValue());
			} catch (ParseException e) {
				LOG.error(e.getMessage());
			}
		}
		String criticRotten = scoreStats.$("span:containsOwn(Rotten:) + span", "text").get();
		if (StringUtils.isNotBlank(criticRotten)) {
			try {
//				res.setCriticRotten(nf.parse(criticRotten).intValue());
				json.put("criticRotten", nf.parse(criticRotten).intValue());
			} catch (ParseException e) {
				LOG.error(e.getMessage());
			}
		}

		String criticAvg = scoreStats.$("div > div:contains(Average Rating:)", "text").get();
		criticAvg = StringUtils.removeStart(criticAvg, "Average Rating:");
//		res.setCriticAverageRating(StringUtils.trimToNull(criticAvg));
		json.put("criticAverageRating", StringUtils.trimToNull(criticAvg));
		
		String audienceRate = page.getHtml().$("div#scorePanel a[href*=audience_reviews] div[class*=meter-value] > span", "text").get();
		if (StringUtils.isNotBlank(audienceRate)) {
//			res.setAudienceRatingValue(Integer.parseInt(StringUtils.removeEnd(StringUtils.trimToEmpty(audienceRate), "%")));
			json.put("audienceRatingValue", Integer.parseInt(StringUtils.removeEnd(StringUtils.trimToEmpty(audienceRate), "%")));
		}

		String audienceAvg = page.getHtml().$("div[class*=audiencepanel] div[class*=audience-info] > div:contains(Average Rating:)", "text").get();
		audienceAvg = StringUtils.removeStart(audienceAvg, "Average Rating:");
//		res.setAudienceAverageRating(StringUtils.trimToEmpty(audienceAvg));
		json.put("audienceAverageRating", StringUtils.trimToEmpty(audienceAvg));
		
		
		String audienceUserRating = page.getHtml().$("div[class*=audiencepanel] div[class*=audience-info] > div:contains(User Ratings:)", "text").get();
		if (StringUtils.isNotBlank(audienceUserRating)) {
			audienceUserRating = StringUtils.trimToEmpty(StringUtils.removeStart(audienceUserRating, "User Ratings:"));
			try {
//				res.setAudienceRatingCount(nf.parse(audienceUserRating).intValue());
				json.put("audienceRatingCount", nf.parse(audienceUserRating).intValue());
			} catch (ParseException e) {
				LOG.error(e.getMessage());
			} 
		}
//		res.setCriticsConsensus(page.getHtml().xpath("//div[@id='all-critics-numbers']//p[@class='critic_consensus superPageFontColor']/allText()").get());
		json.put("criticsConsensus", page.getHtml().xpath("//div[@id='all-critics-numbers']//p[@class='critic_consensus superPageFontColor']/allText()").get());
		page.putField("rating", json);
	}
	
	private static void processCast(Page page, ReadContext ctx) {
//		List<RTCast> res = new ArrayList<>();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> actors = ctx.read("$.actors", List.class);
		List<String> characters = ctx.read("$.character");
		HtmlNode castSection = (HtmlNode)page.getHtml().xpath("//div[@class='castSection']");
		for (int i = 0; i < actors.size(); i ++) {
//			RTCast cast = new RTCast();
			Map<String, Object> actor = actors.get(i);
//			cast.setName((String)actor.get("name"));
//			cast.setImage((String)actor.get("image"));
//			cast.setLink((String)actor.get("sameAs"));
//			cast.setType((String)actor.get("@type"));
			
			if (characters.size() == actors.size()) {
//				cast.setCharacters(characters.get(i));
				actor.put("characters", characters.get(i));
			} else {
//				cast.setCharacters(castSection.$("a:contains(" + cast.getName() + ") + span", "title").get());
				actor.put("characters", castSection.$("a:contains(" + (String)actor.get("name") + ") + span", "title").get());
			}
//			res.add(cast);
		}
		page.putField("cast", actors);
	}
}
