package cn.monitor4all.springbootwebsocketdemo.controller;

import cn.monitor4all.springbootwebsocketdemo.model.ChatMessage;
import cn.monitor4all.springbootwebsocketdemo.service.ChatService;
import cn.monitor4all.springbootwebsocketdemo.util.JsonUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;


@Controller
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    @Value("${redis.channel.msgToAll}")
    private String msgToAll;

    @Value("${redis.set.onlineUsers}")
    private String onlineUsers;

    @Value("${redis.channel.userStatus}")
    private String userStatus;

    @Value("${redis.channel.msgToOne}")
    private String msgToOne;

    @Autowired
    private ChatService chatService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        try {
            redisTemplate.convertAndSend(msgToAll, JsonUtil.parseObjToJson(chatMessage));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {

        LOGGER.info("User added in Chatroom:" + chatMessage.getSender());
        try {
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            if(!redisTemplate.opsForSet().isMember(onlineUsers, chatMessage.getSender())){
                redisTemplate.opsForSet().add(onlineUsers, chatMessage.getSender());
            }
            redisTemplate.convertAndSend(userStatus, JsonUtil.parseObjToJson(chatMessage));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * 特定发给发送者自己
     * */
    @MessageMapping("/chat.sendOneMessage")
    public void sendOneMessage(@Payload ChatMessage chatMessage) {
        try {
            if(chatMessage.getReceiver().equals("apple") || chatMessage.getReceiver().equals("okc")){
                Object oo = redisTemplate.opsForHash().get(chatMessage.getReceiver(),chatMessage.getSender());
                Map redisMap = new LinkedHashMap();
                if(oo != null){
                    redisMap = JSON.parseObject(oo.toString(), LinkedHashMap.class);
                }
                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                redisMap.put(format.format(date),chatMessage);
                redisTemplate.opsForHash().put(chatMessage.getReceiver(),chatMessage.getSender(), JSON.toJSONString(redisMap));
                redisTemplate.opsForHash().put(chatMessage.getSender(),chatMessage.getReceiver(), JSON.toJSONString(redisMap));

                chatMessage.setBeifen(JSON.toJSONString(redisMap));
            }
            redisTemplate.convertAndSend(msgToOne, JsonUtil.parseObjToJson(chatMessage));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws Exception{
        System.out.println(unescape("&#x7fc1;&#x5e86; TCL"));
    }

    //UNICODE转中文
    public static String unicodeToChina(String str) throws Exception{
        byte[] byteArr = str.getBytes("UTF-8");
        String chinese=new String(byteArr,"UTF-8");
        return chinese;
    }

    public static void aa(String unicode){
        StringBuffer string = new StringBuffer();
        if (unicode.startsWith("&#x")) {
            String[] hex = unicode.replace("&#x", "").split(";");for (int i = 0; i < hex.length; i++) {int data = Integer.parseInt(hex[i], 16);string.append((char) data); }

        }
    }
    public static String  unescape (String src){
        StringBuffer tmp = new StringBuffer();
        tmp.ensureCapacity(src.length());
        int  lastPos=0,pos=0;
        char ch;
        src = src.replace("&#x","%u").replace(";","");
        while (lastPos<src.length()){
            pos = src.indexOf("%",lastPos);
            if (pos == lastPos){
                if (src.charAt(pos+1)=='u'){
                    ch = (char)Integer.parseInt(src.substring(pos+2,pos+6),16);
                    tmp.append(ch);
                    lastPos = pos+6;
                }else{
                    ch = (char)Integer.parseInt(src.substring(pos+1,pos+3),16);
                    tmp.append(ch);
                    lastPos = pos+3;
                }
            } else{
                if (pos == -1){
                    tmp.append(src.substring(lastPos));
                    lastPos=src.length();
                } else{
                    tmp.append(src.substring(lastPos,pos));
                    lastPos=pos;
                }
            }
        }
        return tmp.toString();
    }

}

/**
 * websocket+webrtc实现视频通话
 * */