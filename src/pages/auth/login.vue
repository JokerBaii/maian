<template>
  <view class="login-page apple-page motion-page-focus">
    <scroll-view class="login-scroll" scroll-y :show-scrollbar="false">
      <view class="brand-block motion-enter">
        <app-brand size="large" />
        <text class="screen-title">{{ registerMode ? '创建账户' : '欢迎回来' }}</text>
        <text class="brand-copy">{{ registerMode ? '创建你的健康与急救账户' : '登录后继续使用健康与急救服务' }}</text>
      </view>

      <view class="auth-surface motion-enter motion-enter-delay-1">
        <view v-if="registerMode" class="field-row">
          <text class="field-label">称呼</text>
          <input v-model="nickname" class="field-input" placeholder="请输入称呼" maxlength="60" />
        </view>
        <view class="field-row">
          <text class="field-label">手机号</text>
          <input v-model="phone" class="field-input" type="number" placeholder="请输入手机号" maxlength="11" />
        </view>
        <view class="field-row field-row-last">
          <text class="field-label">密码</text>
          <input v-model="password" class="field-input" password placeholder="8–72 位字符" maxlength="72" @confirm="submit" />
        </view>
      </view>

      <view class="primary-action app-motion-control" :class="{ disabled: submitting }" @tap="submit">
        <text>{{ submitting ? '请稍候…' : registerMode ? '创建账户' : '登录' }}</text>
      </view>
      <view class="mode-action" @tap="toggleMode">
        <text>{{ registerMode ? '已有账户？登录' : '还没有账户？注册' }}</text>
      </view>

      <text class="legal-copy">登录或注册即表示你已阅读并同意用户协议与隐私政策</text>
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppBrand from '@/components/AppBrand.vue'
import { loginWithPassword, registerAccount } from '@/api/auth'
import { hasAccountSession } from '@/utils/authSession'
import { userFacingError } from '@/utils/presentation'

const phone = ref('')
const password = ref('')
const nickname = ref('')
const registerMode = ref(false)
const submitting = ref(false)

onLoad(() => {
  if (hasAccountSession()) {
    uni.switchTab({ url: '/pages/index/index' })
  }
})

function toggleMode() {
  registerMode.value = !registerMode.value
  password.value = ''
}

function validate() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) return '请输入正确的手机号'
  if (password.value.length < 8) return '密码至少 8 位'
  if (registerMode.value && !nickname.value.trim()) return '请输入称呼'
  return ''
}

async function submit() {
  if (submitting.value) return
  const message = validate()
  if (message) {
    uni.showToast({ title: message, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    if (registerMode.value) {
      await registerAccount(phone.value, password.value, nickname.value.trim())
    } else {
      await loginWithPassword(phone.value, password.value)
    }
    uni.switchTab({ url: '/pages/index/index' })
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, registerMode.value ? '注册失败' : '登录失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

</script>

<style lang="scss" scoped>
.login-page { min-height: 100vh; background: #F2F2F7; color: #1C1C1E; }
.login-scroll { height: 100vh; box-sizing: border-box; }
.brand-block { padding: calc(70rpx + env(safe-area-inset-top)) 40rpx 38rpx; }
.screen-title { display: block; margin-top: 54rpx; font-size: 45rpx; font-weight: 740; letter-spacing: -1rpx; }
.brand-copy { display: block; max-width: 590rpx; margin-top: 12rpx; color: #636366; font-size: 27rpx; line-height: 1.5; }
.auth-surface { margin: 0 28rpx; padding-left: 24rpx; overflow: hidden; border: 1rpx solid rgba(60,60,67,.12); border-radius: 22rpx; background: #FFFFFF; }
.field-row { display: grid; min-height: 96rpx; grid-template-columns: 112rpx minmax(0,1fr); align-items: center; border-bottom: 1rpx solid rgba(60,60,67,.14); }
.field-row-last { border-bottom: 0; }
.field-label { font-size: 27rpx; font-weight: 600; }
.field-input { height: 96rpx; padding-right: 24rpx; background: transparent !important; color: #1C1C1E; font-size: 27rpx; }
.primary-action { display: flex; min-height: 92rpx; align-items: center; justify-content: center; margin: 28rpx 28rpx 0; border-radius: 18rpx; background: #007AFF; color: #FFFFFF; font-size: 29rpx; font-weight: 700; }
.primary-action:active { transform: scale(.985); opacity: .8; }
.primary-action.disabled { opacity: .55; }
.mode-action { padding: 22rpx; color: #007AFF; text-align: center; font-size: 25rpx; }
.legal-copy { display: block; margin: 30rpx 54rpx 0; color: #8E8E93; text-align: center; font-size: 20rpx; line-height: 1.55; }
.bottom-safe { height: calc(42rpx + env(safe-area-inset-bottom)); }
</style>
