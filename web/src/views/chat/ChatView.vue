<template>
  <ContentField>

    <div class="chat-box">
      <div class="chat-messages">
        <!-- 聊天记录列表 -->
        <div v-for="(message, index) in chatMessages" :key="index" class="message">
          <div class="message-content" :class="message.isMyMessage ? 'my-message' : 'other-message'">
            <div class="avatar">
              <img :src="message.avatar" alt="Avatar"/>
            </div>
            <div class="message-details">
              <div class="message-header">
                <span class="username">{{ message.username }}</span>
              </div>
              <div class="message-text">{{ message.text }}</div>
              <span class="time">{{ message.time }}</span>

            </div>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <input v-model="newMessage" @keyup.enter="sendMessage" placeholder="输入消息..."/>
        <button @click="sendMessage">发送</button>
      </div>
    </div>
  </ContentField>

</template>

<script>
import ContentField from "@/components/ContentField.vue"
import {useStore} from "vuex";
import {onMounted, onUnmounted, ref} from "vue";
import Global from "@/global";

export default {
  components: {
    ContentField,
  },
  setup() {
    const store = useStore();
    const chatMessages = ref([]);
    const newMessage = ref("");
    const socketUrl = Global.wsApiUrl + `/websocket/chat/${store.state.user.token}/`
    let socket = null;

    const scrollToBottom = () => {
      const chatMessagesElement = document.querySelector('.chat-messages');
      chatMessagesElement.scrollTop = chatMessagesElement.scrollHeight;
    };

    const sendMessage = () => {
      if (newMessage.value.trim() !== '') {
        socket.send(JSON.stringify({
          avatar: store.state.user.photo,
          username: store.state.user.username,
          time: "",
          text: newMessage.value,
          isMyMessage: false,
        }));
        newMessage.value = '';
      }
    };


    onMounted(() => {
      socket = new WebSocket(socketUrl);
      socket.onopen = () => {
        console.log("connected!");
      }

      socket.onmessage = msg => {
        const data = JSON.parse(msg.data);
        chatMessages.value.push(data);
        scrollToBottom();
      }

      socket.onclose = () => {
        console.log("disconnected!");
      }
    });

    onUnmounted(() => {
      socket.close();
    })

    return {
      chatMessages,
      newMessage,
      sendMessage,
      scrollToBottom
    };
  },
};
</script>

<style scoped>
.chat-box {
  //width: 300px;
  height: 700px;
  border: 1px solid #ccc;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex-grow: 1;
  overflow-y: scroll;
  padding: 10px;
}

.message {
  margin-bottom: 10px;
}

.message-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  max-width: 100%;
}

.my-message {
  justify-content: flex-end;
}

.my-message .message-text {
  background-color: mediumseagreen; /* 绿色背景 */
}

.other-message .message-text {
  background-color: lightgrey;;
}

.avatar {
  flex-shrink: 0; /* 防止头像被压缩 */
  width: 40px;
  height: 40px;
  margin-right: 10px;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
  }
}

.message-details {
  display: flex;
  flex-direction: column;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.username {
  font-weight: bold;
}

.time {
  right: 10px; /* 根据需要调整 */
  align-content: center;
  color: #777;
  font-size: 12px;
}

.message-text {
  background-color: #f0f0f0;
  padding: 10px;
  border-radius: 10px;
  max-width: 600px;
  word-wrap: break-word; /* 让文本内容多了就自动换行 */
  white-space: pre-wrap; /* 允许保留换行符 */
}

.chat-input {
  display: flex;
  padding: 10px;
  align-items: center;

  input {
    flex-grow: 1;
    padding: 5px;
    border: 1px solid #ccc;
    border-radius: 5px;
  }

  button {
    margin-left: 10px;
    background-color: #007bff;
    color: #fff;
    border: none;
    border-radius: 5px;
    padding: 5px 10px;
    cursor: pointer;
  }
}
</style>
