<template>
  <view class="page apple-page motion-page-list">
    <view class="heading">
      <text class="title">账户权限</text>
      <text class="subtitle">授权后，账户重新登录即可进入对应门户</text>
    </view>

    <view v-if="loading" class="state">正在加载账户…</view>
    <view v-else-if="!accounts.length" class="state">暂无已注册账户</view>
    <view v-else class="account-list">
      <view
        v-for="account in accounts"
        :key="account.id"
        class="account-row app-motion-control"
        @tap="selectAccount(account)"
      >
        <app-avatar :name="account.nickname" :size="42" />
        <view class="account-copy">
          <text class="account-name">{{ account.nickname }}</text>
          <text class="account-phone">{{ account.phone }}</text>
        </view>
        <text class="role-label">{{ roleLabel(account.role) }}</text>
        <text class="chevron">›</text>
      </view>
    </view>

    <app-action-sheet
      v-model:visible="sheetVisible"
      :title="selectedAccount?.nickname || ''"
      message="选择该账户可以使用的门户"
      :actions="roleActions"
      @select="changeRole"
    />
    <assistant-companion context="mine" :subject="{ accountId: selectedAccount?.id }" :hidden="sheetVisible" />
  </view>
</template>

<script setup lang="ts">
import { useAssistantRefresh } from '@/composables/useAssistantRefresh'
import AssistantCompanion from '@/components/AssistantCompanion.vue'
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppActionSheet, { type AppActionSheetItem } from '@/components/AppActionSheet.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { listAccounts, updateAccountRole, type AccountRole, type AdminUserResponse } from '@/api/admin'
import { userFacingError } from '@/utils/presentation'

const accounts = ref<AdminUserResponse[]>([])
const selectedAccount = ref<AdminUserResponse | null>(null)
const loading = ref(true)
const sheetVisible = ref(false)
const saving = ref(false)
const roleValues: AccountRole[] = ['USER', 'VOLUNTEER', 'ADMIN']
const roleActions: AppActionSheetItem[] = [
  { label: '普通用户' },
  { label: '急救志愿者' },
  { label: '平台审核员' }
]

function roleLabel(role: AccountRole) {
  return { USER: '普通用户', VOLUNTEER: '急救志愿者', ADMIN: '平台审核员' }[role]
}

async function load() {
  loading.value = true
  try {
    accounts.value = (await listAccounts()).content
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '账户加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function selectAccount(account: AdminUserResponse) {
  if (saving.value) return
  selectedAccount.value = account
  sheetVisible.value = true
}

async function changeRole(index: number) {
  const account = selectedAccount.value
  const role = roleValues[index]
  if (!account || !role || saving.value || account.role === role) return
  saving.value = true
  try {
    const updated = await updateAccountRole(account.id, role)
    accounts.value = accounts.value.map(item => item.id === updated.id ? updated : item)
    uni.showToast({ title: '权限已更新', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '权限更新失败'), icon: 'none' })
  } finally {
    saving.value = false
    selectedAccount.value = null
  }
}

onShow(load)
useAssistantRefresh(load)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; padding: 24rpx 0 48rpx; box-sizing: border-box; background: #F2F2F7; color: #1C1C1E; }
.heading { padding: 0 32rpx 22rpx; }
.title { display: block; font-size: 38rpx; font-weight: 750; letter-spacing: -.03em; }
.subtitle { display: block; margin-top: 7rpx; color: #8E8E93; font-size: 23rpx; line-height: 1.45; }
.state { margin: 0 24rpx; padding: 72rpx 24rpx; border-radius: 22rpx; background: #FFFFFF; color: #8E8E93; text-align: center; font-size: 24rpx; }
.account-list { margin: 0 24rpx; padding-left: 22rpx; overflow: hidden; border: 1rpx solid rgba(60,60,67,.12); border-radius: 22rpx; background: #FFFFFF; }
.account-row { display: flex; min-height: 112rpx; align-items: center; gap: 18rpx; padding-right: 20rpx; border-bottom: 1rpx solid rgba(60,60,67,.12); }
.account-row:last-child { border-bottom: 0; }
.account-row:active { background: rgba(118,118,128,.08); }
.account-copy { min-width: 0; flex: 1; }
.account-name, .account-phone { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-name { font-size: 28rpx; font-weight: 650; }
.account-phone { margin-top: 4rpx; color: #8E8E93; font-size: 22rpx; }
.role-label { color: #007AFF; font-size: 23rpx; }
.chevron { color: #C7C7CC; font-size: 34rpx; font-weight: 300; }
</style>
