<template>
  <view class="page">
    <view class="contact-list">
      <view
        v-for="(contact, idx) in contacts"
        :key="contact.id"
        class="contact-card"
      >
        <view class="contact-avatar">
          <text class="contact-avatar-text">{{ contact.name.charAt(0) }}</text>
        </view>
        <view class="contact-info">
          <view class="contact-name-row">
            <text class="contact-name">{{ contact.name }}</text>
            <view class="relation-tag">
              <text class="relation-tag-text">{{ contact.relation }}</text>
            </view>
          </view>
          <text class="contact-phone">{{ contact.phone }}</text>
        </view>
        <view class="contact-actions">
          <view class="action-btn action-edit" @tap="editContact(idx)">
            <text class="action-btn-text">编辑</text>
          </view>
          <view class="action-btn action-delete" @tap="deleteContact(idx)">
            <text class="action-btn-text action-delete-text">删除</text>
          </view>
        </view>
      </view>

      <view v-if="contacts.length === 0" class="empty-state">
        <app-icon class="empty-icon" name="phone" :size="44" color="#8994A8" />
        <text class="empty-text">暂无紧急联系人</text>
        <text class="empty-hint">添加联系人以便在紧急情况下快速通知</text>
      </view>
    </view>

    <view class="add-btn" @tap="showAddPopup">
      <text class="add-btn-icon">+</text>
      <text class="add-btn-text">添加联系人</text>
    </view>

    <view v-if="popupVisible" class="popup-mask" @tap="hidePopup">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">{{ isEditing ? '编辑联系人' : '添加联系人' }}</text>
          <view class="popup-close" @tap="hidePopup">
            <app-icon class="popup-close-text" name="closeempty" :size="20" color="#6E7A90" />
          </view>
        </view>
        <view class="popup-form">
          <view class="popup-form-item">
            <text class="popup-form-label">姓名</text>
            <input
              class="popup-form-input"
              v-model="popupForm.name"
              placeholder="请输入联系人姓名"
              placeholder-class="popup-form-placeholder"
            />
          </view>
          <view class="popup-form-divider"></view>
          <view class="popup-form-item">
            <text class="popup-form-label">电话</text>
            <input
              class="popup-form-input"
              v-model="popupForm.phone"
              placeholder="请输入联系电话"
              placeholder-class="popup-form-placeholder"
              type="number"
              maxlength="11"
            />
          </view>
          <view class="popup-form-divider"></view>
          <view class="popup-form-item">
            <text class="popup-form-label">关系</text>
            <view class="relation-selector">
              <view
                v-for="rel in relationOptions"
                :key="rel"
                class="relation-option"
                :class="{ 'relation-option-active': popupForm.relation === rel }"
                @tap="popupForm.relation = rel"
              >
                <text class="relation-option-text" :class="{ 'relation-option-text-active': popupForm.relation === rel }">{{ rel }}</text>
              </view>
            </view>
          </view>
        </view>
        <view class="popup-submit" @tap="handleSave">
          <text class="popup-submit-text">保存</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import {
  createEmergencyContact,
  deleteEmergencyContact,
  listEmergencyContacts,
  updateEmergencyContact,
  type EmergencyContactResponse
} from '@/api/user'

const contacts = ref<EmergencyContactResponse[]>([])

const popupVisible = ref(false)
const isEditing = ref(false)
const editingIndex = ref(-1)

const relationOptions = ['配偶', '父亲', '母亲', '子女', '兄弟', '姐妹', '朋友', '其他']

const popupForm = reactive({
  name: '',
  phone: '',
  relation: '配偶'
})

function showAddPopup() {
  isEditing.value = false
  editingIndex.value = -1
  popupForm.name = ''
  popupForm.phone = ''
  popupForm.relation = '配偶'
  popupVisible.value = true
}

function editContact(idx: number) {
  isEditing.value = true
  editingIndex.value = idx
  const c = contacts.value[idx]
  popupForm.name = c.name
  popupForm.phone = c.phone
  popupForm.relation = c.relation
  popupVisible.value = true
}

function deleteContact(idx: number) {
  const contact = contacts.value[idx]
  uni.showModal({
    title: '确认删除',
    content: `确定要删除联系人“${contact.name}”吗？`,
    confirmColor: '#F53F3F',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteEmergencyContact(contact.id)
          contacts.value.splice(idx, 1)
          uni.showToast({ title: '已删除', icon: 'success' })
        } catch {
          uni.showToast({ title: '删除失败，请重试', icon: 'none' })
        }
      }
    }
  })
}

function hidePopup() {
  popupVisible.value = false
}

async function handleSave() {
  if (!popupForm.name.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  if (!popupForm.phone.trim()) {
    uni.showToast({ title: '请输入电话', icon: 'none' })
    return
  }
  const payload = {
    name: popupForm.name.trim(),
    phone: popupForm.phone.trim(),
    relation: popupForm.relation
  }
  try {
    if (isEditing.value && editingIndex.value >= 0) {
      const current = contacts.value[editingIndex.value]
      contacts.value[editingIndex.value] = await updateEmergencyContact(current.id, payload)
      uni.showToast({ title: '修改成功', icon: 'success' })
    } else {
      contacts.value.push(await createEmergencyContact(payload))
      uni.showToast({ title: '添加成功', icon: 'success' })
    }
    hidePopup()
  } catch (error: any) {
    uni.showToast({ title: error?.message || '保存失败，请重试', icon: 'none' })
  }
}

async function loadContacts() {
  try {
    contacts.value = await listEmergencyContacts()
  } catch {
    uni.showToast({ title: '联系人加载失败', icon: 'none' })
  }
}

onShow(loadContacts)
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
  padding: 24rpx 32rpx;
  box-sizing: border-box;
  padding-bottom: 160rpx;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.contact-card {
  display: flex;
  align-items: center;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}
.contact-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.contact-avatar-text {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.contact-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.contact-name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.contact-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1D2129;
}
.relation-tag {
  background: rgba(43, 111, 240, 0.08);
  border-radius: 8rpx;
  padding: 2rpx 14rpx;
}
.relation-tag-text {
  font-size: 20rpx;
  color: #2B6FF0;
  font-weight: 500;
}
.contact-phone {
  font-size: 26rpx;
  color: #86909C;
}
.contact-actions {
  display: flex;
  gap: 12rpx;
  flex-shrink: 0;
  margin-left: 16rpx;
}
.action-btn {
  padding: 10rpx 20rpx;
  border-radius: 12rpx;
}
.action-edit {
  background: rgba(43, 111, 240, 0.08);
}
.action-btn-text {
  font-size: 24rpx;
  color: #2B6FF0;
  font-weight: 500;
}
.action-delete {
  background: rgba(245, 63, 63, 0.06);
}
.action-delete-text {
  font-size: 24rpx;
  color: #F53F3F;
  font-weight: 500;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}
.empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}
.empty-text {
  font-size: 30rpx;
  color: #86909C;
  font-weight: 500;
  margin-bottom: 8rpx;
}
.empty-hint {
  font-size: 24rpx;
  color: #C9CDD4;
}

.add-btn {
  position: fixed;
  bottom: 60rpx;
  left: 32rpx;
  right: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-radius: 48rpx;
  padding: 28rpx 0;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
  z-index: 10;
}
.add-btn:active {
  opacity: 0.85;
}
.add-btn-icon {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 300;
  line-height: 1;
}
.add-btn-text {
  font-size: 30rpx;
  color: #FFFFFF;
  font-weight: 600;
}

.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}
.popup-content {
  width: 100%;
  background: #FFFFFF;
  border-radius: 32rpx 32rpx 0 0;
  padding: 36rpx 32rpx 60rpx;
  box-sizing: border-box;
}
.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32rpx;
}
.popup-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1D2129;
}
.popup-close {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #F2F3F5;
  display: flex;
  align-items: center;
  justify-content: center;
}
.popup-close-text {
  font-size: 28rpx;
  color: #86909C;
}

.popup-form {
  background: #FAFBFC;
  border-radius: 20rpx;
  padding: 8rpx 28rpx;
}
.popup-form-item {
  padding: 24rpx 0;
}
.popup-form-label {
  font-size: 26rpx;
  color: #86909C;
  font-weight: 500;
  margin-bottom: 12rpx;
  display: block;
}
.popup-form-input {
  font-size: 30rpx;
  color: #1D2129;
  font-weight: 500;
  width: 100%;
}
.popup-form-placeholder {
  color: #C9CDD4;
  font-size: 30rpx;
}
.popup-form-divider {
  height: 1rpx;
  background: #E5E6EB;
}

.relation-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 8rpx;
}
.relation-option {
  padding: 12rpx 28rpx;
  border-radius: 12rpx;
  background: #FFFFFF;
  border: 2rpx solid #E5E6EB;
  transition: all 0.2s ease;
}
.relation-option-active {
  background: rgba(43, 111, 240, 0.08);
  border-color: #2B6FF0;
}
.relation-option-text {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.relation-option-text-active {
  color: #2B6FF0;
  font-weight: 600;
}

.popup-submit {
  margin-top: 40rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-radius: 48rpx;
  padding: 28rpx 0;
  text-align: center;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
}
.popup-submit:active {
  opacity: 0.85;
}
.popup-submit-text {
  font-size: 30rpx;
  color: #FFFFFF;
  font-weight: 700;
}
</style>
