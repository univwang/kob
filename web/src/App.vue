<template>
  <NavBar />
  <router-view></router-view>
</template>

<script>
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap";
import NavBar from './components/NavBar.vue';

const BASE_URL = "http://127.0.0.1:3000"
export default {
  BASE_URL,
  components: {
    NavBar
  },
  setup() {
  }
}
</script>

<style>
body {
  background-image: url("@/assets/images/background.png");
  background-size: cover;
}
</style>
