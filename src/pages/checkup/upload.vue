<template>
  <view class="page">
    <scroll-view class="page-scroll" scroll-y>
      <view class="intro-card">
        <view class="intro-icon">
          <app-icon name="folder-add-filled" :size="25" color="#FFFFFF" />
        </view>
        <view class="intro-copy">
          <text class="intro-kicker">HEALTH REPORT</text>
          <text class="intro-title">录入体检报告</text>
          <text class="intro-desc">上传报告原图并确认关键指标，系统将生成结构化健康分析</text>
        </view>
      </view>

      <view class="form-card">
        <view class="section-head">
          <view>
            <text class="section-kicker">BASIC INFO</text>
            <text class="section-title">体检信息</text>
          </view>
          <text class="required-note">* 必填</text>
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
            <view class="upload-icon">
              <app-icon name="camera-filled" :size="23" color="#1F63D5" />
            </view>
            <view>
              <text class="upload-title">拍照或从相册选择</text>
              <text class="upload-desc">支持 JPG、PNG、WebP</text>
            </view>
          </view>
          <view v-else class="image-preview">
            <image :src="imagePath" class="preview-image" mode="aspectFill" />
            <view class="preview-mask" @tap="chooseImage">
              <text>更换图片</text>
            </view>
            <view class="remove-image" @tap.stop="imagePath = ''">
              <app-icon name="closeempty" :size="15" color="#FFFFFF" />
            </view>
          </view>
        </view>
      </view>

      <view class="form-card indicator-card">
        <view class="section-head">
          <view>
            <text class="section-kicker">INDICATORS</text>
            <text class="section-title">关键指标</text>
          </view>
          <text class="indicator-count">{{ form.indicators.length }} 项</text>
        </view>
        <text class="section-desc">请按报告原文填写；异常项可手动标记，分析结果以录入内容为准。</text>

        <view
          v-for="(indicator, index) in form.indicators"
          :key="indicator.key"
          class="indicator-item"
        >
          <view class="indicator-head">
            <text class="indicator-index">指标 {{ index + 1 }}</text>
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

      <view class="submit-area">
        <view class="submit-button" :class="{ disabled: submitting }" @tap="submitReport">
          <app-icon name="spinner-cycle" :size="19" color="#FFFFFF" />
          <text>{{ submitting ? '正在生成分析…' : '保存并生成分析' }}</text>
        </view>
        <text class="disclaimer">分析仅用于健康信息整理，不能替代医生诊断</text>
      </view>
      <view class="bottom-space"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { uploadImage } from '@/api/files'
import { createHealthReport } from '@/api/reports'

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
    }
  })
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
    const sourceImage = imagePath.value ? await uploadImage(imagePath.value) : null
    const report = await createHealthReport({
      checkupDate: form.checkupDate,
      hospital,
      sourceImageUrl: sourceImage?.url,
      indicators
    })
    uni.hideLoading()
    uni.redirectTo({ url: `/pages/checkup/report?id=${encodeURIComponent(report.id)}` })
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '报告保存失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F1F5FA;
  color: #18253A;
}

.page-scroll {
  height: 100vh;
}

.intro-card,
.form-card {
  margin: 24rpx 24rpx 0;
  border-radius: 26rpx;
}

.intro-card {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 30rpx;
  background:
    radial-gradient(circle at 92% 6%, rgba(255, 255, 255, 0.2), transparent 32%),
    linear-gradient(135deg, #285FBF 0%, #3B7CE7 100%);
  color: #FFFFFF;
  box-shadow: 0 16rpx 38rpx rgba(40, 99, 198, 0.2);
}

.intro-icon {
  flex: none;
  width: 84rpx;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(255, 255, 255, 0.22);
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.14);
}

.intro-copy {
  min-width: 0;
}

.intro-kicker,
.section-kicker {
  display: block;
  font-size: 19rpx;
  font-weight: 800;
  letter-spacing: 3rpx;
}

.intro-kicker {
  color: rgba(255, 255, 255, 0.62);
}

.intro-title {
  display: block;
  margin-top: 5rpx;
  font-size: 34rpx;
  font-weight: 800;
}

.intro-desc {
  display: block;
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 22rpx;
  line-height: 1.55;
}

.form-card {
  padding: 28rpx;
  background: #FFFFFF;
  box-shadow: 0 8rpx 26rpx rgba(38, 63, 103, 0.06);
}

.section-head,
.field-label-row,
.indicator-head,
.indicator-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-kicker {
  color: #8190A7;
}

.section-title {
  display: block;
  margin-top: 5rpx;
  font-size: 31rpx;
  font-weight: 800;
}

.required-note,
.field-hint,
.indicator-count {
  color: #8997AB;
  font-size: 21rpx;
}

.field {
  margin-top: 27rpx;
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
  height: 82rpx;
  box-sizing: border-box;
  margin-top: 13rpx;
  padding: 0 22rpx;
  border: 1rpx solid #DDE5F0;
  border-radius: 17rpx;
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
  padding: 24rpx;
  border: 1rpx dashed #B7C7DD;
  border-radius: 18rpx;
  background: #F7FAFF;
}

.upload-icon {
  width: 68rpx;
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 19rpx;
  background: #E8F1FF;
}

.upload-title,
.upload-desc {
  display: block;
}

.upload-title {
  font-size: 25rpx;
  font-weight: 700;
}

.upload-desc {
  margin-top: 5rpx;
  color: #8D9AAF;
  font-size: 21rpx;
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

.section-desc {
  display: block;
  margin-top: 15rpx;
  color: #7D8AA0;
  font-size: 22rpx;
  line-height: 1.6;
}

.indicator-item {
  margin-top: 24rpx;
  padding: 22rpx;
  border: 1rpx solid #E0E7F0;
  border-radius: 20rpx;
  background: #FAFCFF;
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
  height: 72rpx;
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
  margin-top: 16rpx;
}

.mini-field,
.range-field {
  min-width: 0;
}

.mini-input {
  height: 68rpx;
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
  height: 76rpx;
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
  margin: 28rpx 24rpx 0;
}

.submit-button {
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #245FC6 0%, #377BE9 100%);
  box-shadow: 0 12rpx 28rpx rgba(37, 101, 207, 0.24);
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
