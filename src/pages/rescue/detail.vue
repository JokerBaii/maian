<template>
  <view class="page">
    <!-- 顶部状态区域 -->
    <view class="status-header" :class="'status-bg-' + rescueStatus">
      <view class="status-header-content">
        <view class="status-icon-wrap">
          <view class="status-icon-circle">
            <text class="status-emoji">{{ statusEmoji }}</text>
          </view>
          <view v-if="rescueStatus === 'matching' || rescueStatus === 'rescuing'" class="status-pulse"></view>
        </view>
        <text class="status-main-text">{{ statusMainText }}</text>
        <text class="status-sub-text">{{ statusSubText }}</text>
      </view>
    </view>

    <scroll-view class="scroll-content" scroll-y>
      <!-- 进度时间线 -->
      <view class="timeline-section">
        <view class="timeline">
          <view
            v-for="(step, idx) in timelineSteps"
            :key="idx"
            class="timeline-step"
            :class="{
              'step-active': step.active,
              'step-done': step.done,
              'step-current': step.current
            }"
          >
            <view class="step-line" v-if="idx > 0" :class="{ 'line-done': step.done || step.active }"></view>
            <view class="step-node">
              <view class="step-dot">
                <text v-if="step.done" class="step-check">&#x2713;</text>
                <text v-else-if="step.current" class="step-number">{{ idx + 1 }}</text>
                <text v-else class="step-number step-number-pending">{{ idx + 1 }}</text>
              </view>
            </view>
            <view class="step-info">
              <text class="step-title" :class="{ 'step-title-active': step.done || step.active }">{{ step.label }}</text>
              <text v-if="step.time" class="step-time">{{ step.time }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 匹配信息卡片 -->
      <view v-if="rescueData.matchedVolunteer" class="info-section">
        <view class="section-card">
          <view class="card-header">
            <view class="card-header-icon card-icon-volunteer">
              <text class="card-header-emoji">👨‍⚕️</text>
            </view>
            <view class="card-header-info">
              <text class="card-header-title">救援志愿者</text>
              <text class="card-header-sub">{{ rescueData.matchedVolunteer }}</text>
            </view>
            <view class="card-header-action" @tap="callVolunteer">
              <text class="call-icon">📞</text>
              <text class="call-text">联系</text>
            </view>
          </view>
          <view class="card-body">
            <view class="info-row">
              <text class="info-label">联系电话</text>
              <text class="info-value">{{ rescueData.volunteerPhone }}</text>
            </view>
            <view v-if="rescueData.matchedDevice" class="info-row">
              <text class="info-label">携带设备</text>
              <text class="info-value info-value-highlight">{{ getDeviceName(rescueData.matchedDevice) }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">预计到达</text>
              <text class="info-value">约{{ rescueData.urgency === 'critical' ? '5' : '10' }}分钟</text>
            </view>
            <view class="info-row">
              <text class="info-label">距您距离</text>
              <text class="info-value info-value-highlight">{{ volunteerDistance }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 实时位置追踪区域 - 高德地图 -->
      <view class="info-section">
        <view class="section-card">
          <view class="card-header">
            <view class="card-header-icon card-icon-location">
              <text class="card-header-emoji">📍</text>
            </view>
            <view class="card-header-info">
              <text class="card-header-title">实时位置追踪</text>
              <text class="card-header-sub">{{ rescueData.address }}</text>
            </view>
            <view class="refresh-badge" @tap="refreshLocation">
              <text class="refresh-icon">🔄</text>
              <text class="refresh-text">{{ locationRefreshing ? '刷新中...' : '刷新' }}</text>
            </view>
          </view>
          <!-- 高德地图容器 -->
          <view class="rescue-map-wrapper">
            <view id="rescue-map-container" class="rescue-map-container"></view>
            <!-- 位置刷新提示 -->
            <view class="map-refresh-tip">
              <view class="refresh-dot"></view>
              <text class="refresh-tip-text">每5秒自动更新位置</text>
            </view>
          </view>
          <!-- 位置信息 -->
          <view class="location-info-bar">
            <view class="location-info-item">
              <view class="location-dot location-dot-self"></view>
              <text class="location-info-label">我的位置</text>
              <text class="location-info-value">{{ myLocationText }}</text>
            </view>
            <view v-if="rescueData.matchedVolunteer" class="location-info-item">
              <view class="location-dot location-dot-volunteer"></view>
              <text class="location-info-label">志愿者位置</text>
              <text class="location-info-value">{{ volunteerLocationText }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 求救信息 -->
      <view class="info-section">
        <view class="section-card">
          <view class="card-header">
            <view class="card-header-icon card-icon-info">
              <text class="card-header-emoji">📋</text>
            </view>
            <view class="card-header-info">
              <text class="card-header-title">求救信息</text>
            </view>
          </view>
          <view class="card-body">
            <view class="info-row">
              <text class="info-label">紧急等级</text>
              <view class="urgency-badge" :class="'urgency-badge-' + rescueData.urgency">
                <text class="urgency-badge-text">{{ urgencyLabel(rescueData.urgency) }}</text>
              </view>
            </view>
            <view class="info-row">
              <text class="info-label">发起时间</text>
              <text class="info-value">{{ rescueData.createTime }}</text>
            </view>
            <view class="info-row info-row-desc">
              <text class="info-label">描述</text>
              <text class="info-value info-value-desc">{{ rescueData.description }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 通讯按钮 -->
      <view v-if="rescueStatus === 'rescuing' || rescueStatus === 'matching'" class="comm-area">
        <view class="comm-btn comm-btn-call" @tap="callVolunteer">
          <text class="comm-btn-icon">📞</text>
          <text class="comm-btn-text">电话联系</text>
        </view>
        <view class="comm-btn comm-btn-msg" @tap="openChat">
          <text class="comm-btn-icon">💬</text>
          <text class="comm-btn-text">发送消息</text>
        </view>
      </view>

      <!-- 聊天面板 -->
      <view v-if="showChat" class="chat-overlay" :style="{ paddingTop: statusBarHeight + 'px' }">
        <view class="chat-panel">
          <view class="chat-header">
            <view class="chat-back" @tap="closeChat">
              <text class="chat-back-icon">&#x2190;</text>
            </view>
            <view class="chat-header-info">
              <text class="chat-header-name">{{ rescueData.matchedVolunteer || '救援志愿者' }}</text>
              <text class="chat-header-status">在线</text>
            </view>
            <view class="chat-header-call" @tap="callVolunteer">
              <text class="chat-call-icon">📞</text>
            </view>
          </view>
          <scroll-view class="chat-messages" scroll-y :scroll-top="chatScrollTop">
            <view
              v-for="(msg, idx) in chatMessages"
              :key="idx"
              class="chat-msg"
              :class="{ 'chat-msg-self': msg.isSelf, 'chat-msg-other': !msg.isSelf }"
            >
              <view v-if="!msg.isSelf" class="chat-avatar">
                <text class="chat-avatar-text">🆘</text>
              </view>
              <view class="chat-bubble" :class="{ 'bubble-self': msg.isSelf, 'bubble-other': !msg.isSelf }">
                <text class="chat-bubble-text">{{ msg.content }}</text>
                <text class="chat-bubble-time">{{ msg.time }}</text>
              </view>
              <view v-if="msg.isSelf" class="chat-avatar chat-avatar-self">
                <text class="chat-avatar-text">👤</text>
              </view>
            </view>
          </scroll-view>
          <view class="chat-input-area">
            <view class="chat-input-wrap">
              <input
                class="chat-input"
                v-model="chatInput"
                placeholder="输入消息..."
                placeholder-class="chat-input-placeholder"
                confirm-type="send"
                @confirm="sendChatMessage"
              />
            </view>
            <view class="chat-send-btn" @tap="sendChatMessage">
              <text class="chat-send-text">发送</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 评价区域（已完成时显示） -->
      <view v-if="rescueStatus === 'completed'" class="info-section">
        <view class="section-card rating-card">
          <view class="card-header">
            <view class="card-header-icon card-icon-rating">
              <text class="card-header-emoji">⭐</text>
            </view>
            <view class="card-header-info">
              <text class="card-header-title">救援评价</text>
              <text class="card-header-sub">为本次救援服务评分</text>
            </view>
          </view>
          <view class="rating-stars">
            <view
              v-for="star in 5"
              :key="star"
              class="star-item"
              @tap="setRating(star)"
            >
              <text class="star-icon" :class="{ 'star-active': star <= rating }">&#x2605;</text>
            </view>
          </view>
          <view v-if="rating > 0" class="rating-submit" @tap="submitRating">
            <text class="rating-submit-text">提交评价</text>
          </view>
          <view v-if="rescueData.rating" class="rating-done">
            <view class="rating-done-stars">
              <text v-for="star in rescueData.rating" :key="star" class="star-icon star-active">&#x2605;</text>
            </view>
            <text class="rating-done-text">已评价</text>
          </view>
        </view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { mockRescueCalls, mockMobileDevices, mockFixedDevices } from '@/mock/data'

// 获取路由参数
const rescueId = ref('R001')
const pages = getCurrentPages()
const currentPage = pages[pages.length - 1] as any
if (currentPage?.options?.id) {
  rescueId.value = currentPage.options.id
}

// 救援数据
const rescueData = computed(() => {
  const found = mockRescueCalls.find(r => r.id === rescueId.value)
  return found || mockRescueCalls[0]
})

// 救援状态
const rescueStatus = computed(() => rescueData.value.status)

// 评价
const rating = ref(0)

// 聊天
const showChat = ref(false)
const chatInput = ref('')
const chatScrollTop = ref(0)
const chatMessages = ref<any[]>([
  {
    isSelf: true,
    content: '你好，我需要紧急救援！',
    time: '14:30'
  },
  {
    isSelf: false,
    content: '收到！我已出发，预计5分钟到达，请保持电话畅通。',
    time: '14:31'
  },
  {
    isSelf: true,
    content: '好的，我在文三路268号门口，穿红色外套',
    time: '14:32'
  },
  {
    isSelf: false,
    content: '收到，我带着AED设备正在赶来，请保持冷静，有情况随时告诉我。',
    time: '14:33'
  }
])

// 快捷回复选项
const quickReplies = [
  '我位置有变化',
  '伤者情况稳定',
  '需要更多设备',
  '已到达约定地点'
]

// 地图相关
let mapInstance: any = null
let selfMarker: any = null
let volunteerMarker: any = null
let routeLine: any = null
let refreshTimer: any = null
const locationRefreshing = ref(false)

// 模拟位置 - 呼救者固定位置
const myLng = ref(rescueData.value.longitude || 120.15)
const myLat = ref(rescueData.value.latitude || 30.28)

// 模拟志愿者位置 - 初始在附近，会随时间移动靠近
const volunteerLng = ref(myLng.value + 0.012)
const volunteerLat = ref(myLat.value + 0.008)

// 位置文字描述
const myLocationText = ref('杭州市西湖区文三路268号附近')
const volunteerLocationText = ref('正在赶往现场')
const volunteerDistance = ref('1.2km')

// 状态显示
const statusEmoji = computed(() => {
  const map: Record<string, string> = {
    pending: '⏳', matching: '🔍', rescuing: '🚑',
    completed: '✅', cancelled: '❌'
  }
  return map[rescueStatus.value] || '⏳'
})

const statusMainText = computed(() => {
  const map: Record<string, string> = {
    pending: '等待处理', matching: '正在匹配救援资源',
    rescuing: '救援进行中', completed: '救援已完成', cancelled: '救援已取消'
  }
  return map[rescueStatus.value] || '等待处理'
})

const statusSubText = computed(() => {
  const map: Record<string, string> = {
    pending: '请保持电话畅通', matching: '正在搜索附近志愿者和设备...',
    rescuing: '志愿者正在赶往现场', completed: '感谢您的使用', cancelled: ''
  }
  return map[rescueStatus.value] || ''
})

// 时间线步骤
const timelineSteps = computed(() => {
  const data = rescueData.value
  const steps = [
    { label: '已发起', done: true, active: false, current: false, time: data.createTime },
    { label: '匹配中', done: false, active: false, current: false, time: '' },
    { label: '救援中', done: false, active: false, current: false, time: '' },
    { label: '已完成', done: false, active: false, current: false, time: data.completeTime || '' }
  ]
  const statusOrder: Record<string, number> = {
    pending: 0, matching: 1, rescuing: 2, completed: 3, cancelled: 1
  }
  const currentIdx = statusOrder[rescueStatus.value] || 0
  steps.forEach((step, idx) => {
    if (idx < currentIdx) { step.done = true; step.active = true }
    else if (idx === currentIdx) { step.active = true; step.current = true }
  })
  if (rescueStatus.value === 'completed') {
    steps[3].done = true; steps[3].active = true; steps[3].current = true
  }
  return steps
})

// 获取设备名称
function getDeviceName(deviceId: string | null) {
  if (!deviceId) return '--'
  const mobile = mockMobileDevices.find(d => d.id === deviceId)
  if (mobile) return mobile.name
  const fixed = mockFixedDevices.find(d => d.id === deviceId)
  if (fixed) return fixed.name
  return '便携AED设备'
}

// 紧急等级标签
function urgencyLabel(urgency: string) {
  const map: Record<string, string> = { critical: '危急', high: '紧急', medium: '一般' }
  return map[urgency] || urgency
}

// 加载高德地图
const loadAMap = () => {
  return new Promise((resolve) => {
    if ((window as any).AMap) { resolve(true); return }
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '2b1374475410bd35525b1e1770ad69d1' }
    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/maps?v=2.0&key=d8ce027a3ead033d535a5a99bb81490f'
    script.onload = () => resolve(true)
    document.head.appendChild(script)
  })
}

// 初始化地图
async function initMap() {
  await loadAMap()
  const AMap = (window as any).AMap

  // 创建地图实例，以呼救者位置为中心
  mapInstance = new AMap.Map('rescue-map-container', {
    zoom: 15,
    center: [myLng.value, myLat.value],
    mapStyle: 'amap://styles/whitesmoke',
    resizeEnable: true
  })

  // 添加呼救者标记（蓝色）
  selfMarker = new AMap.Marker({
    position: [myLng.value, myLat.value],
    content: '<div style="width:24px;height:24px;border-radius:50%;background:#2B6FF0;border:3px solid #fff;box-shadow:0 2px 8px rgba(43,111,240,0.5);"></div>',
    offset: new AMap.Pixel(-12, -12),
    zIndex: 120,
    title: '我的位置'
  })
  mapInstance.add(selfMarker)

  // 呼救者标记脉冲效果
  const selfPulse = new AMap.Marker({
    position: [myLng.value, myLat.value],
    content: '<div style="width:48px;height:48px;border-radius:50%;border:2px solid rgba(43,111,240,0.3);animation:mapPulse 2s ease-out infinite;"></div>',
    offset: new AMap.Pixel(-24, -24),
    zIndex: 110
  })
  mapInstance.add(selfPulse)

  // 如果有志愿者，添加志愿者标记（绿色）和路线
  if (rescueData.value.matchedVolunteer) {
    addVolunteerMarker()
    addRouteLine()
  }

  // 添加地图样式动画
  const styleEl = document.createElement('style')
  styleEl.textContent = `@keyframes mapPulse { 0% { transform: scale(0.8); opacity: 1; } 100% { transform: scale(2); opacity: 0; } }`
  document.head.appendChild(styleEl)

  // 启动定时刷新位置
  startLocationRefresh()
}

// 添加志愿者标记
function addVolunteerMarker() {
  if (!mapInstance || !(window as any).AMap) return
  const AMap = (window as any).AMap

  if (volunteerMarker) mapInstance.remove(volunteerMarker)

  volunteerMarker = new AMap.Marker({
    position: [volunteerLng.value, volunteerLat.value],
    content: '<div style="width:24px;height:24px;border-radius:50%;background:#00B42A;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,180,42,0.5);position:relative;"><div style="position:absolute;top:-8px;left:-8px;width:40px;height:40px;border-radius:50%;border:2px solid rgba(0,180,42,0.3);animation:mapPulse 2s ease-out infinite;"></div></div>',
    offset: new AMap.Pixel(-12, -12),
    zIndex: 120,
    title: '志愿者位置'
  })
  mapInstance.add(volunteerMarker)
}

// 添加路线
function addRouteLine() {
  if (!mapInstance || !(window as any).AMap) return
  const AMap = (window as any).AMap

  if (routeLine) mapInstance.remove(routeLine)

  routeLine = new AMap.Polyline({
    path: [
      new AMap.LngLat(volunteerLng.value, volunteerLat.value),
      new AMap.LngLat(myLng.value, myLat.value)
    ],
    strokeColor: '#2B6FF0',
    strokeWeight: 4,
    strokeStyle: 'dashed',
    strokeDasharray: [10, 5],
    strokeOpacity: 0.7,
    zIndex: 50
  })
  mapInstance.add(routeLine)
}

// 启动位置定时刷新
function startLocationRefresh() {
  // 每5秒刷新一次位置
  refreshTimer = setInterval(() => {
    simulateLocationUpdate()
  }, 5000)
}

// 模拟位置更新 - 志愿者逐步靠近呼救者
function simulateLocationUpdate() {
  // 计算志愿者与呼救者的距离差
  const dLng = myLng.value - volunteerLng.value
  const dLat = myLat.value - volunteerLat.value

  // 如果距离很近就不再移动
  if (Math.abs(dLng) < 0.0002 && Math.abs(dLat) < 0.0002) {
    volunteerLocationText.value = '已到达现场附近'
    volunteerDistance.value = '<100m'
    return
  }

  // 志愿者向呼救者靠近（每次移动剩余距离的15%）
  volunteerLng.value += dLng * 0.15
  volunteerLat.value += dLat * 0.15

  // 更新地图标记
  if (volunteerMarker) {
    volunteerMarker.setPosition([volunteerLng.value, volunteerLat.value])
  }
  // 更新路线
  if (routeLine) {
    const AMap = (window as any).AMap
    routeLine.setPath([
      new AMap.LngLat(volunteerLng.value, volunteerLat.value),
      new AMap.LngLat(myLng.value, myLat.value)
    ])
  }

  // 计算距离（简化估算）
  const dist = Math.sqrt(dLng * dLng + dLat * dLat) * 111000 // 粗略转为米
  if (dist > 1000) {
    volunteerDistance.value = (dist / 1000).toFixed(1) + 'km'
  } else {
    volunteerDistance.value = Math.round(dist) + 'm'
  }

  // 更新位置文字
  if (dist > 500) {
    volunteerLocationText.value = '正在赶往现场 · ' + volunteerDistance.value
  } else if (dist > 100) {
    volunteerLocationText.value = '即将到达 · ' + volunteerDistance.value
  } else {
    volunteerLocationText.value = '已到达现场附近'
  }

  // 自动调整地图视野包含两个点
  if (mapInstance) {
    mapInstance.setFitView([selfMarker, volunteerMarker], false, [60, 60, 60, 60])
  }
}

// 手动刷新位置
function refreshLocation() {
  locationRefreshing.value = true
  simulateLocationUpdate()
  setTimeout(() => {
    locationRefreshing.value = false
    uni.showToast({ title: '位置已更新', icon: 'none', duration: 1000 })
  }, 500)
}

// 设置评分
function setRating(star: number) {
  if (rescueData.value.rating) return
  rating.value = star
}

// 提交评分
function submitRating() {
  uni.showToast({ title: '评价成功', icon: 'success' })
}

// 拨打电话
function callVolunteer() {
  if (rescueData.value.volunteerPhone) {
    uni.makePhoneCall({ phoneNumber: rescueData.value.volunteerPhone.replace(/\*/g, '0') })
  }
}

// 打开聊天面板
function openChat() {
  showChat.value = true
  scrollChatToBottom()
}

// 关闭聊天面板
function closeChat() {
  showChat.value = false
}

// 发送消息
function sendChatMessage() {
  const text = chatInput.value.trim()
  if (!text) return

  const now = new Date()
  const timeStr = now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0')

  chatMessages.value.push({
    isSelf: true,
    content: text,
    time: timeStr
  })
  chatInput.value = ''
  scrollChatToBottom()

  // 模拟志愿者自动回复
  setTimeout(() => {
    const replies = [
      '收到，我正在赶来！',
      '好的，请保持电话畅通。',
      '我大概还有2分钟到达。',
      '请先保持伤者平躺，不要随意移动。',
      '收到，注意安全！',
      '我已看到您的位置，马上到！'
    ]
    const replyTime = new Date()
    const replyTimeStr = replyTime.getHours().toString().padStart(2, '0') + ':' + replyTime.getMinutes().toString().padStart(2, '0')
    chatMessages.value.push({
      isSelf: false,
      content: replies[Math.floor(Math.random() * replies.length)],
      time: replyTimeStr
    })
    scrollChatToBottom()
  }, 1000 + Math.random() * 2000)
}

// 滚动到底部
function scrollChatToBottom() {
  nextTick(() => {
    chatScrollTop.value = chatScrollTop.value + 999
  })
}

// 生命周期
onMounted(() => {
  // H5环境下初始化地图
  // #ifdef H5
  setTimeout(() => { initMap() }, 300)
  // #endif
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
}

/* 顶部状态区域 */
.status-header {
  padding: 80rpx 32rpx 48rpx;
  position: relative;
  overflow: hidden;
}
.status-bg-pending { background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%); }
.status-bg-matching { background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%); }
.status-bg-rescuing { background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%); }
.status-bg-completed { background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%); }
.status-bg-cancelled { background: linear-gradient(135deg, #86909C 0%, #C9CDD4 100%); }
.status-header-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.status-icon-wrap {
  position: relative;
  width: 120rpx;
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-icon-circle {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}
.status-emoji { font-size: 48rpx; }
.status-pulse {
  position: absolute;
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  animation: statusPulse 2s ease-out infinite;
}
@keyframes statusPulse {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.8); opacity: 0; }
}
.status-main-text {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
  margin-top: 20rpx;
}
.status-sub-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8rpx;
}

/* 滚动内容 */
.scroll-content { min-height: 60vh; }

/* 时间线 */
.timeline-section { padding: 32rpx; }
.timeline {
  display: flex;
  align-items: flex-start;
  padding: 24rpx 16rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.timeline-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.step-line {
  position: absolute;
  top: 24rpx;
  left: -50%;
  width: 100%;
  height: 4rpx;
  background: #E5E6EB;
  z-index: 0;
}
.line-done { background: linear-gradient(90deg, #2B6FF0, #5B8DEF); }
.step-node { position: relative; z-index: 1; }
.step-dot {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #E5E6EB;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step-done .step-dot { background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%); }
.step-current .step-dot {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  box-shadow: 0 0 16rpx rgba(43, 111, 240, 0.4);
}
.step-check { font-size: 24rpx; color: #FFFFFF; font-weight: 700; }
.step-number { font-size: 22rpx; color: #FFFFFF; font-weight: 700; }
.step-number-pending { color: #86909C; }
.step-info { display: flex; flex-direction: column; align-items: center; margin-top: 12rpx; }
.step-title { font-size: 22rpx; color: #C9CDD4; font-weight: 500; }
.step-title-active { color: #1D2129; font-weight: 600; }
.step-time { font-size: 18rpx; color: #86909C; margin-top: 4rpx; }

/* 信息卡片区域 */
.info-section { padding: 0 32rpx 24rpx; }
.section-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.card-header {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.card-header-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
}
.card-icon-volunteer { background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%); }
.card-icon-location { background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%); }
.card-icon-info { background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%); }
.card-icon-rating { background: linear-gradient(135deg, #F53F3F 0%, #FF7D7D 100%); }
.card-header-emoji { font-size: 32rpx; }
.card-header-info { flex: 1; display: flex; flex-direction: column; }
.card-header-title { font-size: 28rpx; font-weight: 600; color: #1D2129; }
.card-header-sub { font-size: 22rpx; color: #86909C; margin-top: 4rpx; }
.card-header-action {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.call-icon { font-size: 28rpx; }
.call-text { font-size: 24rpx; color: #FFFFFF; font-weight: 600; }
.card-body { padding: 8rpx 24rpx 24rpx; }
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #F7F8FA;
}
.info-row:last-child { border-bottom: none; }
.info-row-desc { flex-direction: column; align-items: flex-start; gap: 8rpx; }
.info-label { font-size: 26rpx; color: #86909C; flex-shrink: 0; }
.info-value { font-size: 26rpx; color: #1D2129; font-weight: 500; text-align: right; }
.info-value-highlight { color: #2B6FF0; font-weight: 600; }
.info-value-desc { text-align: left; line-height: 1.6; color: #4E5969; }

/* 刷新按钮 */
.refresh-badge {
  display: flex;
  align-items: center;
  gap: 4rpx;
  padding: 8rpx 16rpx;
  border-radius: 20rpx;
  background: #F2F3F5;
}
.refresh-icon { font-size: 22rpx; }
.refresh-text { font-size: 20rpx; color: #4E5969; }

/* 高德地图容器 */
.rescue-map-wrapper {
  position: relative;
  padding: 0;
}
.rescue-map-container {
  width: 100%;
  height: 500rpx;
}
.map-refresh-tip {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
  z-index: 10;
}
.refresh-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #00B42A;
  animation: dotBlink 1.5s ease-in-out infinite;
}
@keyframes dotBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
.refresh-tip-text { font-size: 20rpx; color: #4E5969; }

/* 位置信息栏 */
.location-info-bar {
  display: flex;
  justify-content: space-around;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid #F2F3F5;
}
.location-info-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.location-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}
.location-dot-self { background: #2B6FF0; box-shadow: 0 0 6rpx rgba(43, 111, 240, 0.4); }
.location-dot-volunteer { background: #00B42A; box-shadow: 0 0 6rpx rgba(0, 180, 42, 0.4); }
.location-info-label { font-size: 22rpx; color: #86909C; }
.location-info-value { font-size: 22rpx; color: #1D2129; font-weight: 500; }

/* 紧急等级标签 */
.urgency-badge { padding: 4rpx 16rpx; border-radius: 8rpx; }
.urgency-badge-critical { background: rgba(245, 63, 63, 0.1); }
.urgency-badge-high { background: rgba(255, 154, 46, 0.1); }
.urgency-badge-medium { background: rgba(43, 111, 240, 0.1); }
.urgency-badge-text { font-size: 24rpx; font-weight: 600; }
.urgency-badge-critical .urgency-badge-text { color: #F53F3F; }
.urgency-badge-high .urgency-badge-text { color: #FF9A2E; }
.urgency-badge-medium .urgency-badge-text { color: #2B6FF0; }

/* 通讯按钮 */
.comm-area { display: flex; gap: 24rpx; padding: 24rpx 32rpx; }
.comm-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 88rpx;
  border-radius: 44rpx;
  transition: all 0.3s ease;
}
.comm-btn:active { transform: scale(0.97); }
.comm-btn-call {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  box-shadow: 0 8rpx 24rpx rgba(43, 111, 240, 0.3);
}
.comm-btn-msg { background: #FFFFFF; border: 2rpx solid #2B6FF0; }
.comm-btn-icon { font-size: 32rpx; }
.comm-btn-text { font-size: 28rpx; font-weight: 600; }
.comm-btn-call .comm-btn-text { color: #FFFFFF; }
.comm-btn-msg .comm-btn-text { color: #2B6FF0; }

/* 评价区域 */
.rating-card { padding-bottom: 32rpx; }
.rating-stars { display: flex; justify-content: center; gap: 16rpx; padding: 32rpx 0 24rpx; }
.star-item { padding: 8rpx; }
.star-icon { font-size: 56rpx; color: #E5E6EB; transition: all 0.2s ease; }
.star-active { color: #FF9A2E; text-shadow: 0 0 16rpx rgba(255, 154, 46, 0.3); }
.rating-submit {
  margin: 0 48rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(43, 111, 240, 0.3);
}
.rating-submit-text { font-size: 28rpx; font-weight: 600; color: #FFFFFF; }
.rating-done { display: flex; flex-direction: column; align-items: center; gap: 8rpx; padding: 16rpx 0; }
.rating-done-stars { display: flex; gap: 8rpx; }
.rating-done-text { font-size: 24rpx; color: #86909C; }

.bottom-safe { height: 60rpx; }

/* 聊天面板 */
.chat-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: #F5F7FA;
}
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.chat-header {
  display: flex;
  align-items: center;
  height: 88rpx;
  padding: 0 24rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.chat-back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-back-icon {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.chat-header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 12rpx;
}
.chat-header-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #FFFFFF;
}
.chat-header-status {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.8);
}
.chat-header-call {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-call-icon {
  font-size: 32rpx;
}
.chat-messages {
  flex: 1;
  padding: 24rpx;
  overflow-y: auto;
}
.chat-msg {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24rpx;
}
.chat-msg-self {
  flex-direction: row-reverse;
}
.chat-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.chat-avatar-self {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.chat-avatar-text {
  font-size: 28rpx;
}
.chat-bubble {
  max-width: 70%;
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  margin: 0 16rpx;
  position: relative;
}
.bubble-other {
  background: #FFFFFF;
  border-top-left-radius: 4rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
}
.bubble-self {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-top-right-radius: 4rpx;
}
.chat-bubble-text {
  font-size: 28rpx;
  line-height: 1.6;
  display: block;
}
.bubble-other .chat-bubble-text {
  color: #1D2129;
}
.bubble-self .chat-bubble-text {
  color: #FFFFFF;
}
.chat-bubble-time {
  font-size: 20rpx;
  display: block;
  margin-top: 8rpx;
  text-align: right;
}
.bubble-other .chat-bubble-time {
  color: #C9CDD4;
}
.bubble-self .chat-bubble-time {
  color: rgba(255, 255, 255, 0.7);
}
.chat-input-area {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #FFFFFF;
  border-top: 1rpx solid #F2F3F5;
}
.chat-input-wrap {
  flex: 1;
  height: 72rpx;
  background: #F5F7FA;
  border-radius: 36rpx;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
}
.chat-input {
  width: 100%;
  height: 72rpx;
  font-size: 28rpx;
  color: #1D2129;
}
.chat-input-placeholder {
  color: #C9CDD4;
  font-size: 28rpx;
}
.chat-send-btn {
  padding: 16rpx 32rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-radius: 36rpx;
  flex-shrink: 0;
}
.chat-send-btn:active {
  opacity: 0.8;
}
.chat-send-text {
  font-size: 26rpx;
  color: #FFFFFF;
  font-weight: 600;
}
</style>
