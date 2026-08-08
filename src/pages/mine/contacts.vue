<template>
  <view class="page apple-page motion-page-sheet">
    <view class="contact-list">
      <view
        v-for="(contact, idx) in contacts"
        :key="contact.id"
        class="contact-card"
        @tap="openContactActions(idx)"
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
        <app-icon name="right" :size="16" color="#A1ACB8" />
      </view>

      <view v-if="contacts.length === 0" class="empty-state">
        <app-icon-tile class="empty-icon" name="phone-filled" tone="green" size="large" />
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
import AppIconTile from '@/components/AppIconTile.vue'
import {
  createEmergencyContact,
  deleteEmergencyContact,
  listEmergencyContacts,
  updateEmergencyContact,
  type EmergencyContactResponse
} from '@/api/user'
import { userFacingError } from '@/utils/presentation'

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

function openContactActions(idx: number) {
  const contact = contacts.value[idx]
  uni.showActionSheet({
    itemList: ['拨打电话', '编辑联系人', '删除联系人'],
    success: ({ tapIndex }) => {
      if (tapIndex === 0) return uni.makePhoneCall({ phoneNumber: contact.phone })
      if (tapIndex === 1) return editContact(idx)
      deleteContact(idx)
    }
  })
}

function deleteContact(idx: number) {
  const contact = contacts.value[idx]
  uni.showModal({
    title: '确认删除',
    content: `确定要删除联系人“${contact.name}”吗？`,
    confirmColor: '#C93D46',
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
    uni.showToast({ title: userFacingError(error, '保存失败，请重试'), icon: 'none' })
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
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #F2F2F7;
  padding: 24rpx 32rpx;
  box-sizing: border-box;
  padding-bottom: 160rpx;
}
.page::before { display: none; }

.contact-list {
  position: relative;
  z-index: 1;
  overflow: hidden;
  border: 1rpx solid #E2E8EF;
  border-radius: 22rpx;
  background: #FFFFFF;
  box-shadow: none;
}
.contact-card {
  display: flex;
  align-items: center;
  min-height: 118rpx;
  margin-left: 22rpx;
  padding: 0 22rpx 0 0;
  border-bottom: 1rpx solid #EDF1F5;
  background: #FFFFFF;
}
.contact-card:last-child { border-bottom: 0; }
.contact-card:active { background: #F7F9FB; }
.contact-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 18rpx;
  background: #3478D4;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.contact-card:nth-child(3n + 2) .contact-avatar { background: #248A5A; }
.contact-card:nth-child(3n) .contact-avatar { background: #64748B; }
.contact-avatar-text {
  font-size: 26rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.contact-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3rpx;
}
.contact-name-row {
  display: flex;
  align-items: center;
  gap: 9rpx;
}
.contact-name {
  font-size: 27rpx;
  font-weight: 680;
  color: #20364D;
}
.relation-tag {
  background: rgba(43, 111, 240, 0.08);
  border-radius: 7rpx;
  padding: 2rpx 10rpx;
}
.relation-tag-text {
  font-size: 17rpx;
  color: #2E6DD1;
  font-weight: 500;
}
.contact-phone {
  font-size: 22rpx;
  color: #86909C;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}
.empty-icon {
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
  left: 74rpx;
  right: 74rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: #007AFF;
  min-height: 84rpx;
  border-radius: 22rpx;
  padding: 0;
  box-shadow: none;
  z-index: 10;
}
.add-btn:active {
  opacity: 0.85;
}
.add-btn-icon {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 300;
  line-height: 1;
}
.add-btn-text {
  font-size: 26rpx;
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
  color: #20364D;
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
  color: #20364D;
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
  border-color: #2E6DD1;
}
.relation-option-text {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.relation-option-text-active {
  color: #2E6DD1;
  font-weight: 600;
}

.popup-submit {
  margin-top: 40rpx;
  background: #007AFF;
  border-radius: 48rpx;
  padding: 28rpx 0;
  text-align: center;
  box-shadow: none;
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
