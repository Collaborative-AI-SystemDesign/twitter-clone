package com.example.demo.rabbitmq;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Queue 로 메세지를 발행할 때에는 RabbitTemplate 의 ConvertAndSend 메소드를 사용하고
 * Queue 에서 메세지를 구독할때는 @RabbitListener 을 사용
 *
 **/
@Slf4j
@RequiredArgsConstructor
@Service
public class RabbitMqService {

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    /**
     * 1. Queue 로 메세지를 발행
     * 2. Producer 역할 -> Direct Exchange 전략
     * 3. RabbitTemplate 의 convertAndSend 메소드를 사용하여 메세지를 발행
     **/
    public void sendMessage(Object messageDto) {
        log.info("messagge send: {}",messageDto.toString());
        this.rabbitTemplate.convertAndSend(exchangeName,routingKey,messageDto);
    }

    /**
     * 1. Queue 에서 메세지를 받도록 함.
     * 2. 임의로 messageDto를 Object 타입으로 받았지만, 실제로는 DTO 클래스를 사용하여 타입을 지정.
     **/
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void receiveMessage(Object messageDto) {
        log.info("Received Message : {}",messageDto.toString());
    }
}
