<template>
  <view class="login-page apple-page motion-page-focus">
    <scroll-view class="login-scroll" scroll-y :show-scrollbar="false" :scroll-into-view="scrollTarget" scroll-with-animation>
      <view class="brand-block motion-enter">
        <app-brand size="large" />
        <text class="screen-title">{{ adminMode ? '平台管理员登录' : registerMode ? '创建账户' : '欢迎回来' }}</text>
        <text class="brand-copy">{{ adminMode ? '使用已授权的管理员账号，处理设备、内容审核与账户权限' : registerMode ? '创建你的健康与急救账户' : '登录后继续使用健康与急救服务' }}</text>
      </view>

      <view v-if="!registerMode" class="portal-switch">
        <button class="portal-option" :class="{ selected: !adminMode }" :disabled="submitting" @tap="selectPortal(false)">用户登录</button>
        <button class="portal-option" :class="{ selected: adminMode }" :disabled="submitting" @tap="selectPortal(true)">平台管理员登录</button>
      </view>

      <view id="login-fields" class="auth-surface motion-enter motion-enter-delay-1">
        <view v-if="registerMode" class="field-row">
          <text class="field-label">称呼</text>
          <input v-model="nickname" class="field-input" placeholder="请输入称呼" maxlength="60" />
        </view>
        <view class="field-row">
          <text class="field-label">手机号</text>
          <input v-model="phone" class="field-input" type="number" :placeholder="adminMode ? '请输入管理员手机号' : '请输入手机号'" maxlength="11" />
        </view>
        <view class="field-row field-row-last">
          <text class="field-label">密码</text>
          <input v-model="password" class="field-input" password :placeholder="adminMode ? '请输入管理员密码' : '8–72 位字符'" maxlength="72" @confirm="submit" />
        </view>
      </view>

      <view class="primary-action app-motion-control" :class="{ disabled: submitting }" @tap="submit">
        <text>{{ submitting ? '请稍候…' : adminMode ? '登录管理平台' : registerMode ? '创建账户' : '登录' }}</text>
      </view>
      <text v-if="adminMode" class="admin-hint">仅限已有管理员权限的账户使用</text>
      <view v-else class="mode-action" @tap="toggleMode">
        <text>{{ registerMode ? '已有账户？登录' : '还没有账户？注册' }}</text>
      </view>

      <view v-if="previewPassword && !registerMode && !adminMode" class="demo-accounts">
        <text class="demo-heading">演示账号 · 点击填入</text>
        <view class="demo-account-list">
          <button v-for="account in demoAccounts" :key="account.phone" class="demo-account" :data-phone="account.phone" :disabled="submitting" @tap="fillDemoAccount(account)">
            <app-icon-tile :name="account.icon" :tone="account.tone" />
            <view class="demo-account-copy"><text class="demo-account-role">{{ account.role }}</text><text class="demo-account-phone">{{ account.phone }}</text></view>
            <text class="demo-account-action">{{ phone === account.phone && password === previewPassword ? '已填入' : '填入' }}</text>
          </button>
        </view>
      </view>

      <text class="legal-copy">登录或注册即表示你已阅读并同意用户协议与隐私政策</text>
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppBrand from '@/components/AppBrand.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { loginPlatformAdmin, loginWithPassword, registerAccount } from '@/api/auth'
import { getCurrentProfile } from '@/api/user'
import { hasAccountSession } from '@/utils/authSession'
import { userFacingError } from '@/utils/presentation'

const phone = ref('')
const password = ref('')
const nickname = ref('')
const registerMode = ref(false)
const adminMode = ref(false)
const submitting = ref(false)
const scrollTarget = ref('')
const previewPassword = __MAIAN_PREVIEW_PASSWORD__
const demoAccounts = [
  { role: '普通用户', phone: '19900000001', icon: 'user-filled', tone: 'blue' as const },
  { role: '急救志愿者', phone: '19900000002', icon: 'volunteer', tone: 'green' as const },
  { role: '管理员', phone: '19900000003', icon: 'auth-filled', tone: 'slate' as const }
].filter(account => !__MAIAN_PUBLIC_DEMO__ || account.phone !== '19900000003')

async function fillDemoAccount(account: typeof demoAccounts[number]) {
  if (submitting.value || registerMode.value || !previewPassword) return
  phone.value = account.phone
  password.value = previewPassword
  uni.hideKeyboard()
  scrollTarget.value = ''
  await nextTick()
  scrollTarget.value = 'login-fields'
}

onLoad(async options => {
  adminMode.value = options?.portal === 'admin'
  if (hasAccountSession()) {
    if (!adminMode.value) {
      uni.switchTab({ url: '/pages/index/index' })
      return
    }
    try {
      const profile = await getCurrentProfile()
      if (profile.role === 'ADMIN') uni.switchTab({ url: '/pages/mine/index' })
    } catch {
      // Keep the login form available when the existing session cannot be verified.
    }
  }
})

function selectPortal(admin: boolean) {
  if (submitting.value || adminMode.value === admin) return
  adminMode.value = admin
  password.value = ''
}

function toggleMode() {
  if (submitting.value) return
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
    } else if (adminMode.value) {
      await loginPlatformAdmin(phone.value, password.value)
    } else {
      await loginWithPassword(phone.value, password.value)
    }
    uni.switchTab({ url: adminMode.value ? '/pages/mine/index' : '/pages/index/index' })
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
.portal-switch { display: flex; gap: 8rpx; margin: 0 28rpx 24rpx; padding: 8rpx; border-radius: 18rpx; background: #E5E5EA; }
.portal-option { flex: 1; margin: 0; padding: 18rpx 8rpx; border-radius: 14rpx; background: transparent; color: #636366; font-size: 25rpx; line-height: 1.4; }
.portal-option::after { border: 0; }
.portal-option.selected { background: #FFFFFF; color: #1C1C1E; font-weight: 650; box-shadow: 0 2rpx 8rpx rgba(0,0,0,.06); }
.admin-hint { display: block; margin: 22rpx 28rpx; color: #636366; text-align: center; font-size: 23rpx; }
.auth-surface { margin: 0 28rpx; padding-left: 24rpx; overflow: hidden; border: 1rpx solid rgba(60,60,67,.12); border-radius: 22rpx; background: #FFFFFF; }
.field-row { display: grid; min-height: 96rpx; grid-template-columns: 112rpx minmax(0,1fr); align-items: center; border-bottom: 1rpx solid rgba(60,60,67,.14); }
.field-row-last { border-bottom: 0; }
.field-label { font-size: 27rpx; font-weight: 600; }
.field-input { height: 96rpx; padding-right: 24rpx; background: transparent !important; color: #1C1C1E; font-size: 27rpx; }
.primary-action { display: flex; min-height: 92rpx; align-items: center; justify-content: center; margin: 28rpx 28rpx 0; border-radius: 18rpx; background: #007AFF; color: #FFFFFF; font-size: 29rpx; font-weight: 700; }
.primary-action:active { transform: scale(.985); opacity: .8; }
.primary-action.disabled { opacity: .55; }
.mode-action { padding: 22rpx; color: #007AFF; text-align: center; font-size: 25rpx; }
.demo-accounts { margin: 12rpx 28rpx 0; }
.demo-heading { display: block; margin: 0 24rpx 12rpx; color: #8E8E93; font-size: 23rpx; }
.demo-account-list { overflow: hidden; border-radius: 20rpx; background: #FFFFFF; }
.demo-account { position: relative; display: flex; align-items: center; gap: 20rpx; width: 100%; min-height: 104rpx; box-sizing: border-box; margin: 0; padding: 16rpx 24rpx; border: 0; border-radius: 0; background: transparent; text-align: left; font-family: inherit; line-height: 1.4; }
.demo-account::after { border: 0; }
.demo-account:not(:last-child)::before { content: ''; position: absolute; left: 100rpx; right: 0; bottom: 0; height: 1rpx; background: rgba(60,60,67,.14); }
.demo-account-copy { flex: 1; min-width: 0; }
.demo-account-role { display: block; color: #1C1C1E; font-size: 27rpx; }
.demo-account-phone { display: block; margin-top: 4rpx; color: #8E8E93; font-size: 23rpx; font-variant-numeric: tabular-nums; }
.demo-account-action { flex: none; color: #007AFF; font-size: 23rpx; }
.demo-account:active { background: #EDEDF2; }
.demo-account:focus-visible { outline: 2px solid #007AFF; outline-offset: -2px; }
.demo-account[disabled] { opacity: .5; }
.legal-copy { display: block; margin: 30rpx 54rpx 0; color: #8E8E93; text-align: center; font-size: 20rpx; line-height: 1.55; }
.bottom-safe { height: calc(42rpx + env(safe-area-inset-bottom)); }
</style>
