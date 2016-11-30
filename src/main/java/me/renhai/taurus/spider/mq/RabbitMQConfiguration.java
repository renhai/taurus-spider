package me.renhai.taurus.spider.mq;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitMQConfiguration {
	
	@Value("${rabbitmq.taurus.queue}")
	private String queueName;
	@Value("${rabbitmq.taurus.exchange}")
	private String exchangeName;
	@Value("${rabbitmq.taurus.routingkey}")
	private String routingKey;
	
	@Bean
	RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}
	
	@Bean
	RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setExchange(exchangeName);
		rabbitTemplate.setRoutingKey(routingKey);
//		rabbitTemplate.setQueue(queueName);
		return rabbitTemplate;
	}
	
}
