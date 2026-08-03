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
    overflow-x: hidden;
    background: #F3F6F9;
    box-shadow: 0 0 0 1px rgba(36, 58, 82, 0.05);
    transform: translateZ(0);
  }

  uni-app.uni-app--showtabbar {
    height: 100vh;
    min-height: 0;
    overflow-y: hidden;
  }
}
/* #endif */
</style>
