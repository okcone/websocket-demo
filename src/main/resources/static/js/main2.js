'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var disconForm = document.querySelector('#disconForm');

var stompClient = null;
var username = null;
var receiverName = null;
var sameUserName = null;
var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];
var socket;
function connect(event) {
    var thisUsername = document.querySelector('#name').value.trim();

    username = thisUsername.substring(thisUsername.lastIndexOf("@") + 1,thisUsername.length);
    receiverName = thisUsername.substring(0,thisUsername.lastIndexOf("@"));

    if (username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        createWebSocket();
    }
    event.preventDefault();
}

function disConnect() {
    stompClient.disconnect(function () {
        console.log('断开连接');
    }, getHeaders());
    socket.close();

    function getHeaders() {
        return {
            'X-Requested-With': 'X-Requested-With',
            'Authorization': localStorage.token
        }
    }
    reconnect();
}

var isConnected = false;
function onConnected() {
    isConnected = true;
    if(receiverName == null){
        receiverName = 'okc';
    }
    heartCheck.start();
    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/' + username, onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.addUser",
        {},
        JSON.stringify({sender: username, receiver:receiverName,type: 'JOIN'})
    )

    connectingElement.classList.add('hidden');
}


function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';

    console.log("error");
    reconnect();
}


function sendMessage(event) {
    var messageContent = messageInput.value.trim();
/**
    var receiver = '';
    if(messageContent.indexOf('@') > 0){
        receiver = messageContent.substring(messageContent.lastIndexOf("@") + 1,messageContent.length);
        messageContent = messageContent.substring(0,messageContent.lastIndexOf("@"));
    }*/


    if (messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageContent,
            receiver: receiverName,
            type: 'CHAT'
        };

        stompClient.send("/app/chat.sendOneMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}


function onMessageReceived(payload) {
    heartCheck.start();

    var message = JSON.parse(payload.body);

    if (message.type === 'JOIN' && isConnected) {
        isConnected = false;
        document.getElementById("userName").innerHTML = message.sender;

        var myBeifen = JSON.parse(message.beifen);
        if(myBeifen !== null){
            for(var k in myBeifen){
                var messageElement = document.createElement('li');
                aa(messageElement,myBeifen[k]);
                var textElement = document.createElement('p');
                var messageText = document.createTextNode(myBeifen[k].content);
                textElement.appendChild(messageText);

                messageElement.appendChild(textElement);
                messageArea.appendChild(messageElement);
                messageArea.scrollTop = messageArea.scrollHeight;
            }

        }
    }
    if(message.type === 'JOIN' && sameUserName == message.sender){
        return;
    }
    sameUserName = message.sender;

    var messageElement = document.createElement('li');
    if(message.content == 'hi'){
        return
    }
    if (message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined!';

    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!';
    } else {
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        var avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        var usernameElement = document.createElement('span');
        var usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    var textElement = document.createElement('p');
    var messageText = document.createTextNode(message.content);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;

}

function aa(messageElement,Mymessage) {
    messageElement.classList.add('chat-message');

    var avatarElement = document.createElement('i');
    var avatarText = document.createTextNode(Mymessage.sender[0]);
    avatarElement.appendChild(avatarText);
    avatarElement.style['background-color'] = getAvatarColor(Mymessage.sender);

    messageElement.appendChild(avatarElement);

    var usernameElement = document.createElement('span');
    var usernameText = document.createTextNode(Mymessage.sender);
    usernameElement.appendChild(usernameText);
    messageElement.appendChild(usernameElement);
}

function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    var index = Math.abs(hash % colors.length);
    return colors[index];
}


//心跳检测
var heartCheck = {
    timeout: 30000,
    timeoutObj: null,
    serverTimeoutObj: null,
    start: function(){
        console.log('start');
        var self = this;
        this.timeoutObj && clearTimeout(this.timeoutObj);
        this.serverTimeoutObj && clearTimeout(this.serverTimeoutObj);
        this.timeoutObj = setTimeout(function(){
            var chatMessage2 = {
                sender: username,
                content: 'hi',
                receiver: '',
                type: 'CHAT'
            };
            stompClient.send("/app/chat.sendOneMessage", {}, JSON.stringify(chatMessage2));
            if(!stompClient.connected){
                setTimeout(function() {
                    console.log(111);
                    disConnect();
                    // createWebSocket();
                }, self.timeout);
            }



        }, this.timeout)
    }
}
var lockReconnect = false;//避免重复连接
var tt;
function reconnect() {
    if(lockReconnect) {
        return;
    };
    lockReconnect = true;
    //没连接上会一直重连，设置延迟避免请求过多
    tt && clearTimeout(tt);
    tt = setTimeout(function () {
        createWebSocket();
        lockReconnect = false;
    }, 4000);
}

function createWebSocket() {
    socket = new SockJS('http://10.0.8.62:8084/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onError);
}

usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
disconForm.addEventListener('submit', disConnect, true);
