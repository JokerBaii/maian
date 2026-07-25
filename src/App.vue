<script setup lang="ts">
defineOptions({ name: 'MaianApp' })
</script>

<style lang="scss">
page {
  background-color: $bg-color;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: $text-primary;
  line-height: 1.6;
}

/* #ifdef H5 */
@media (min-width: 540px) {
  html {
    font-size: 20px !important;
    background: #E7EDF4;
  }

  body {
    background: #E7EDF4;
  }

  uni-app {
    position: relative;
    display: block;
    width: 480px;
    max-width: 100%;
    min-height: 100vh;
    margin: 0 auto;
    overflow: hidden;
    background: #F3F6F9;
    box-shadow: 0 0 0 1px rgba(36, 58, 82, 0.05);
    transform: translateZ(0);
  }
}
/* #endif */
</style>
