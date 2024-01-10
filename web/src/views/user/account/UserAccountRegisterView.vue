<template>
    <ContentField>
        <div class="row justify-content-md-center">
            <div class="col-3">
                <form>
                    <div class="mb-3">
                        <label for="username" class="form-label">用户名</label>
                        <input v-model="username" type="text" class="form-control" id="username" placeholder="请输入用户名">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">密码</label>
                        <input v-model="password" type="password" class="form-control" id="password" placeholder="请输入密码">
                    </div>
                    <div class="mb-3">
                        <label for="confirmedPassword" class="form-label">确认密码</label>
                        <input v-model="confirmedPassword" type="password" class="form-control" id="confirmedPassword"
                            placeholder="请再次输入密码">
                    </div>
                    <div class="mb-3">
                        <label for="mail" class="form-label">邮箱</label>
                        <input v-model="mail" type="text" class="form-control" id="mail" placeholder="请输入邮箱">
                    </div>

                    <div class="mb-3">
                        <label for="mailCode" class="form-label">邮箱验证码</label>
                        <input v-model="mailCode" type="text" class="form-control" id="mailCode" placeholder="请输入验证码">
                    </div>

                    <div class="mb-3">
                        <label for="headImg" class="form-label">头像</label>
                        <input v-model="headImg" type="text" class="form-control" id="headImg" placeholder="请输入头像链接">
                    </div>

                    <div class="error-message">{{ error_message }}</div>
                    <div class="mb-3">
                        <button type="button" class="btn btn-warning" @click="sendMailVerification">邮箱验证</button>
                    </div>
                    <div class="mb-3">
                        <button type="button" class="btn btn-primary" @click="register">注册</button>
                    </div>
                </form>
            </div>
        </div>
    </ContentField>
</template>

<script>
import $ from 'jquery'
import { ref } from 'vue'
import ContentField from '../../../components/ContentField.vue'
import router from '../../../router/index'
import Global from '@/global'
export default {
    components: {
        ContentField
    },
    setup() {
        let username = ref('');
        let password = ref('');
        let confirmedPassword = ref('');
        let headImg = ref('');
        let error_message = ref('');
        let mail = ref('')
        let mailCode = ref('')

        const register = () => {
            $.ajax({
                // url: "https://app2981.acapp.acwing.com.cn/api/user/account/register/",
                url: Global.apiUrl + "/api/user/account/register/",

                type: "post",
                data: {
                    username: username.value,
                    password: password.value,
                    headImg: headImg.value,
                    confirmedPassword: confirmedPassword.value,
                    mail: mail.value,
                    mailCode: mailCode.value,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        router.push({ name: "user_account_login" });
                    } else {
                        error_message.value = resp.error_message;
                    }
                },
            });
        }

        const sendMailVerification = () => {
            $.ajax({
                // url: "https://app2981.acapp.acwing.com.cn/api/user/account/register/",
                url: Global.apiUrl + "/api/user/account/sendMail/",

                type: "post",
                data: {
                    username: username.value,
                    password: password.value,
                    headImg: headImg.value,
                    confirmedPassword: confirmedPassword.value,
                    mail: mail.value,
                    mailCode: mailCode.value,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        error_message.value = "验证码已发送";
                    } else {
                        error_message.value = resp.error_message;
                    }
                },
            });
        }

        return {
            username,
            password,
            confirmedPassword,
            headImg,
            error_message,
            register,
            sendMailVerification,
            mail,
            mailCode
        }
    }
}
</script>

<style scoped>
button {
    width: 100%;
}

div.error-message {
    color: red;
}
</style>