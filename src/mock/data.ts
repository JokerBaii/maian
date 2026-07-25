// Mock数据 - 用户信息
export const mockUser = {
  id: 'U001',
  nickname: '张明',
  avatar: '/static/images/avatar.png',
  phone: '138****6789',
  role: 'volunteer', // normal | volunteer | admin | superAdmin
  realName: '张明',
  idCard: '3301****1234',
  isVerified: true,
  emergencyContacts: [
    { name: '李芳', phone: '139****5678', relation: '配偶' },
    { name: '张伟', phone: '137****4321', relation: '父亲' },
    { name: '王小红', phone: '136****9012', relation: '朋友' }
  ],
  bindDevice: {
    name: '华为手环 Band 8',
    type: 'band',
    connected: true,
    battery: 78
  },
  createTime: '2025-03-15'
}

// Mock数据 - 固定急救设备（扩充到12个）
export const mockFixedDevices = [
  {
    id: 'D001', type: 'fixed', category: 'AED',
    name: '浙江大学紫金港校区AED-01',
    address: '杭州市西湖区浙大路38号紫金港校区体育馆入口',
    longitude: 120.0869, latitude: 30.3055,
    status: 'available', expireDate: '2026-12-31',
    image: '/static/images/device-aed.png',
    owner: '浙江大学校医院', ownerPhone: '0571-8820****',
    instructions: '1.打开设备箱门 2.按语音提示操作 3.贴电极片 4.等待分析',
    shareScope: 'campus', createTime: '2025-01-10'
  },
  {
    id: 'D002', type: 'fixed', category: 'AED',
    name: '西湖文化广场AED-01',
    address: '杭州市下城区中山北路口西湖文化广场B1层',
    longitude: 120.1633, latitude: 30.2794,
    status: 'available', expireDate: '2026-06-30',
    image: '/static/images/device-aed.png',
    owner: '下城区红十字会', ownerPhone: '0571-8510****',
    instructions: '1.打开设备箱门 2.按语音提示操作 3.贴电极片 4.等待分析',
    shareScope: 'public', createTime: '2025-02-15'
  },
  {
    id: 'D003', type: 'fixed', category: '急救箱',
    name: '城西银泰城急救箱-01',
    address: '杭州市拱墅区萍水街城西银泰城1楼服务台',
    longitude: 120.1198, latitude: 30.2942,
    status: 'available', expireDate: '2026-09-30',
    image: '/static/images/device-kit.png',
    owner: '城西银泰城物业', ownerPhone: '0571-8800****',
    instructions: '包含绷带、消毒液、止血贴、三角巾等基础急救用品',
    shareScope: 'public', createTime: '2025-03-01'
  },
  {
    id: 'D004', type: 'fixed', category: 'AED',
    name: '杭州火车东站AED-03',
    address: '杭州市江干区天城路1号东站候车大厅B区',
    longitude: 120.2133, latitude: 30.2907,
    status: 'maintenance', expireDate: '2026-03-15',
    image: '/static/images/device-aed.png',
    owner: '铁路杭州站', ownerPhone: '0571-5670****',
    instructions: '1.打开设备箱门 2.按语音提示操作 3.贴电极片 4.等待分析',
    shareScope: 'public', createTime: '2024-12-20'
  },
  {
    id: 'D005', type: 'fixed', category: 'AED',
    name: '西溪湿地国家公园AED-01',
    address: '杭州市西湖区天目山路518号西溪湿地周家村入口',
    longitude: 120.0627, latitude: 30.2674,
    status: 'available', expireDate: '2027-01-31',
    image: '/static/images/device-aed.png',
    owner: '西溪湿地管委会', ownerPhone: '0571-8810****',
    instructions: '1.打开设备箱门 2.按语音提示操作 3.贴电极片 4.等待分析',
    shareScope: 'public', createTime: '2025-04-10'
  },
  {
    id: 'D006', type: 'fixed', category: '急救箱',
    name: '浙江工业大学朝晖校区急救箱-01',
    address: '杭州市拱墅区潮王路18号工大体育馆',
    longitude: 120.1487, latitude: 30.2863,
    status: 'available', expireDate: '2026-08-31',
    image: '/static/images/device-kit.png',
    owner: '浙江工业大学校医院', ownerPhone: '0571-8832****',
    instructions: '包含绷带、消毒液、止血贴、三角巾等基础急救用品',
    shareScope: 'campus', createTime: '2025-05-01'
  },
  {
    id: 'D007', type: 'fixed', category: 'AED',
    name: '武林广场地铁站AED-02',
    address: '杭州市下城区武林广场地铁站B1层客服中心旁',
    longitude: 120.1747, latitude: 30.2753,
    status: 'available', expireDate: '2026-11-30',
    image: '/static/images/device-aed.png',
    owner: '杭州地铁集团', ownerPhone: '0571-96600',
    instructions: '1.打开设备箱门 2.按语音提示操作 3.贴电极片 4.等待分析',
    shareScope: 'public', createTime: '2025-01-25'
  },
  {
    id: 'D008', type: 'fixed', category: '急救包',
    name: '钱江新城市民中心急救包-01',
    address: '杭州市江干区新业路311号市民中心L楼1层大厅',
    longitude: 120.2128, latitude: 30.2547,
    status: 'available', expireDate: '2027-03-31',
    image: '/static/images/device-kit.png',
    owner: '市民中心物业', ownerPhone: '0571-8700****',
    instructions: '包含止血带、绷带、消毒液、急救剪刀、三角巾等',
    shareScope: 'public', createTime: '2025-02-20'
  },
  {
    id: 'D009', type: 'fixed', category: 'AED',
    name: '萧山国际机场T3航站楼AED-04',
    address: '杭州市萧山区空港大道1号T3航站楼出发层8号门',
    longitude: 120.4317, latitude: 30.2358,
    status: 'available', expireDate: '2026-10-31',
    image: '/static/images/device-aed.png',
    owner: '萧山国际机场', ownerPhone: '0571-8666****',
    instructions: '1.打开设备箱门 2.按语音提示操作 3.贴电极片 4.等待分析',
    shareScope: 'public', createTime: '2025-03-08'
  },
  {
    id: 'D010', type: 'fixed', category: '急救箱',
    name: '浙江大学玉泉校区急救箱-02',
    address: '杭州市西湖区浙大路38号玉泉校区图书馆一楼',
    longitude: 120.1033, latitude: 30.2627,
    status: 'expired', expireDate: '2025-05-01',
    image: '/static/images/device-kit.png',
    owner: '浙江大学校医院', ownerPhone: '0571-8820****',
    instructions: '包含绷带、消毒液、止血贴、三角巾等基础急救用品（已过期待更换）',
    shareScope: 'campus', createTime: '2024-06-01'
  },
  {
    id: 'D011', type: 'fixed', category: 'AED',
    name: '湖滨银泰in77 AED-01',
    address: '杭州市上城区延安路98号银泰in77A区1楼客服中心',
    longitude: 120.1683, latitude: 30.2531,
    status: 'available', expireDate: '2027-02-28',
    image: '/static/images/device-aed.png',
    owner: '湖滨银泰物业', ownerPhone: '0571-8716****',
    instructions: '1.打开设备箱门 2.按语音提示操作 3.贴电极片 4.等待分析',
    shareScope: 'public', createTime: '2025-05-10'
  },
  {
    id: 'D012', type: 'fixed', category: '急救包',
    name: '杭州大剧院急救包-01',
    address: '杭州市江干区新业路39号杭州大剧院1楼大厅',
    longitude: 120.2097, latitude: 30.2511,
    status: 'available', expireDate: '2026-07-31',
    image: '/static/images/device-kit.png',
    owner: '杭州大剧院', ownerPhone: '0571-8685****',
    instructions: '包含止血带、绷带、消毒液、急救剪刀、冰袋等',
    shareScope: 'public', createTime: '2025-04-22'
  }
]

// Mock数据 - 移动急救设备（扩充到8个）
export const mockMobileDevices = [
  {
    id: 'M001', type: 'mobile', category: 'AED',
    name: '车载便携AED-王师傅',
    address: '实时位置·西湖区', longitude: 120.13, latitude: 30.27,
    status: 'online', online: true, expireDate: '2026-12-31',
    image: '/static/images/device-aed.png',
    owner: '王建国', ownerPhone: '135****7890',
    vehicleInfo: '浙A·8K923 白色SUV', serviceRange: 5, serviceTime: '全天',
    instructions: '1.打开设备箱 2.按语音提示操作 3.贴电极片 4.等待分析',
    createTime: '2025-02-01'
  },
  {
    id: 'M002', type: 'mobile', category: '急救包',
    name: '车载急救包-李女士',
    address: '实时位置·拱墅区', longitude: 120.15, latitude: 30.32,
    status: 'online', online: true, expireDate: '2026-09-30',
    image: '/static/images/device-kit.png',
    owner: '李雪梅', ownerPhone: '136****4567',
    vehicleInfo: '浙A·3M567 蓝色轿车', serviceRange: 3, serviceTime: '工作日 8:00-18:00',
    instructions: '包含止血带、绷带、消毒用品、急救剪刀等',
    createTime: '2025-03-10'
  },
  {
    id: 'M003', type: 'mobile', category: 'AED',
    name: '车载便携AED-赵先生',
    address: '实时位置·江干区', longitude: 120.20, latitude: 30.25,
    status: 'online', online: true, expireDate: '2027-03-31',
    image: '/static/images/device-aed.png',
    owner: '赵志远', ownerPhone: '137****8901',
    vehicleInfo: '浙A·6P234 黑色MPV', serviceRange: 8, serviceTime: '全天',
    instructions: '1.打开设备箱 2.按语音提示操作 3.贴电极片 4.等待分析',
    createTime: '2025-01-20'
  },
  {
    id: 'M004', type: 'mobile', category: '急救包',
    name: '车载急救包-陈师傅',
    address: '实时位置·滨江区', longitude: 120.18, latitude: 30.21,
    status: 'offline', online: false, expireDate: '2026-06-30',
    image: '/static/images/device-kit.png',
    owner: '陈大海', ownerPhone: '138****2345',
    vehicleInfo: '浙A·2N891 银色面包车', serviceRange: 5, serviceTime: '工作日 9:00-17:00',
    instructions: '包含止血带、绷带、消毒用品、急救剪刀等',
    createTime: '2025-04-05'
  },
  {
    id: 'M005', type: 'mobile', category: 'AED',
    name: '车载便携AED-周医生',
    address: '实时位置·西湖区', longitude: 120.11, latitude: 30.26,
    status: 'online', online: true, expireDate: '2027-06-30',
    image: '/static/images/device-aed.png',
    owner: '周明辉', ownerPhone: '139****6789',
    vehicleInfo: '浙A·5W123 黑色轿车', serviceRange: 10, serviceTime: '全天',
    instructions: '1.打开设备箱 2.按语音提示操作 3.贴电极片 4.等待分析',
    createTime: '2025-02-18'
  },
  {
    id: 'M006', type: 'mobile', category: '急救包',
    name: '车载急救包-孙先生',
    address: '实时位置·下城区', longitude: 120.17, latitude: 30.29,
    status: 'online', online: true, expireDate: '2026-12-31',
    image: '/static/images/device-kit.png',
    owner: '孙志强', ownerPhone: '158****3456',
    vehicleInfo: '浙A·9H456 红色SUV', serviceRange: 5, serviceTime: '周末全天',
    instructions: '包含止血带、绷带、消毒用品、夹板、三角巾等',
    createTime: '2025-03-25'
  },
  {
    id: 'M007', type: 'mobile', category: 'AED',
    name: '车载便携AED-吴女士',
    address: '实时位置·上城区', longitude: 120.19, latitude: 30.24,
    status: 'offline', online: false, expireDate: '2026-08-31',
    image: '/static/images/device-aed.png',
    owner: '吴婷婷', ownerPhone: '186****7890',
    vehicleInfo: '浙A·1T789 白色轿车', serviceRange: 3, serviceTime: '工作日 9:00-18:00',
    instructions: '1.打开设备箱 2.按语音提示操作 3.贴电极片 4.等待分析',
    createTime: '2025-04-12'
  },
  {
    id: 'M008', type: 'mobile', category: '急救包',
    name: '车载急救包-刘师傅',
    address: '实时位置·萧山区', longitude: 120.26, latitude: 30.18,
    status: 'online', online: true, expireDate: '2027-01-31',
    image: '/static/images/device-kit.png',
    owner: '刘国强', ownerPhone: '137****2345',
    vehicleInfo: '浙A·7R321 蓝色商务车', serviceRange: 8, serviceTime: '全天',
    instructions: '包含止血带、绷带、消毒用品、急救剪刀、冰袋等',
    createTime: '2025-05-08'
  }
]

// Mock数据 - 紧急呼救记录（扩充到8条）
export const mockRescueCalls = [
  {
    id: 'R001', userId: 'U002', userName: '刘小红', userPhone: '139****3456',
    longitude: 120.15, latitude: 30.28,
    address: '杭州市西湖区文三路268号',
    description: '同事突然晕倒，疑似心脏骤停，需要AED设备',
    urgency: 'critical', images: ['/static/images/rescue-scene1.png'],
    status: 'rescuing',
    matchedDevice: 'M001', matchedVolunteer: '王建国', volunteerPhone: '135****7890',
    createTime: '2025-06-15 14:32:00', completeTime: null, rating: null
  },
  {
    id: 'R002', userId: 'U003', userName: '孙大伟', userPhone: '137****7890',
    longitude: 120.10, latitude: 30.26,
    address: '杭州市西湖区古翠路60号',
    description: '老人摔倒头部出血，需要急救包',
    urgency: 'high', images: [],
    status: 'completed',
    matchedDevice: 'D003', matchedVolunteer: '李雪梅', volunteerPhone: '136****4567',
    createTime: '2025-06-10 09:15:00', completeTime: '2025-06-10 09:28:00', rating: 5
  },
  {
    id: 'R003', userId: 'U004', userName: '周丽', userPhone: '136****5678',
    longitude: 120.21, latitude: 30.29,
    address: '杭州市江干区景芳五区',
    description: '小孩误吞异物，需要急救指导',
    urgency: 'medium', images: [],
    status: 'completed',
    matchedDevice: null, matchedVolunteer: '赵志远', volunteerPhone: '137****8901',
    createTime: '2025-05-28 16:45:00', completeTime: '2025-05-28 17:00:00', rating: 4
  },
  {
    id: 'R004', userId: 'U005', userName: '陈小明', userPhone: '159****1234',
    longitude: 120.12, latitude: 30.25,
    address: '杭州市西湖区文二西路88号',
    description: '运动时突然胸闷气短，心率异常加速',
    urgency: 'high', images: [],
    status: 'completed',
    matchedDevice: 'M005', matchedVolunteer: '周明辉', volunteerPhone: '139****6789',
    createTime: '2025-05-15 08:20:00', completeTime: '2025-05-15 08:35:00', rating: 5
  },
  {
    id: 'R005', userId: 'U006', userName: '李阿姨', userPhone: '138****5678',
    longitude: 120.17, latitude: 30.28,
    address: '杭州市下城区朝晖路166号',
    description: '邻居老人跌倒骨折，无法移动，需要急救包固定',
    urgency: 'high', images: [],
    status: 'completed',
    matchedDevice: 'M006', matchedVolunteer: '孙志强', volunteerPhone: '158****3456',
    createTime: '2025-04-22 11:10:00', completeTime: '2025-04-22 11:32:00', rating: 5
  },
  {
    id: 'R006', userId: 'U007', userName: '王小明', userPhone: '137****9012',
    longitude: 120.20, latitude: 30.26,
    address: '杭州市江干区九堡镇格畈家园',
    description: '小孩高烧抽搐，需要紧急降温指导',
    urgency: 'critical', images: [],
    status: 'completed',
    matchedDevice: null, matchedVolunteer: '赵志远', volunteerPhone: '137****8901',
    createTime: '2025-04-08 22:15:00', completeTime: '2025-04-08 22:40:00', rating: 4
  },
  {
    id: 'R007', userId: 'U008', userName: '赵女士', userPhone: '136****3456',
    longitude: 120.14, latitude: 30.23,
    address: '杭州市西湖区之江路168号',
    description: '家人食物中毒呕吐不止，需要急救指导',
    urgency: 'high', images: [],
    status: 'completed',
    matchedDevice: 'D005', matchedVolunteer: '王建国', volunteerPhone: '135****7890',
    createTime: '2025-03-30 19:05:00', completeTime: '2025-03-30 19:22:00', rating: 5
  },
  {
    id: 'R008', userId: 'U009', userName: '张先生', userPhone: '139****7890',
    longitude: 120.16, latitude: 30.30,
    address: '杭州市拱墅区大关路98号',
    description: '同事在工作中被割伤出血较多，需要止血包扎',
    urgency: 'medium', images: [],
    status: 'completed',
    matchedDevice: 'D006', matchedVolunteer: '李雪梅', volunteerPhone: '136****4567',
    createTime: '2025-03-12 14:48:00', completeTime: '2025-03-12 15:05:00', rating: 4
  }
]

// Mock数据 - 心率监测（扩充30天月数据 + 更丰富的小时数据）
export const mockHeartRateData = {
  current: 72,
  min: 48,
  max: 142,
  avg: 73,
  status: 'normal',
  scene: 'resting',
  todayData: [
    { time: '00:00', value: 62, scene: 'sleeping' },
    { time: '00:30', value: 60, scene: 'sleeping' },
    { time: '01:00', value: 58, scene: 'sleeping' },
    { time: '01:30', value: 56, scene: 'sleeping' },
    { time: '02:00', value: 55, scene: 'sleeping' },
    { time: '02:30', value: 54, scene: 'sleeping' },
    { time: '03:00', value: 53, scene: 'sleeping' },
    { time: '03:30', value: 55, scene: 'sleeping' },
    { time: '04:00', value: 57, scene: 'sleeping' },
    { time: '04:30', value: 58, scene: 'sleeping' },
    { time: '05:00', value: 60, scene: 'sleeping' },
    { time: '05:30', value: 63, scene: 'resting' },
    { time: '06:00', value: 65, scene: 'resting' },
    { time: '06:30', value: 68, scene: 'resting' },
    { time: '07:00', value: 72, scene: 'resting' },
    { time: '07:30', value: 76, scene: 'resting' },
    { time: '08:00', value: 78, scene: 'resting' },
    { time: '08:30', value: 82, scene: 'resting' },
    { time: '09:00', value: 85, scene: 'exercise' },
    { time: '09:30', value: 98, scene: 'exercise' },
    { time: '10:00', value: 112, scene: 'exercise' },
    { time: '10:30', value: 125, scene: 'exercise' },
    { time: '11:00', value: 95, scene: 'exercise' },
    { time: '11:30', value: 88, scene: 'resting' },
    { time: '12:00', value: 80, scene: 'resting' },
    { time: '12:30', value: 78, scene: 'resting' },
    { time: '13:00', value: 75, scene: 'resting' },
    { time: '13:30', value: 73, scene: 'resting' },
    { time: '14:00', value: 72, scene: 'resting' },
    { time: '14:30', value: 70, scene: 'resting' },
    { time: '15:00', value: 70, scene: 'resting' },
    { time: '15:30', value: 69, scene: 'resting' },
    { time: '16:00', value: 68, scene: 'resting' },
    { time: '16:30', value: 75, scene: 'resting' },
    { time: '17:00', value: 82, scene: 'exercise' },
    { time: '17:30', value: 95, scene: 'exercise' },
    { time: '18:00', value: 90, scene: 'exercise' },
    { time: '18:30', value: 85, scene: 'exercise' },
    { time: '19:00', value: 76, scene: 'resting' },
    { time: '19:30', value: 74, scene: 'resting' },
    { time: '20:00', value: 74, scene: 'resting' },
    { time: '20:30', value: 72, scene: 'resting' },
    { time: '21:00', value: 72, scene: 'resting' },
    { time: '21:30', value: 70, scene: 'resting' },
    { time: '22:00', value: 68, scene: 'resting' },
    { time: '22:30', value: 66, scene: 'sleeping' },
    { time: '23:00', value: 65, scene: 'sleeping' },
    { time: '23:30', value: 63, scene: 'sleeping' }
  ],
  weekData: [
    { date: '06-24', avg: 71, min: 52, max: 108 },
    { date: '06-25', avg: 73, min: 55, max: 112 },
    { date: '06-26', avg: 69, min: 48, max: 105 },
    { date: '06-27', avg: 75, min: 54, max: 118 },
    { date: '06-28', avg: 72, min: 50, max: 110 },
    { date: '06-29', avg: 70, min: 53, max: 102 },
    { date: '06-30', avg: 73, min: 51, max: 115 }
  ],
  monthData: [
    { date: '06-01', avg: 72, min: 54, max: 110 },
    { date: '06-02', avg: 71, min: 52, max: 108 },
    { date: '06-03', avg: 74, min: 56, max: 116 },
    { date: '06-04', avg: 70, min: 53, max: 106 },
    { date: '06-05', avg: 68, min: 50, max: 102 },
    { date: '06-06', avg: 73, min: 55, max: 112 },
    { date: '06-07', avg: 75, min: 58, max: 120 },
    { date: '06-08', avg: 77, min: 56, max: 142 },
    { date: '06-09', avg: 72, min: 54, max: 110 },
    { date: '06-10', avg: 70, min: 52, max: 105 },
    { date: '06-11', avg: 71, min: 51, max: 108 },
    { date: '06-12', avg: 69, min: 49, max: 104 },
    { date: '06-13', avg: 74, min: 55, max: 118 },
    { date: '06-14', avg: 76, min: 57, max: 122 },
    { date: '06-15', avg: 73, min: 53, max: 112 },
    { date: '06-16', avg: 71, min: 52, max: 108 },
    { date: '06-17', avg: 70, min: 54, max: 106 },
    { date: '06-18', avg: 72, min: 50, max: 110 },
    { date: '06-19', avg: 75, min: 56, max: 120 },
    { date: '06-20', avg: 73, min: 55, max: 114 },
    { date: '06-21', avg: 78, min: 58, max: 135 },
    { date: '06-22', avg: 74, min: 54, max: 115 },
    { date: '06-23', avg: 72, min: 52, max: 110 },
    { date: '06-24', avg: 71, min: 52, max: 108 },
    { date: '06-25', avg: 73, min: 55, max: 112 },
    { date: '06-26', avg: 69, min: 48, max: 105 },
    { date: '06-27', avg: 75, min: 54, max: 118 },
    { date: '06-28', avg: 72, min: 50, max: 110 },
    { date: '06-29', avg: 70, min: 53, max: 102 },
    { date: '06-30', avg: 73, min: 51, max: 115 }
  ],
  alerts: [
    { time: '2025-06-27 10:15', value: 118, type: 'high', message: '心率偏高，请注意休息' },
    { time: '2025-06-25 06:30', value: 48, type: 'low', message: '心率偏低，如持续请就医' },
    { time: '2025-06-21 09:45', value: 135, type: 'high', message: '运动中心率过高，请降低运动强度' },
    { time: '2025-06-14 17:20', value: 122, type: 'high', message: '心率偏高，建议停止运动休息' },
    { time: '2025-06-08 10:30', value: 142, type: 'high', message: '心率严重偏高！请立即停止活动并休息' },
    { time: '2025-06-03 23:50', value: 49, type: 'low', message: '睡眠中心率偏低，如伴有不适请就医' },
    { time: '2025-05-28 05:15', value: 48, type: 'low', message: '心率偏低，如持续请就医' },
    { time: '2025-05-20 18:40', value: 128, type: 'high', message: '运动中心率偏高，请注意控制强度' }
  ]
}

// Mock数据 - 体检报告（扩充更多指标）
export const mockCheckupReport = {
  id: 'H001',
  userId: 'U001',
  checkupDate: '2025-06-01',
  hospital: '浙江大学医学院附属第一医院',
  ocrResult: {
    items: [
      { name: '白细胞计数', value: '6.8', unit: '×10⁹/L', refRange: '3.5-9.5', status: 'normal' },
      { name: '红细胞计数', value: '4.2', unit: '×10¹²/L', refRange: '4.3-5.8', status: 'low' },
      { name: '血红蛋白', value: '128', unit: 'g/L', refRange: '130-175', status: 'low' },
      { name: '血小板计数', value: '220', unit: '×10⁹/L', refRange: '125-350', status: 'normal' },
      { name: '中性粒细胞比率', value: '62.3', unit: '%', refRange: '40-75', status: 'normal' },
      { name: '淋巴细胞比率', value: '28.5', unit: '%', refRange: '20-50', status: 'normal' },
      { name: '单核细胞比率', value: '6.2', unit: '%', refRange: '3-10', status: 'normal' },
      { name: '谷丙转氨酶(ALT)', value: '25', unit: 'U/L', refRange: '9-50', status: 'normal' },
      { name: '谷草转氨酶(AST)', value: '22', unit: 'U/L', refRange: '15-40', status: 'normal' },
      { name: '谷氨酰转肽酶(GGT)', value: '38', unit: 'U/L', refRange: '10-60', status: 'normal' },
      { name: '总胆红素', value: '15.2', unit: 'μmol/L', refRange: '5.1-28.0', status: 'normal' },
      { name: '直接胆红素', value: '4.8', unit: 'μmol/L', refRange: '0-8.0', status: 'normal' },
      { name: '总蛋白', value: '72', unit: 'g/L', refRange: '65-85', status: 'normal' },
      { name: '白蛋白', value: '45', unit: 'g/L', refRange: '40-55', status: 'normal' },
      { name: '总胆固醇', value: '5.8', unit: 'mmol/L', refRange: '2.8-5.2', status: 'high' },
      { name: '甘油三酯', value: '1.6', unit: 'mmol/L', refRange: '0.56-1.70', status: 'normal' },
      { name: '高密度脂蛋白', value: '1.05', unit: 'mmol/L', refRange: '1.04-1.55', status: 'normal' },
      { name: '低密度脂蛋白', value: '3.8', unit: 'mmol/L', refRange: '1.5-3.4', status: 'high' },
      { name: '空腹血糖', value: '5.2', unit: 'mmol/L', refRange: '3.9-6.1', status: 'normal' },
      { name: '糖化血红蛋白', value: '5.4', unit: '%', refRange: '4.0-6.0', status: 'normal' },
      { name: '肌酐', value: '78', unit: 'μmol/L', refRange: '57-111', status: 'normal' },
      { name: '尿素氮', value: '5.2', unit: 'mmol/L', refRange: '3.1-8.0', status: 'normal' },
      { name: '尿酸', value: '420', unit: 'μmol/L', refRange: '208-428', status: 'high' },
      { name: '促甲状腺激素(TSH)', value: '2.1', unit: 'mIU/L', refRange: '0.27-4.2', status: 'normal' },
      { name: '游离T3', value: '4.8', unit: 'pmol/L', refRange: '3.1-6.8', status: 'normal' },
      { name: '游离T4', value: '16.2', unit: 'pmol/L', refRange: '12.0-22.0', status: 'normal' }
    ]
  },
  aiAnalysis: {
    summary: '您的体检报告整体状况良好，但存在轻度贫血倾向和血脂偏高问题，需关注并调整生活方式。共检测26项指标，其中4项异常（偏低2项、偏高2项），22项正常。',
    riskLevel: 'medium',
    abnormalItems: [
      {
        name: '红细胞计数偏低',
        value: '4.2 ×10¹²/L',
        refRange: '4.3-5.8 ×10¹²/L',
        riskLevel: 'low',
        analysis: '红细胞计数略低于正常范围下限，结合血红蛋白也偏低，提示轻度贫血倾向。可能与营养摄入不均衡、铁元素缺乏有关。青年男性轻度贫血需排除消化道慢性失血等可能。',
        suggestions: ['增加红肉、动物肝脏等富含铁元素的食物摄入', '多食用维生素C丰富的蔬果促进铁吸收', '建议3个月后复查血常规，如持续偏低需进一步检查']
      },
      {
        name: '血红蛋白偏低',
        value: '128 g/L',
        refRange: '130-175 g/L',
        riskLevel: 'low',
        analysis: '血红蛋白略低于正常值，属于轻度贫血，通常不会引起明显症状，但长期偏低可能影响身体供氧能力，出现乏力、头晕等表现。',
        suggestions: ['补充含铁丰富的食物', '避免饮浓茶影响铁吸收', '定期监测血红蛋白变化', '适当增加蛋白质摄入']
      },
      {
        name: '总胆固醇偏高',
        value: '5.8 mmol/L',
        refRange: '2.8-5.2 mmol/L',
        riskLevel: 'medium',
        analysis: '总胆固醇超出正常范围，结合低密度脂蛋白也偏高（3.8 mmol/L），是心血管疾病的危险因素。您日常心率监测显示运动时心率峰值偏高（最高142BPM），需警惕心血管风险。长期高血脂可导致动脉粥样硬化，增加心脑血管事件风险。',
        suggestions: ['减少高胆固醇食物摄入（动物内脏、蛋黄等）', '增加有氧运动，每周至少150分钟', '控制体重，避免肥胖', '建议3个月后复查血脂四项', '如复查仍偏高，建议心内科就诊评估是否需药物治疗']
      },
      {
        name: '低密度脂蛋白偏高',
        value: '3.8 mmol/L',
        refRange: '1.5-3.4 mmol/L',
        riskLevel: 'medium',
        analysis: '低密度脂蛋白（"坏胆固醇"）偏高，与总胆固醇升高一致，是动脉粥样硬化的主要危险因素。需与总胆固醇协同管控。',
        suggestions: ['严格控制饱和脂肪和反式脂肪摄入', '增加膳食纤维摄入（全谷物、蔬菜、水果）', '坚持规律有氧运动', '戒烟限酒']
      },
      {
        name: '尿酸偏高',
        value: '420 μmol/L',
        refRange: '208-428 μmol/L',
        riskLevel: 'low',
        analysis: '尿酸处于正常上限附近，虽未明显超标，但需注意饮食控制，防止进一步升高导致痛风风险。高尿酸也与代谢综合征相关。',
        suggestions: ['减少高嘌呤食物（海鲜、啤酒、动物内脏）', '多饮水促进尿酸排泄（每日2000ml以上）', '限制酒精摄入', '定期监测尿酸水平']
      }
    ],
    overallSuggestions: [
      '饮食方面：增加蛋白质和铁元素摄入，减少高脂高嘌呤食物，多食蔬果和全谷物',
      '运动方面：保持规律有氧运动（快走、游泳、骑行），每周5次、每次30分钟以上，控制运动强度避免心率过快',
      '作息方面：保证7-8小时充足睡眠，避免熬夜，减少精神压力',
      '饮水方面：每日饮水2000ml以上，促进代谢废物排泄',
      '复查建议：3个月后复查血常规和血脂四项，持续监测相关指标变化，如血脂持续偏高建议心内科就诊'
    ],
    analyzeTime: '2025-06-02 10:30:00'
  }
}

// Mock数据 - 健康档案历史（扩充到6条）
export const mockHealthArchive = [
  { id: 'H001', date: '2025-06-01', hospital: '浙大一院', abnormalCount: 5, riskLevel: 'medium' },
  { id: 'H002', date: '2025-01-15', hospital: '浙江省人民医院', abnormalCount: 2, riskLevel: 'low' },
  { id: 'H003', date: '2024-07-20', hospital: '杭州市第一人民医院', abnormalCount: 1, riskLevel: 'low' },
  { id: 'H004', date: '2024-01-08', hospital: '浙大二院', abnormalCount: 3, riskLevel: 'medium' },
  { id: 'H005', date: '2023-06-12', hospital: '邵逸夫医院', abnormalCount: 0, riskLevel: 'low' },
  { id: 'H006', date: '2023-01-20', hospital: '浙大一院', abnormalCount: 4, riskLevel: 'high' }
]

// Mock数据 - 科普内容（扩充到12条）
export const mockScienceContents = [
  {
    id: 'S001', title: 'AED自动体外除颤器使用教程', category: 'device', categoryLabel: '设备使用',
    cover: '/static/images/science-aed.png', author: '平台官方', authorAvatar: '/static/images/avatar-official.png',
    summary: '详细讲解AED的开箱、电极片粘贴、语音提示跟随操作全流程，人人都能学会的救命技能。',
    content: 'AED（自动体外除颤器）是一种便携式医疗设备，可以诊断特定的心律失常，并给予电击除颤。AED操作简单，非专业人员也能使用...\n\n第一步：打开AED电源\n按下电源按钮或翻开盖子即可自动开机...\n\n第二步：贴电极片\n按照电极片上的图示，将一片贴在右胸上部，另一片贴在左胸下部...\n\n第三步：听从语音提示\nAED会自动分析心律，如需电击，会语音提示"建议除颤"...\n\n第四步：电击\n确保无人接触患者后，按下电击按钮...',
    viewCount: 12580, likeCount: 892, collectCount: 567, publishTime: '2025-03-01', isLiked: false, isCollected: true,
    media: {
      type: 'video',
      url: '/video-proxy/nxgov/masvod/public/2026/01/12/20260112_19bb10227c7_r1_1200k.mp4',
      poster: '/static/images/science-aed.png',
      images: []
    }
  },
  {
    id: 'S002', title: '心肺复苏CPR黄金四分钟', category: 'emergency', categoryLabel: '突发急症',
    cover: '/static/images/science-cpr.png', author: '平台官方', authorAvatar: '/static/images/avatar-official.png',
    summary: '心搏骤停4分钟后脑细胞开始不可逆损伤，掌握CPR正确手法，在黄金时间内挽救生命。',
    content: '心肺复苏（CPR）是针对心搏骤停采取的急救措施...\n\n判断意识：轻拍双肩，大声呼唤\n呼救：拨打120，取AED\n胸外按压：双手交叠，掌根置于胸骨中下1/3处，深度5-6cm，频率100-120次/分\n人工呼吸：30次按压后2次吹气\n持续循环直到专业救援到达...',
    viewCount: 9870, likeCount: 723, collectCount: 445, publishTime: '2025-03-15', isLiked: true, isCollected: false,
    media: {
      type: 'video',
      url: '/video-proxy/gzfuquan/masvod/public/2020/06/10/20200610_1729d9a8e31_r1_500k.mp4',
      poster: '/static/images/science-cpr.png',
      images: []
    }
  },
  {
    id: 'S003', title: '海姆立克急救法：异物卡喉怎么办', category: 'emergency', categoryLabel: '突发急症',
    cover: '/static/images/science-heimlich.png', author: '李医生', authorAvatar: '/static/images/avatar-doctor.png',
    summary: '异物卡喉窒息是常见急症，海姆立克法简单有效，适用于成人和儿童不同场景。',
    content: '海姆立克急救法是通过冲击腹部使膈肌上抬，增加胸腔压力将异物排出...\n\n成人施救：站在患者身后，一手握拳置于肚脐上方，另一手包住拳头，向内向上快速冲击...\n\n儿童施救：跪在儿童身后，使用同样手法但力度减小...\n\n婴儿施救：面朝下放在前臂上，掌根拍击背部5次...',
    viewCount: 7650, likeCount: 534, collectCount: 389, publishTime: '2025-04-01', isLiked: false, isCollected: false,
    media: {
      type: 'video',
      url: '/video-proxy/bcebos/video/video_16444798680122HsmFTQo.mp4',
      poster: '/static/images/science-heimlich.png',
      images: []
    }
  },
  {
    id: 'S004', title: '日常心率监测：了解你的心脏健康', category: 'health', categoryLabel: '健康管理',
    cover: '/static/images/science-heartrate.png', author: '王医生', authorAvatar: '/static/images/avatar-doctor.png',
    summary: '静息心率、运动心率、最大心率分别代表什么？如何通过日常监测发现心脏异常信号？',
    content: '心率是反映心脏健康的重要指标...\n\n静息心率：正常范围60-100次/分，运动员可低至40-50次/分\n运动心率：根据年龄计算最大心率=220-年龄\n异常信号：持续心率过快/过慢、心律不齐、运动后恢复慢...',
    viewCount: 5430, likeCount: 367, collectCount: 234, publishTime: '2025-04-15', isLiked: false, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: [
        'https://picsum.photos/seed/hr1/600/400',
        'https://picsum.photos/seed/hr2/600/400',
        'https://picsum.photos/seed/hr3/600/400',
        'https://picsum.photos/seed/hr4/600/400'
      ]
    }
  },
  {
    id: 'S005', title: '运动损伤急救处理指南', category: 'health', categoryLabel: '健康管理',
    cover: '/static/images/science-sport.png', author: '运动医学中心', authorAvatar: '/static/images/avatar-official.png',
    summary: '扭伤、拉伤、骨折等常见运动损伤的现场急救处理方法，RICE原则详解。',
    content: 'RICE原则：Rest休息、Ice冰敷、Compression加压、Elevation抬高...\n\n扭伤处理：立即停止运动，冰敷20分钟，弹性绷带包扎...\n骨折处理：固定伤肢，避免移动，呼叫急救...',
    viewCount: 4320, likeCount: 289, collectCount: 178, publishTime: '2025-05-01', isLiked: false, isCollected: false,
    media: {
      type: 'video',
      url: '/video-proxy/nxgov/masvod/public/2026/01/12/20260112_19bb10227c7_r1_1200k.mp4',
      poster: '/static/images/science-sport.png',
      images: []
    }
  },
  {
    id: 'S006', title: '中暑急救：高温天气如何自救互救', category: 'emergency', categoryLabel: '突发急症',
    cover: '/static/images/science-heat.png', author: '平台官方', authorAvatar: '/static/images/avatar-official.png',
    summary: '夏季高温中暑频发，识别先兆中暑、轻症中暑、重症中暑的不同表现与急救措施。',
    content: '先兆中暑：口渴、头晕、耳鸣、胸闷，转移到阴凉处休息补水即可...\n轻症中暑：体温38°C以上，面色潮红，快速降温补水...\n重症中暑（热射病）：体温40°C以上，意识障碍，立即拨打120...',
    viewCount: 3210, likeCount: 198, collectCount: 145, publishTime: '2025-05-20', isLiked: false, isCollected: false,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: [
        'https://picsum.photos/seed/heat1/600/400',
        'https://picsum.photos/seed/heat2/600/400',
        'https://picsum.photos/seed/heat3/600/400'
      ]
    }
  },
  {
    id: 'S007', title: '如何正确使用急救箱中的物品', category: 'device', categoryLabel: '设备使用',
    cover: '/static/images/science-kit.png', author: '红十字会', authorAvatar: '/static/images/avatar-official.png',
    summary: '急救箱里有什么？绷带、消毒液、止血贴、三角巾各自怎么用？一文讲清楚。',
    content: '急救箱标配物品及用法：\n\n绷带：用于固定敷料、加压止血，缠绕时注意松紧适度\n消毒液/碘伏：清洁伤口，从中心向外环形消毒\n止血贴：小伤口覆盖保护，注意更换频率\n三角巾：悬吊伤肢、固定敷料，可做头部手部包扎\n止血带：大出血时使用，记录使用时间，每40分钟松开1-2分钟...',
    viewCount: 6890, likeCount: 456, collectCount: 312, publishTime: '2025-02-10', isLiked: true, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: [
        'https://picsum.photos/seed/kit1/600/400',
        'https://picsum.photos/seed/kit2/600/400',
        'https://picsum.photos/seed/kit3/600/400',
        'https://picsum.photos/seed/kit4/600/400'
      ]
    }
  },
  {
    id: 'S008', title: '高血压患者日常管理指南', category: 'health', categoryLabel: '健康管理',
    cover: '/static/images/science-bp.png', author: '陈医生', authorAvatar: '/static/images/avatar-doctor.png',
    summary: '高血压是最常见的慢性病之一，科学管理血压从饮食、运动、用药三方面入手。',
    content: '高血压日常管理要点：\n\n饮食：低盐（每日<6g）、低脂、高纤维，DASH饮食法\n运动：中等强度有氧运动，每周5次，每次30分钟\n用药：遵医嘱规律服药，不可自行停药\n监测：每日早晚各测一次血压，记录变化趋势\n预警：收缩压≥180或舒张压≥110需立即就医...',
    viewCount: 8920, likeCount: 623, collectCount: 478, publishTime: '2025-03-08', isLiked: false, isCollected: true,
    media: {
      type: 'video',
      url: '/video-proxy/nxgov/masvod/public/2026/01/12/20260112_19bb10227c7_r1_1200k.mp4',
      poster: '/static/images/science-bp.png',
      images: []
    }
  },
  {
    id: 'S009', title: '触电急救与心肺复苏', category: 'emergency', categoryLabel: '突发急症',
    cover: '/static/images/science-shock.png', author: '平台官方', authorAvatar: '/static/images/avatar-official.png',
    summary: '触电事故如何安全施救？切断电源是第一步，心肺复苏是关键。',
    content: '触电急救步骤：\n\n1.确保安全：先切断电源或用绝缘物挑开电线，切勿直接接触触电者\n2.判断意识：轻拍双肩，大声呼唤\n3.呼救：拨打120\n4.心肺复苏：如无呼吸无脉搏，立即进行CPR\n5.持续监护：直到专业救援到达，注意观察触电者状态...',
    viewCount: 2890, likeCount: 178, collectCount: 134, publishTime: '2025-04-28', isLiked: false, isCollected: false,
    media: {
      type: 'video',
      url: '/video-proxy/nxgov/masvod/public/2026/01/12/20260112_19bb10227c7_r1_1200k.mp4',
      poster: '/static/images/science-shock.png',
      images: []
    }
  },
  {
    id: 'S010', title: '科学跑步：保护心脏还是伤心脏？', category: 'exercise', categoryLabel: '运动养生',
    cover: '/static/images/science-running.png', author: '运动医学中心', authorAvatar: '/static/images/avatar-official.png',
    summary: '跑步对心脏是利大于弊还是弊大于利？如何科学跑步既锻炼又不伤身？',
    content: '科学跑步建议：\n\n强度控制：运动心率不超过最大心率(220-年龄)的85%\n频率：每周3-5次，每次30-45分钟\n热身：跑前5-10分钟热身，逐步提升心率\n恢复：跑后慢走5分钟，拉伸放松\n预警信号：胸闷、心悸、头晕应立即停止\n建议：有心血管疾病家族史者跑步前需做心脏评估...',
    viewCount: 15600, likeCount: 1023, collectCount: 678, publishTime: '2025-01-20', isLiked: true, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: [
        'https://picsum.photos/seed/run1/600/400',
        'https://picsum.photos/seed/run2/600/400',
        'https://picsum.photos/seed/run3/600/400'
      ]
    }
  },
  {
    id: 'S011', title: '糖尿病低血糖急救处理', category: 'emergency', categoryLabel: '突发急症',
    cover: '/static/images/science-diabetes.png', author: '张医生', authorAvatar: '/static/images/avatar-doctor.png',
    summary: '糖尿病患者低血糖发作怎么办？识别症状、紧急处理、预防复发全攻略。',
    content: '低血糖识别与急救：\n\n症状：出冷汗、手抖、心慌、面色苍白、意识模糊\n急救：意识清醒时立即进食15-20g糖（糖果、含糖饮料）\n昏迷：不可喂食，侧卧位防止窒息，拨打120\n预防：规律进餐、运动前进食、随身携带糖果\n监测：定期测血糖，了解自身低血糖规律...',
    viewCount: 4560, likeCount: 312, collectCount: 201, publishTime: '2025-05-12', isLiked: false, isCollected: false,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: [
        'https://picsum.photos/seed/sugar1/600/400',
        'https://picsum.photos/seed/sugar2/600/400',
        'https://picsum.photos/seed/sugar3/600/400'
      ]
    }
  },
  {
    id: 'S012', title: '办公室颈椎保健操', category: 'exercise', categoryLabel: '运动养生',
    cover: '/static/images/science-neck.png', author: '康复医学中心', authorAvatar: '/static/images/avatar-official.png',
    summary: '久坐办公颈椎酸痛？5分钟颈椎保健操，在工作间隙轻松缓解不适。',
    content: '颈椎保健操5式：\n\n1.左右转头：缓慢左右转头各10次\n2.前后点头：缓慢前屈后伸各10次\n3.侧屈拉伸：耳向肩方向倾斜，每侧保持15秒\n4.环绕运动：顺时针逆时针各转5圈\n5.抗阻训练：手抵额头互推，保持5秒\n\n注意：动作缓慢轻柔，如出现头晕立即停止...\n\n建议每工作1小时做一次，预防颈椎病发生...',
    viewCount: 11340, likeCount: 789, collectCount: 534, publishTime: '2025-02-28', isLiked: false, isCollected: true,
    media: {
      type: 'video',
      url: '/video-proxy/nxgov/masvod/public/2026/01/12/20260112_19bb10227c7_r1_1200k.mp4',
      poster: '/static/images/science-neck.png',
      images: []
    }
  }
]

// Mock数据 - 科普分类
export const mockScienceCategories = [
  { key: 'all', label: '全部' },
  { key: 'device', label: '设备使用' },
  { key: 'emergency', label: '突发急症' },
  { key: 'health', label: '健康管理' },
  { key: 'exercise', label: '运动养生' }
]

// Mock数据 - 知识自测题库（扩充到15题）
export const mockQuizData = [
  {
    id: 'Q001', question: 'AED使用时，电极片应贴在患者的哪个部位？',
    options: ['左胸和右胸', '右胸上部和左胸下部', '腹部两侧', '背部两侧'],
    answer: 1, explanation: 'AED电极片应一片贴在右胸上部（锁骨下方），另一片贴在左胸下部（心尖部位），这样电流才能有效通过心脏。'
  },
  {
    id: 'Q002', question: '心肺复苏胸外按压的频率应为多少次/分钟？',
    options: ['60-80次', '80-100次', '100-120次', '120-140次'],
    answer: 2, explanation: '根据最新指南，胸外按压频率应为100-120次/分钟，按压深度5-6厘米。'
  },
  {
    id: 'Q003', question: '成人海姆立克急救法施救时，施救者应站在患者的哪个位置？',
    options: ['患者前方', '患者身后', '患者左侧', '患者右侧'],
    answer: 1, explanation: '施救者应站在患者身后，双臂环绕患者腰部，一手握拳置于肚脐上方进行腹部冲击。'
  },
  {
    id: 'Q004', question: '正常成人静息心率的范围是多少？',
    options: ['40-60次/分', '60-100次/分', '100-120次/分', '120-140次/分'],
    answer: 1, explanation: '正常成人静息心率为60-100次/分钟。运动员可低至40-50次/分，持续超过100次/分需就医检查。'
  },
  {
    id: 'Q005', question: '运动损伤急救的RICE原则中，I代表什么？',
    options: ['Injury（损伤）', 'Ice（冰敷）', 'Immobilize（固定）', 'Inject（注射）'],
    answer: 1, explanation: 'RICE原则：R-Rest休息，I-Ice冰敷，C-Compression加压包扎，E-Elevation抬高患肢。'
  },
  {
    id: 'Q006', question: '发现有人晕倒，第一步应该做什么？',
    options: ['立即进行胸外按压', '拨打120并判断意识', '直接使用AED', '喂水喂药'],
    answer: 1, explanation: '发现有人晕倒，首先应轻拍双肩大声呼唤判断意识，同时呼叫120，再根据情况决定是否进行CPR或使用AED。'
  },
  {
    id: 'Q007', question: '热射病（重症中暑）患者体温通常超过多少度？',
    options: ['37.5°C', '38.5°C', '39°C', '40°C'],
    answer: 3, explanation: '热射病是重症中暑，核心体温通常超过40°C，伴有中枢神经系统功能障碍，需立即急救。'
  },
  {
    id: 'Q008', question: 'AED分析心律时，以下哪项操作是正确的？',
    options: ['继续胸外按压', '接触患者确保电极片贴紧', '所有人离开患者不接触', '手动调节除颤能量'],
    answer: 2, explanation: 'AED分析心律时，必须确保无人接触患者，以免干扰分析结果。分析完成后根据提示决定是否电击。'
  },
  {
    id: 'Q009', question: '发现触电者，第一步应如何处理？',
    options: ['立即拉拽触电者', '切断电源或用绝缘物挑开电线', '往触电者身上泼水', '用手直接推开电线'],
    answer: 1, explanation: '发现触电者，必须先切断电源或用干燥的绝缘物（木棍、塑料等）挑开电线，切勿直接接触触电者。'
  },
  {
    id: 'Q010', question: '止血带使用时，应每隔多长时间松开一次？',
    options: ['10分钟', '20分钟', '40分钟', '60分钟'],
    answer: 2, explanation: '止血带使用后每40分钟应松开1-2分钟，以防肢体缺血坏死。同时记录开始使用时间。'
  },
  {
    id: 'Q011', question: '心肺复苏时胸外按压与人工呼吸的比例是？',
    options: ['15:1', '15:2', '30:1', '30:2'],
    answer: 3, explanation: '成人CPR时，胸外按压与人工呼吸比例为30:2，即30次按压后进行2次人工呼吸。'
  },
  {
    id: 'Q012', question: '低血糖昏迷患者，以下哪项处理是正确的？',
    options: ['立即喂食糖果', '口服含糖饮料', '侧卧位防止窒息并拨打120', '大量饮水'],
    answer: 2, explanation: '低血糖昏迷患者不可喂食（有窒息风险），应使其侧卧位防止误吸，立即拨打120。意识清醒时才可进食糖分。'
  },
  {
    id: 'Q013', question: '扭伤后应立即采取什么措施？',
    options: ['热敷促进血液循环', '揉搓患处消肿', '冰敷减轻肿胀', '立即进行拉伸'],
    answer: 2, explanation: '扭伤后应立即冰敷（不是热敷），每次20分钟，可减轻疼痛和肿胀。48小时后可改为热敷。'
  },
  {
    id: 'Q014', question: '高血压患者每日食盐摄入量应控制在多少以下？',
    options: ['3g', '6g', '10g', '15g'],
    answer: 1, explanation: '高血压患者每日食盐摄入应控制在6g以下（约一啤酒瓶盖）。减少盐摄入可有效降低血压。'
  },
  {
    id: 'Q015', question: '婴儿发生异物卡喉时，应采用什么急救方法？',
    options: ['海姆立克腹部冲击法', '背部拍击法（面朝下）', '催吐法', '喝水冲服'],
    answer: 1, explanation: '1岁以下婴儿异物卡喉，应采用背部拍击法：将婴儿面朝下放在前臂上，头部低于胸部，掌根拍击背部5次，如无效再翻正面按压胸部5次。'
  }
]

// Mock数据 - 我的救援记录（扩充到6条）
export const mockMyRescues = [
  {
    id: 'R002', type: 'volunteer', callUser: '孙大伟',
    address: '杭州市西湖区古翠路60号', description: '老人摔倒头部出血',
    urgency: 'high', status: 'completed',
    createTime: '2025-06-10 09:15', completeTime: '2025-06-10 09:28', rating: 5
  },
  {
    id: 'R004', type: 'caller', volunteerUser: '赵志远',
    address: '杭州市拱墅区萍水街', description: '运动扭伤需要急救包',
    urgency: 'medium', status: 'completed',
    createTime: '2025-05-20 15:30', completeTime: '2025-05-20 15:45', rating: 4
  },
  {
    id: 'R005', type: 'volunteer', callUser: '李阿姨',
    address: '杭州市下城区朝晖路166号', description: '邻居老人跌倒骨折',
    urgency: 'high', status: 'completed',
    createTime: '2025-04-22 11:10', completeTime: '2025-04-22 11:32', rating: 5
  },
  {
    id: 'R007', type: 'caller', volunteerUser: '王建国',
    address: '杭州市西湖区之江路168号', description: '家人食物中毒呕吐不止',
    urgency: 'high', status: 'completed',
    createTime: '2025-03-30 19:05', completeTime: '2025-03-30 19:22', rating: 5
  },
  {
    id: 'R008', type: 'volunteer', callUser: '张先生',
    address: '杭州市拱墅区大关路98号', description: '同事在工作中被割伤出血',
    urgency: 'medium', status: 'completed',
    createTime: '2025-03-12 14:48', completeTime: '2025-03-12 15:05', rating: 4
  },
  {
    id: 'R009', type: 'caller', volunteerUser: '周明辉',
    address: '杭州市西湖区文二西路88号', description: '运动时突然胸闷气短',
    urgency: 'high', status: 'completed',
    createTime: '2025-02-15 16:20', completeTime: '2025-02-15 16:38', rating: 5
  }
]

// Mock数据 - 可扫描发现的蓝牙设备（用于设备绑定页）
export const mockBLEDevices = [
  { id: 'BLE001', name: '华为手环 Band 8', type: 'band', signal: 4, bonded: true },
  { id: 'BLE002', name: '小米手环 7', type: 'band', signal: 3, bonded: false },
  { id: 'BLE003', name: 'Apple Watch S9', type: 'watch', signal: 2, bonded: false },
  { id: 'BLE004', name: 'OPPO Watch 3', type: 'watch', signal: 1, bonded: false },
  { id: 'BLE005', name: '华为Watch GT4', type: 'watch', signal: 3, bonded: false },
  { id: 'BLE006', name: '荣耀手环 7', type: 'band', signal: 2, bonded: false }
]

// Mock数据 - 首页统计概览
export const mockHomeStats = {
  totalDevices: 20,       // 12固定 + 8移动
  onlineDevices: 15,      // 在线设备数
  totalRescues: 156,      // 平台总救援次数
  totalVolunteers: 89,    // 注册志愿者数
  todayRescues: 3,        // 今日救援
  nearbyDevices: 8        // 附近设备数
}
