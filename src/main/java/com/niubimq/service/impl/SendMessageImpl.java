package com.niubimq.service.impl;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.niubimq.cache.RetryCache;
import com.niubimq.client.RabbitFactory;
import com.niubimq.pojo.DetailRes;
import com.niubimq.service.MessageBaseService;
import com.niubimq.service.SendMessage;

/**
 * Created by junjin4838 on 17/3/17.
 */
@Service
public class SendMessageImpl extends MessageBaseService implements SendMessage{
   
	 @Autowired
	 private ConnectionFactory connectionFactory;

	/**
	 * 发送消息
	 * Step1: 构造template, exchange, routingkey等
     * Step2: 设置message序列化方法
     * Step3: 设置发送确认
     * Step4: 发送消息
	 */
	@Override
	public DetailRes send(String exchange, String routingKey, String queue,String type, Object message) throws IOException, TimeoutException {
		
		RetryCache retryCache = new RetryCache();
		
		Connection connection = connectionFactory.createConnection();
		
		//Step1
		super.buildQueue(exchange, routingKey, queue, connection, type);
		
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		
		//Step2
		rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
		
		//Step3
		RabbitFactory.createRabbitTemplate(rabbitTemplate,exchange,routingKey,retryCache);
		
		try {
		  //Step4	
		  String id = retryCache.gengeateId();
		  retryCache.add(id, message);
		  rabbitTemplate.correlationConvertAndSend(message, new CorrelationData(id));
		}catch(RuntimeException e){
			return new DetailRes(false, "");
		}
		
		return new DetailRes(true, "");
	}
}
