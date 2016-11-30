package me.renhai.taurus.spider.rottentomatoes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSON;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;


public class TaurusPipline implements Pipeline {
	private static final Logger LOG = LoggerFactory.getLogger(TaurusPipline.class);

	private RabbitTemplate rabbitTemplate;
	
	public TaurusPipline(ApplicationContext ctx) {
		rabbitTemplate = ctx.getBean(RabbitTemplate.class);
	}

    @Override
    public void process(ResultItems resultItems, Task task) {
        try {
        	rabbitTemplate.convertAndSend(JSON.toJSON(resultItems.getAll()));
        	LOG.info("rabbit convertAndSend: " + resultItems.get("link"));
        } catch (Exception e) {
            LOG.error("rabbit convertAndSend error.", e);
        }
    }
}