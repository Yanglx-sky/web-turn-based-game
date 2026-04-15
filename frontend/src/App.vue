<template>
  <div id="app">
    <!-- 主内容 -->
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
// App.vue 现在只作为路由容器，不需要额外的逻辑
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: Arial, sans-serif;
  line-height: 1.6;
  color: #333;
  background-color: #f9f9f9;
}

#app {
  min-height: 100vh;
}

main {
  min-height: 100vh;
}
</style>