package cn.monitor4all.springbootwebsocketdemo.service;

import cn.monitor4all.springbootwebsocketdemo.model.ChatMessage;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void sendMsg(@Payload ChatMessage chatMessage) {
        LOGGER.info("Send msg by simpMessageSendingOperations:" + chatMessage.toString());
        simpMessageSendingOperations.convertAndSend("/topic/public", chatMessage);
    }

    public void alertUserStatus(@Payload ChatMessage chatMessage) {
        LOGGER.info("Alert user online by simpMessageSendingOperations:" + chatMessage.toString());
        Map redisMap = new LinkedHashMap();
        System.out.println(chatMessage.toString());
        if(chatMessage.getReceiver() != null && (chatMessage.getReceiver().equals("apple") || chatMessage.getReceiver().equals("okc"))){
            Object oo = redisTemplate.opsForHash().get(chatMessage.getReceiver(),chatMessage.getSender());
            if(oo != null){
                redisMap = JSON.parseObject(oo.toString(), LinkedHashMap.class);
            }
            simpMessageSendingOperations.convertAndSend("/topic/" + chatMessage.getReceiver(), chatMessage);
            chatMessage.setBeifen(JSON.toJSONString(redisMap));
        }
        simpMessageSendingOperations.convertAndSend("/topic/" + chatMessage.getSender(), chatMessage);
    }

    /**
     * 发送特定的人员
     * */
    public void sendOneMsg(@Payload ChatMessage chatMessage) {
        LOGGER.info("Send one msg by simpMessageSendingOperations:" + chatMessage.toString());
        simpMessageSendingOperations.convertAndSend("/topic/"+ chatMessage.getSender(), chatMessage);
        if(chatMessage.getReceiver() != null){
            simpMessageSendingOperations.convertAndSend("/topic/"+ chatMessage.getReceiver(), chatMessage);
        }
    }
}
