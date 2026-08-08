<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

defineOptions({ name: 'MaianApp' })

const PRIVACY_CONSENT_KEY = 'maian:privacy-consent:v1'

onLaunch(() => {
  if (uni.getStorageSync(PRIVACY_CONSENT_KEY)) return
  setTimeout(() => {
    uni.showModal({
      title: '隐私保护提示',
      content: '为提供急救定位、穿戴设备连接、健康档案和图片上传功能，应用会在使用对应功能时申请必要权限。请阅读隐私政策与医疗免责声明。',
      confirmText: '同意并继续',
      cancelText: '查看详情',
      confirmColor: '#2E6DD1',
      success: result => {
        if (result.confirm) {
          uni.setStorageSync(PRIVACY_CONSENT_KEY, new Date().toISOString())
          return
        }
        uni.navigateTo({ url: '/pages/legal/index?type=privacy' })
      }
    })
  }, 350)
})
</script>

<style lang="scss">
@use '@/styles/motion.scss';
@use '@/styles/apple.scss';

page {
  --network-canvas: #F3F7FA;
  --network-paper: #FFFFFF;
  --network-ink: #20364D;
  --network-muted: #728296;
  --network-faint: #9AA8B7;
  --network-line: #E1E9F0;
  --network-action: #2E6DD1;
  --network-action-soft: #EAF2FC;
  --network-rescue: #C93D46;
  --network-rescue-soft: #F9EAEC;
  --network-online: #23956A;
  --network-online-soft: #E9F5F0;
  --network-warning: #C98327;
  --network-radius-section: 18rpx;
  --network-radius-control: 12rpx;
  --network-space-page: 28rpx;
  --network-space-section: 32rpx;
  --network-shadow-float: 0 8rpx 28rpx rgba(34, 61, 88, 0.1);
  background-color: $bg-color;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: $text-primary;
  line-height: 1.6;
}

/* #ifdef H5 */
html,
body,
uni-app {
  width: 100%;
  min-height: 100%;
  scrollbar-width: none;
}

uni-page-body,
uni-page-wrapper {
  width: 100%;
  min-height: 0;
  scrollbar-width: none;
}

html::-webkit-scrollbar,
body::-webkit-scrollbar,
uni-app::-webkit-scrollbar,
uni-page-body::-webkit-scrollbar,
uni-page-wrapper::-webkit-scrollbar,
uni-scroll-view::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

body { margin: 0; background: #F3F7FA; }
uni-app { display: block; min-height: 100vh; overflow: visible; background: #F3F7FA; }

/* Desktop H5 is a phone preview, while mobile browsers remain full width.
   The document keeps ownership of scrolling, so this does not recreate the
   old nested-scroll shell. */
@media (min-width: 768px) {
  html {
    background: #DDE3E9;
  }

  body,
  uni-app,
  uni-page {
    width: 430px;
    max-width: 430px;
    margin-right: auto;
    margin-left: auto;
  }

  body {
    min-height: 100vh;
    box-shadow: 0 0 0 1px rgba(32, 54, 77, .08), 0 18px 60px rgba(32, 54, 77, .14);
  }

  uni-app {
    position: relative;
  }

  uni-tabbar,
  uni-tabbar .uni-tabbar {
    right: calc(50vw - 215px) !important;
    left: calc(50vw - 215px) !important;
    width: 430px !important;
  }

  uni-page-head,
  .uni-page-head {
    right: calc(50vw - 215px) !important;
    left: calc(50vw - 215px) !important;
    width: 430px !important;
  }

  /* Transforming a page root makes fixed descendants page-relative. Keep the
     desktop phone preview fixed, but retain a transform-free entrance. */
  .motion-page-focus,
  .motion-page-sheet,
  .motion-page-list {
    transform: none !important;
    animation-name: pageDesktopReveal !important;
  }

  .motion-page-focus { animation-duration: 340ms !important; }
  .motion-page-sheet { animation-duration: 400ms !important; }
  .motion-page-list { animation-duration: 280ms !important; }

  :is(
    .nav-bar,
    .matching-layer,
    .top-overlay,
    .map-container,
    .map-fallback,
    .bottom-drawer,
    .success-overlay,
    .popup-mask,
    .location-picker-overlay
  ) {
    right: calc(50vw - 215px) !important;
    left: calc(50vw - 215px) !important;
    width: auto !important;
  }

  :is(uni-toast, uni-modal, uni-actionsheet, uni-popup) {
    right: calc(50vw - 215px) !important;
    left: calc(50vw - 215px) !important;
    width: 430px !important;
  }

  .device-popup {
    right: calc(50vw - 203px) !important;
    left: calc(50vw - 203px) !important;
  }

  .locate-btn {
    right: calc(50vw - 203px) !important;
  }

  .fab-btn {
    right: calc(50vw - 199px) !important;
  }

  .add-btn {
    right: calc(50vw - 178px) !important;
    left: calc(50vw - 178px) !important;
  }
}
/* #endif */
</style>
