<script setup>
// 注意：不在 beforeunload 时清理数据，因为用户可能只是想刷新页面
// 如果需要清理数据，应该在用户明确退出时调用 userStore.clearAll()
</script>

<template>
  <!-- 与 ui-style-example 一致的背景浮动装饰 -->
  <div class="floating-shapes">
    <div class="float-shape" />
    <div class="float-shape" />
    <div class="float-shape" />
    <div class="float-shape" />
  </div>
  <RouterView />
</template>

<style scoped>

</style>
