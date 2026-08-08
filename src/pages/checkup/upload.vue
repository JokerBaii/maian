<template>
  <view class="page apple-page motion-page-sheet">
    <view class="page-scroll">
      <view class="form-card">
        <view class="section-head">
          <text class="section-title">报告信息</text>
          <text class="required-note">标 * 为必填</text>
        </view>

        <view class="field">
          <text class="field-label">体检日期 *</text>
          <picker mode="date" :value="form.checkupDate" :end="today" @change="changeDate">
            <view class="picker-value">
              <text>{{ form.checkupDate }}</text>
              <app-icon name="right" :size="15" color="#8A98AD" />
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label">体检机构 *</text>
          <input
            v-model="form.hospital"
            class="field-input"
            maxlength="120"
            placeholder="请输入医院或体检中心名称"
            placeholder-class="placeholder"
          />
        </view>

        <view class="field">
          <view class="field-label-row">
            <text class="field-label">报告原图</text>
            <text class="field-hint">可选，最大 5MB</text>
          </view>
          <view v-if="!imagePath" class="image-upload" @tap="chooseImage">
            <app-icon-tile name="camera" tone="blue" />
            <view>
              <text class="upload-title">拍照或从相册选择</text>
            </view>
          </view>
          <view v-else class="image-preview">
            <image :src="imagePath" class="preview-image" mode="aspectFill" />
            <view class="preview-mask" @tap="chooseImage">
              <text>更换图片</text>
            </view>
            <view class="remove-image" @tap.stop="clearImage">
              <app-icon name="closeempty" :size="15" color="#FFFFFF" />
            </view>
          </view>

          <view v-if="imagePath" class="recognize-row">
            <view
              class="recognize-button"
              :class="{ 'recognize-button-busy': recognizing }"
              @tap="recognizeReport"
            >
              <app-icon name="scan" :size="16" color="#FFFFFF" />
              <text class="recognize-text">{{ recognizing ? '识别中…' : '识别报告指标' }}</text>
            </view>
            <text class="recognize-hint">自动读取机构、日期与关键指标</text>
          </view>

          <view v-if="imagePath" class="recognition-note">
            <app-icon name="info" :size="13" color="#6F7F94" />
            <text>经你确认后可自动读取报告内容，识别结果请与原报告核对。</text>
          </view>

          <view v-if="recognizeNotice" class="recognize-notice">
            <app-icon name="info" :size="14" color="#1F63D5" />
            <text class="recognize-notice-text">{{ recognizeNotice }}</text>
          </view>
        </view>
        <view class="form-divider"></view>
        <view class="indicator-section">
        <view class="section-head">
          <text class="section-title">体检指标</text>
          <text class="indicator-count">{{ form.indicators.length }} 项</text>
        </view>

        <view
          v-for="(indicator, index) in form.indicators"
          :key="indicator.key"
          class="indicator-item"
        >
          <view class="indicator-head">
            <text class="indicator-index">第 {{ index + 1 }} 项</text>
            <view
              v-if="form.indicators.length > 1"
              class="remove-indicator"
              @tap="removeIndicator(index)"
            >
              <text>删除</text>
            </view>
          </view>
          <input
            v-model="indicator.name"
            class="indicator-name"
            maxlength="100"
            placeholder="指标名称，如：空腹血糖"
            placeholder-class="placeholder"
          />
          <view class="indicator-grid">
            <view class="mini-field value-field">
              <text class="mini-label">数值 *</text>
              <input
                v-model="indicator.value"
                class="mini-input"
                maxlength="100"
                placeholder="5.2"
                placeholder-class="placeholder"
              />
            </view>
            <view class="mini-field">
              <text class="mini-label">单位</text>
              <input
                v-model="indicator.unit"
                class="mini-input"
                maxlength="50"
                placeholder="mmol/L"
                placeholder-class="placeholder"
              />
            </view>
          </view>
          <view class="indicator-bottom">
            <view class="range-field">
              <text class="mini-label">参考范围</text>
              <input
                v-model="indicator.referenceRange"
                class="mini-input"
                maxlength="100"
                placeholder="3.9-6.1"
                placeholder-class="placeholder"
              />
            </view>
            <view class="abnormal-toggle">
              <text :class="{ active: indicator.abnormal }">标记异常</text>
              <switch
                :checked="indicator.abnormal"
                color="#D6424B"
                @change="toggleAbnormal(index, $event)"
              />
            </view>
          </view>
        </view>

        <view v-if="form.indicators.length < 20" class="add-indicator" @tap="addIndicator">
          <app-icon name="plusempty" :size="18" color="#1F63D5" />
          <text>添加指标</text>
        </view>
        </view>
      </view>

      <view class="submit-area">
        <view class="submit-button" :class="{ disabled: submitting }" @tap="submitReport">
          <app-icon :name="submitting ? 'spinner-cycle' : 'checkmarkempty'" :size="19" color="#FFFFFF" />
          <text>{{ submitting ? '正在保存…' : '保存并查看结果' }}</text>
        </view>
        <text class="disclaimer">分析仅用于健康信息整理，不能替代医生诊断</text>
      </view>
      <view class="bottom-space"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { uploadImage } from '@/api/files'
import { createHealthReport, recognizeHealthReport } from '@/api/reports'
import { getUserSettings, updateUserSettings } from '@/api/user'
import { userFacingError } from '@/utils/presentation'

interface IndicatorForm {
  key: number
  name: string
  value: string
  unit: string
  referenceRange: string
  abnormal: boolean
}

const now = new Date()
const today = [
  now.getFullYear(),
  String(now.getMonth() + 1).padStart(2, '0'),
  String(now.getDate()).padStart(2, '0')
].join('-')

let nextKey = 2
const form = reactive({
  checkupDate: today,
  hospital: '',
  indicators: [createIndicator(1)] as IndicatorForm[]
})
const imagePath = ref('')
const submitting = ref(false)
const recognizing = ref(false)
const recognizeNotice = ref('')
/** 识别时已上传的私有媒体 ID，保存时直接复用。 */
const uploadedMediaId = ref('')

function createIndicator(key: number): IndicatorForm {
  return {
    key,
    name: '',
    value: '',
    unit: '',
    referenceRange: '',
    abnormal: false
  }
}

function changeDate(event: any) {
  form.checkupDate = String(event.detail?.value || today)
}

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (result) => {
      imagePath.value = result.tempFilePaths[0] || ''
      // 换图后此前的上传结果和识别提示都不再对应当前图片
      uploadedMediaId.value = ''
      recognizeNotice.value = ''
    }
  })
}

function clearImage() {
  imagePath.value = ''
  uploadedMediaId.value = ''
  recognizeNotice.value = ''
}

async function recognizeReport() {
  if (recognizing.value || !imagePath.value) return
  recognizing.value = true
  uni.showLoading({ title: '识别中…' })
  try {
    if (!await ensureOcrConsent()) {
      uni.hideLoading()
      return
    }
    if (!uploadedMediaId.value) {
      const uploaded = await uploadImage(imagePath.value, 'HEALTH_REPORT')
      uploadedMediaId.value = uploaded.mediaId
    }
    const result = await recognizeHealthReport(uploadedMediaId.value)

    form.hospital = result.hospital
    form.checkupDate = result.checkupDate
    form.indicators = result.indicators.map((indicator) => ({
      key: nextKey++,
      name: indicator.name,
      value: indicator.value,
      unit: indicator.unit || '',
      referenceRange: indicator.referenceRange || '',
      abnormal: indicator.abnormal
    }))
    recognizeNotice.value = result.notice
    uni.hideLoading()
    uni.showToast({ title: `已识别 ${result.indicators.length} 项指标`, icon: 'success' })
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: userFacingError(error, '识别失败，请重试或手动填写'), icon: 'none' })
  } finally {
    recognizing.value = false
  }
}

async function ensureOcrConsent() {
  const settings = await getUserSettings()
  if (settings.healthDataShare) return true
  uni.hideLoading()
  const confirmed = await new Promise<boolean>(resolve => {
    uni.showModal({
      title: '授权智能识别',
      content: '报告图片将交由受保护的文字识别服务处理。是否同意本次及后续使用智能识别？',
      confirmText: '同意并继续',
      cancelText: '手动录入',
      success: result => resolve(Boolean(result.confirm)),
      fail: () => resolve(false)
    })
  })
  if (!confirmed) return false
  await updateUserSettings({
    rescuePush: settings.rescuePush,
    healthAlert: settings.healthAlert,
    scienceUpdate: settings.scienceUpdate,
    locationShare: settings.locationShare,
    healthDataShare: true,
    maxHeartRate: settings.maxHeartRate,
    minHeartRate: settings.minHeartRate
  })
  uni.showLoading({ title: '识别中…' })
  return true
}

function addIndicator() {
  form.indicators.push(createIndicator(nextKey++))
}

function removeIndicator(index: number) {
  form.indicators.splice(index, 1)
}

function toggleAbnormal(index: number, event: any) {
  form.indicators[index].abnormal = Boolean(event.detail?.value)
}

async function submitReport() {
  if (submitting.value) return
  const hospital = form.hospital.trim()
  if (!hospital) {
    uni.showToast({ title: '请填写体检机构', icon: 'none' })
    return
  }

  const indicators = form.indicators.map(indicator => ({
    name: indicator.name.trim(),
    value: indicator.value.trim(),
    unit: indicator.unit.trim() || undefined,
    referenceRange: indicator.referenceRange.trim() || undefined,
    abnormal: indicator.abnormal
  }))
  if (indicators.some(indicator => !indicator.name || !indicator.value)) {
    uni.showToast({ title: '请完整填写指标名称和数值', icon: 'none' })
    return
  }

  submitting.value = true
  uni.showLoading({ title: '生成分析中…' })
  try {
    // 识别时已上传过就直接复用，避免同一张图上传两次
    if (imagePath.value && !uploadedMediaId.value) {
      const uploaded = await uploadImage(imagePath.value, 'HEALTH_REPORT')
      uploadedMediaId.value = uploaded.mediaId
    }
    const report = await createHealthReport({
      checkupDate: form.checkupDate,
      hospital,
      sourceMediaId: uploadedMediaId.value || undefined,
      indicators
    })
    uni.hideLoading()
    uni.redirectTo({ url: `/pages/checkup/report?id=${encodeURIComponent(report.id)}` })
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: userFacingError(error, '报告保存失败，请重试'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F2F2F7;
  color: #18253A;
}

.page-scroll {
  min-height: 100vh;
}

.form-card {
  margin: 20rpx 20rpx 0;
  padding: 25rpx;
  border: 1rpx solid #E2E8EF;
  border-radius: 22rpx;
  background: #FFFFFF;
  box-shadow: 0 16rpx 42rpx rgba(42, 67, 92, .08);
}

.section-head,
.field-label-row,
.indicator-head,
.indicator-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  display: block;
  font-size: 28rpx;
  font-weight: 750;
}

.required-note,
.field-hint,
.indicator-count {
  color: #8997AB;
  font-size: 21rpx;
}

.field {
  margin-top: 22rpx;
}

.field-label,
.mini-label {
  display: block;
  color: #56657C;
  font-size: 23rpx;
  font-weight: 700;
}

.field-input,
.picker-value {
  height: 74rpx;
  box-sizing: border-box;
  margin-top: 13rpx;
  padding: 0 22rpx;
  border: 1rpx solid #DDE5F0;
  border-radius: 14rpx;
  background: #F8FAFD;
  font-size: 26rpx;
}

.picker-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.placeholder {
  color: #B2BDCC;
}

.image-upload {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 13rpx;
  min-height: 76rpx;
  padding: 15rpx 18rpx;
  border: 1rpx dashed #B7C7DD;
  border-radius: 18rpx;
  background: #F2F7FC;
}

.upload-title {
  display: block;
  font-size: 25rpx;
  font-weight: 700;
}

.recognize-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 18rpx;
}

.recognize-button {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 26rpx;
  border-radius: 999rpx;
  background: #007AFF;
  box-shadow: 0 8rpx 18rpx rgba(47, 115, 232, 0.24);
}

.recognize-button-busy {
  opacity: 0.6;
}

.recognize-text {
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 700;
}

.recognize-hint {
  flex: 1;
  color: #8D9AAF;
  font-size: 21rpx;
}

.recognition-note {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 14rpx;
  padding: 14rpx 16rpx;
  border-radius: 12rpx;
  background: #F4F6F8;
  color: #6F7F94;
  font-size: 19rpx;
  line-height: 1.55;
}

.recognition-note text { flex: 1; }

.recognize-notice {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-top: 14rpx;
  padding: 16rpx 18rpx;
  border-radius: 14rpx;
  background: rgba(47, 115, 232, 0.08);
}

.recognize-notice-text {
  flex: 1;
  color: #1F63D5;
  font-size: 21rpx;
  line-height: 1.5;
}

.image-preview {
  position: relative;
  height: 280rpx;
  margin-top: 13rpx;
  overflow: hidden;
  border-radius: 18rpx;
  background: #EDF2F8;
}

.preview-image {
  width: 100%;
  height: 100%;
}

.preview-mask {
  position: absolute;
  right: 14rpx;
  bottom: 14rpx;
  padding: 9rpx 15rpx;
  border-radius: 12rpx;
  background: rgba(19, 34, 56, 0.7);
  color: #FFFFFF;
  font-size: 21rpx;
}

.remove-image {
  position: absolute;
  top: 14rpx;
  right: 14rpx;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(25, 38, 58, 0.72);
}

.form-divider { height: 1rpx; margin: 28rpx -25rpx 24rpx; background: #E7ECF1; }
.indicator-section { margin: 0; }

.indicator-item {
  position: relative;
  margin-top: 18rpx;
  padding: 18rpx;
  border: 1rpx solid #E0E7F0;
  border-radius: 16rpx;
  overflow: hidden;
  background: #F7F7FA;
  animation: indicatorReveal 320ms cubic-bezier(.2, .72, .2, 1) both;
}
.indicator-item::before { display: none; }

@keyframes indicatorReveal {
  from { opacity: 0; transform: translateY(12rpx) scale(.995); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.indicator-index {
  font-size: 23rpx;
  font-weight: 800;
}

.remove-indicator {
  color: #C63E49;
  font-size: 21rpx;
}

.indicator-name {
  height: 66rpx;
  margin-top: 15rpx;
  padding: 0 18rpx;
  border-radius: 14rpx;
  background: #FFFFFF;
  font-size: 25rpx;
}

.indicator-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 14rpx;
  margin-top: 13rpx;
}

.mini-field,
.range-field {
  min-width: 0;
}

.mini-input {
  height: 62rpx;
  margin-top: 8rpx;
  padding: 0 16rpx;
  border-radius: 13rpx;
  background: #FFFFFF;
  font-size: 24rpx;
}

.indicator-bottom {
  gap: 18rpx;
  margin-top: 17rpx;
}

.range-field {
  flex: 1;
}

.abnormal-toggle {
  flex: none;
  display: flex;
  align-items: center;
  gap: 7rpx;
  align-self: flex-end;
  height: 68rpx;
  color: #8794A8;
  font-size: 20rpx;
}

.abnormal-toggle .active {
  color: #C63541;
  font-weight: 700;
}

.abnormal-toggle switch {
  transform: scale(0.72);
  transform-origin: right center;
}

.add-indicator {
  height: 66rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9rpx;
  margin-top: 22rpx;
  border: 1rpx dashed #ABC1E3;
  border-radius: 16rpx;
  background: #F3F7FE;
  color: #1F63D5;
  font-size: 24rpx;
  font-weight: 700;
}

.submit-area {
  margin: 22rpx 20rpx 0;
}

.submit-button {
  height: 82rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: 17rpx;
  background: #2E6DD1;
  box-shadow: 0 8rpx 20rpx rgba(37, 101, 207, 0.18);
  color: #FFFFFF;
  font-size: 27rpx;
  font-weight: 800;
}

.submit-button.disabled {
  opacity: 0.62;
}

.disclaimer {
  display: block;
  margin-top: 15rpx;
  color: #8996AA;
  text-align: center;
  font-size: 20rpx;
}

.bottom-space {
  height: calc(44rpx + env(safe-area-inset-bottom));
}
</style>
