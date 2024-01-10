<template>
    <ContentField>
        <table class="table table-striped table-hover" style="text-align: center;">
            <thead>
                <tr>
                    <th>玩家</th>
                    <th>天梯积分</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="user in users" :key="user.id">
                    <td>
                        <img :src="user.photo" alt="" class="record-user-photo">
                        &nbsp;
                        <span class="record-user-username">{{ user.username }}</span>
                    </td>
                    <td>
                        <span class="record-user-username">{{ user.rating }}</span>
                    </td>
                </tr>
            </tbody>
        </table>
        <nav aria-label="Page navigation example" style="float: right;">
            <ul class="pagination">
                <li class="page-item" @click="click_page(-2)"><a class="page-link" href="#">前一页</a></li>
                <li :class="'page-item ' + page.is_active" v-for="page in pages" :key="page.number"
                    @click="click_page(page.number)">
                    <a class="page-link" href="#">{{ page.number }}</a>
                </li>
                <li class="page-item" @click="click_page(-1)"><a class="page-link" href="#">后一页</a></li>
            </ul>
        </nav>
    </ContentField>
</template>

<script>
import $ from 'jquery';
import { ref } from 'vue';
import { useStore } from 'vuex';
import ContentField from '../../components/ContentField.vue';
import Global from '@/global';
export default {
    components: {
        ContentField
    },
    setup() {
        const store = useStore();
        let current_page = 1;
        let users = ref([]);
        let total_users = 0;
        let pages = ref([]);

        const click_page = page => {
            if (page == -2) page = current_page - 1;
            else if (page == -1) page = current_page + 1;
            let max_pages = parseInt(Math.ceil(total_users / 10));
            if (page >= 1 && page <= max_pages) {
                pull_page(page);
            }
        }

        const update_pages = () => {
            let max_pages = parseInt(Math.ceil(total_users / 10));
            let new_pages = [];
            for (let i = current_page - 2; i <= current_page + 2; i++) {
                if (i >= 1 && i <= max_pages) {
                    new_pages.push({
                        number: i,
                        is_active: i === current_page ? "active" : "",
                    });

                }
            }
            pages.value = new_pages;
        }

        const pull_page = page => {
            current_page = page;
            // console.log(current_page);
            $.ajax({
                // url: "https://app2981.acapp.acwing.com.cn/api/ranklist/getlist/",
                url: Global.apiUrl + "/api/ranklist/getlist/",

                data: {
                    page,
                },
                type: "get",
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    users.value = resp.users;
                    total_users = resp.users_count;
                    // console.log(total_records);

                    update_pages();
                },
                error(resp) {
                    console.log(resp);
                }
            })
        }
        pull_page(current_page);
        return {
            users,
            pages,
            click_page,
        }
    }
}
</script>

<style scoped>
img.record-user-photo {
    width: 5vh;
    border-radius: 50%;
}
</style>