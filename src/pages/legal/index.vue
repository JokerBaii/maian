<template>
  <view class="page">
    <view class="notice">
      <view class="notice-line"></view>
      <view>
        <text class="notice-title">{{ document.title }}</text>
        <text class="notice-date">更新日期：2026年8月2日</text>
      </view>
    </view>

    <view class="document-card">
      <view v-for="section in document.sections" :key="section.title" class="section">
        <text class="section-title">{{ section.title }}</text>
        <text v-for="paragraph in section.paragraphs" :key="paragraph" class="paragraph">{{ paragraph }}</text>
      </view>
    </view>

    <view v-if="documentType === 'privacy'" class="consent-card">
      <text class="consent-title">授权管理</text>
      <text class="consent-desc">系统权限可在手机设置中关闭；撤回本应用内的首次确认不会自动删除已经依法保存的业务记录。</text>
      <view class="consent-action" @tap="withdrawConsent">撤回本机确认</view>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

type DocumentType = 'privacy' | 'terms' | 'medical'
type LegalDocument = { title: string; sections: Array<{ title: string; paragraphs: string[] }> }

const PRIVACY_CONSENT_KEY = 'maian:privacy-consent:v1'
const documentType = ref<DocumentType>('privacy')

const documents: Record<DocumentType, LegalDocument> = {
  privacy: {
    title: '隐私政策',
    sections: [
      { title: '一、适用范围', paragraphs: ['本政策说明脉安驰援在提供急救设备、救援协同和健康管理功能时如何处理个人信息。应用仅在用户主动使用对应功能时申请必要权限。'] },
      { title: '二、信息与用途', paragraphs: ['位置：用于展示附近急救设备、提交呼救位置、更新移动设备位置和打开系统导航。', '身份与联系方式：用于保存用户资料、紧急联系人和设备负责人联系方式；身份证仅做格式与校验位检查，并以脱敏形式保存。', '健康数据：用于展示用户主动同步的心率记录、阈值预警和体检指标分析。', '图片与设备信息：用于用户主动上传急救现场、设备、体检报告图片，以及绑定蓝牙穿戴设备。'] },
      { title: '三、存储与共享', paragraphs: ['业务数据保存于项目配置的服务端数据库和上传目录。未经用户主动操作或法律法规要求，不向无关第三方出售个人信息。地图、系统导航和可选的智能分析服务仅在启用相应能力时处理完成服务所需的数据。'] },
      { title: '四、用户权利', paragraphs: ['用户可以在应用设置中关闭位置共享和健康数据共享，可以删除紧急联系人、本人录入的设备、健康报告及本机缓存。系统权限可在手机系统设置中撤回。'] },
      { title: '五、安全提示', paragraphs: ['请勿在公开演示环境录入真实身份证号、详细病历或其他不必要的敏感信息。发现信息安全问题时，请停止使用相关功能并联系项目负责人。'] }
    ]
  },
  terms: {
    title: '用户协议',
    sections: [
      { title: '一、服务内容', paragraphs: ['脉安驰援提供急救设备信息登记与查询、救援协同、健康记录整理和急救科普。用户应依法、真实、谨慎地使用各项功能。'] },
      { title: '二、用户责任', paragraphs: ['用户不得提交虚假设备、虚假呼救、侵权内容或违法信息。设备状态、位置和联系方式发生变化时，应及时更新。'] },
      { title: '三、紧急情况', paragraphs: ['本应用不能替代120急救系统。发生危及生命的紧急情况时，应立即拨打120，并在确保自身安全的前提下按照专业人员或AED语音提示开展救助。'] },
      { title: '四、服务限制', paragraphs: ['网络、定位、蓝牙、第三方地图或设备硬件可能影响服务可用性。应用展示的距离和预计时间仅供辅助判断，不构成到达承诺。'] },
      { title: '五、内容规范', paragraphs: ['科普内容用于公众教育，不得替代专业培训。用户投稿应注明来源并对内容合法性、真实性和知识产权负责。'] }
    ]
  },
  medical: {
    title: '医疗免责声明',
    sections: [
      { title: '健康分析边界', paragraphs: ['心率统计、阈值预警和体检指标分析仅用于个人健康信息整理，不构成诊断、处方、治疗方案或医疗机构意见。'] },
      { title: '异常处理', paragraphs: ['若出现胸痛、呼吸困难、意识障碍、持续心率异常等症状，请立即拨打120或前往正规医疗机构，不应仅依据应用结果自行处置。'] },
      { title: '数据准确性', paragraphs: ['穿戴设备测量结果可能受佩戴方式、运动、硬件和通信状态影响；人工录入的体检指标由用户负责核对，最终以医疗机构原始报告为准。'] },
      { title: '急救设备使用', paragraphs: ['使用AED和其他急救设备时，应优先遵循设备语音、现场专业人员和急救调度人员的指导，并确保现场环境安全。'] }
    ]
  }
}

const document = computed(() => documents[documentType.value])

onLoad(options => {
  const type = options?.type as DocumentType
  if (type && documents[type]) documentType.value = type
  uni.setNavigationBarTitle({ title: documents[documentType.value].title })
})

function withdrawConsent() {
  uni.showModal({
    title: '撤回本机确认',
    content: '撤回后，下次启动会重新显示隐私保护提示。系统权限请同时前往手机设置关闭。',
    confirmText: '确认撤回',
    confirmColor: '#C93D46',
    success: result => {
      if (!result.confirm) return
      uni.removeStorageSync(PRIVACY_CONSENT_KEY)
      uni.showToast({ title: '已撤回本机确认', icon: 'none' })
    }
  })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 24rpx;
  background: #F3F7FA;
  color: #20364D;
}
.notice {
  display: flex;
  align-items: stretch;
  gap: 18rpx;
  padding: 26rpx;
  border: 1rpx solid #DCE6F2;
  border-radius: 18rpx;
  background: #EDF4FC;
}
.notice-line { width: 6rpx; border-radius: 3rpx; background: #2E6DD1; }
.notice-title,
.notice-date { display: block; }
.notice-title { font-size: 34rpx; font-weight: 750; }
.notice-date { margin-top: 6rpx; color: #738399; font-size: 22rpx; }
.document-card,
.consent-card {
  margin-top: 20rpx;
  padding: 28rpx;
  border: 1rpx solid #E1E9F0;
  border-radius: 18rpx;
  background: #FFFFFF;
}
.section + .section { margin-top: 30rpx; }
.section-title { display: block; font-size: 29rpx; font-weight: 750; }
.paragraph { display: block; margin-top: 13rpx; color: #52647A; font-size: 25rpx; line-height: 1.85; }
.consent-title,
.consent-desc { display: block; }
.consent-title { font-size: 28rpx; font-weight: 700; }
.consent-desc { margin-top: 10rpx; color: #6C7D91; font-size: 23rpx; line-height: 1.7; }
.consent-action {
  margin-top: 22rpx;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #F9EAEC;
  color: #A23B43;
  text-align: center;
  font-size: 25rpx;
  font-weight: 650;
}
.bottom-safe { height: calc(32rpx + env(safe-area-inset-bottom)); }
</style>
