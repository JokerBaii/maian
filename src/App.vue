<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
onLaunch(() => {
  console.log("App Launch");
});
onShow(() => {
  console.log("App Show");
});
onHide(() => {
  console.log("App Hide");
});
</script>

<style lang="scss">
page {
  background-color: $bg-color;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: $text-primary;
  line-height: 1.6;
}

/* 全局工具类 */
.container {
  padding: 24rpx;
}

.card {
  background: $card-bg;
  border-radius: $radius-lg;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $shadow-sm;
}

.flex-row {
  display: flex;
  align-items: center;
}

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.flex-col {
  display: flex;
  flex-direction: column;
}

.text-primary { color: $primary-color; }
.text-danger { color: $danger-color; }
.text-warning { color: $warning-color; }
.text-success { color: $success-color; }
.text-secondary { color: $text-secondary; }

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 24rpx;
}

/* 状态标签 */
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.available, &.online, &.normal {
    background: rgba(0, 180, 42, 0.1);
    color: $success-color;
  }
  &.maintenance, &.offline, &.warning {
    background: rgba(255, 125, 0, 0.1);
    color: $warning-color;
  }
  &.expired, &.danger {
    background: rgba(245, 63, 63, 0.1);
    color: $danger-color;
  }
}

/* 按钮样式 */
.btn-primary {
  background: $primary-color;
  color: #fff;
  border-radius: $radius-lg;
  padding: 20rpx 40rpx;
  font-size: 30rpx;
  font-weight: 500;
  text-align: center;
  border: none;

  &::after { border: none; }
}

.btn-danger {
  background: $danger-color;
  color: #fff;
  border-radius: $radius-lg;
  padding: 20rpx 40rpx;
  font-size: 30rpx;
  font-weight: 500;
  text-align: center;
  border: none;

  &::after { border: none; }
}

.btn-outline {
  background: transparent;
  color: $primary-color;
  border: 2rpx solid $primary-color;
  border-radius: $radius-lg;
  padding: 18rpx 38rpx;
  font-size: 30rpx;
  font-weight: 500;
  text-align: center;

  &::after { border: none; }
}
</style>
