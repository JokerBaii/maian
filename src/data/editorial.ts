const editorialCovers = {
  aed: '/static/editorial/editorial-aed-light-v1.webp',
  cpr: '/static/editorial/editorial-cpr-light-v1.webp',
  monitoring: '/static/editorial/editorial-monitoring-light-v1.webp',
  wellness: '/static/editorial/editorial-wellness-light-v1.webp'
}
const authorAvatar = '/static/logo.png'

export const scienceArticles = [
  {
    id: 'S001', title: 'AED自动体外除颤器使用教程', category: 'device', categoryLabel: '设备使用',
    cover: editorialCovers.aed, author: '平台官方', authorAvatar,
    summary: '详细讲解AED的开箱、电极片粘贴、语音提示跟随操作全流程，人人都能学会的救命技能。',
    content: 'AED（自动体外除颤器）是一种便携式医疗设备，可以诊断特定的心律失常，并给予电击除颤。AED操作简单，非专业人员也能使用...\n\n第一步：打开AED电源\n按下电源按钮或翻开盖子即可自动开机...\n\n第二步：贴电极片\n按照电极片上的图示，将一片贴在右胸上部，另一片贴在左胸下部...\n\n第三步：听从语音提示\nAED会自动分析心律，如需电击，会语音提示"建议除颤"...\n\n第四步：电击\n确保无人接触患者后，按下电击按钮...',
    viewCount: 12580, likeCount: 892, collectCount: 567, publishTime: '2025-03-01', isLiked: false, isCollected: true,
    media: {
      type: 'video',
      url: 'https://www.bjredcross.org.cn/upload/videos/2021/12/22/6cd2d827f38ae7d6.mp4',
      poster: editorialCovers.aed,
      images: []
    }
  },
  {
    id: 'S002', title: '心肺复苏CPR黄金四分钟', category: 'emergency', categoryLabel: '突发急症',
    cover: editorialCovers.cpr, author: '平台官方', authorAvatar,
    summary: '心搏骤停4分钟后脑细胞开始不可逆损伤，掌握CPR正确手法，在黄金时间内挽救生命。',
    content: '心肺复苏（CPR）是针对心搏骤停采取的急救措施...\n\n判断意识：轻拍双肩，大声呼唤\n呼救：拨打120，取AED\n胸外按压：双手交叠，掌根置于胸骨中下1/3处，深度5-6cm，频率100-120次/分\n人工呼吸：30次按压后2次吹气\n持续循环直到专业救援到达...',
    viewCount: 9870, likeCount: 723, collectCount: 445, publishTime: '2025-03-15', isLiked: true, isCollected: false,
    media: {
      type: 'video',
      url: 'https://www.gzfuquan.gov.cn/masvod/public/2020/06/10/20200610_1729d9a8e31_r1_500k.mp4',
      poster: editorialCovers.cpr,
      images: []
    }
  },
  {
    id: 'S003', title: '海姆立克急救法：异物卡喉怎么办', category: 'emergency', categoryLabel: '突发急症',
    cover: editorialCovers.cpr, author: '李医生', authorAvatar,
    summary: '异物卡喉窒息是常见急症，海姆立克法简单有效，适用于成人和儿童不同场景。',
    content: '海姆立克急救法是通过冲击腹部使膈肌上抬，增加胸腔压力将异物排出...\n\n成人施救：站在患者身后，一手握拳置于肚脐上方，另一手包住拳头，向内向上快速冲击...\n\n儿童施救：跪在儿童身后，使用同样手法但力度减小...\n\n婴儿施救：面朝下放在前臂上，掌根拍击背部5次...',
    viewCount: 7650, likeCount: 534, collectCount: 389, publishTime: '2025-04-01', isLiked: false, isCollected: false,
    media: {
      type: 'video',
      url: 'https://medical-cms.cdn.bcebos.com/video/video_16444798680122HsmFTQo.mp4',
      poster: editorialCovers.cpr,
      images: []
    }
  },
  {
    id: 'S004', title: '日常心率监测：了解你的心脏健康', category: 'health', categoryLabel: '健康管理',
    cover: editorialCovers.monitoring, author: '王医生', authorAvatar,
    summary: '静息心率、运动心率、最大心率分别代表什么？如何通过日常监测发现心脏异常信号？',
    content: '心率是反映心脏健康的重要指标...\n\n静息心率：正常范围60-100次/分，运动员可低至40-50次/分\n运动心率：根据年龄计算最大心率=220-年龄\n异常信号：持续心率过快/过慢、心律不齐、运动后恢复慢...',
    viewCount: 5430, likeCount: 367, collectCount: 234, publishTime: '2025-04-15', isLiked: false, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S005', title: '运动损伤急救处理指南', category: 'health', categoryLabel: '健康管理',
    cover: editorialCovers.wellness, author: '运动医学中心', authorAvatar,
    summary: '扭伤、拉伤、骨折等常见运动损伤的现场急救处理方法，RICE原则详解。',
    content: 'RICE原则：Rest休息、Ice冰敷、Compression加压、Elevation抬高...\n\n扭伤处理：立即停止运动，冰敷20分钟，弹性绷带包扎...\n骨折处理：固定伤肢，避免移动，呼叫急救...',
    viewCount: 4320, likeCount: 289, collectCount: 178, publishTime: '2025-05-01', isLiked: false, isCollected: false,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S006', title: '中暑急救：高温天气如何自救互救', category: 'emergency', categoryLabel: '突发急症',
    cover: editorialCovers.cpr, author: '平台官方', authorAvatar,
    summary: '夏季高温中暑频发，识别先兆中暑、轻症中暑、重症中暑的不同表现与急救措施。',
    content: '先兆中暑：口渴、头晕、耳鸣、胸闷，转移到阴凉处休息补水即可...\n轻症中暑：体温38°C以上，面色潮红，快速降温补水...\n重症中暑（热射病）：体温40°C以上，意识障碍，立即拨打120...',
    viewCount: 3210, likeCount: 198, collectCount: 145, publishTime: '2025-05-20', isLiked: false, isCollected: false,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S007', title: '如何正确使用急救箱中的物品', category: 'device', categoryLabel: '设备使用',
    cover: editorialCovers.aed, author: '红十字会', authorAvatar,
    summary: '急救箱里有什么？绷带、消毒液、止血贴、三角巾各自怎么用？一文讲清楚。',
    content: '急救箱标配物品及用法：\n\n绷带：用于固定敷料、加压止血，缠绕时注意松紧适度\n消毒液/碘伏：清洁伤口，从中心向外环形消毒\n止血贴：小伤口覆盖保护，注意更换频率\n三角巾：悬吊伤肢、固定敷料，可做头部手部包扎\n止血带：大出血时使用，记录使用时间，每40分钟松开1-2分钟...',
    viewCount: 6890, likeCount: 456, collectCount: 312, publishTime: '2025-02-10', isLiked: true, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S008', title: '高血压患者日常管理指南', category: 'health', categoryLabel: '健康管理',
    cover: editorialCovers.monitoring, author: '陈医生', authorAvatar,
    summary: '高血压是最常见的慢性病之一，科学管理血压从饮食、运动、用药三方面入手。',
    content: '高血压日常管理要点：\n\n饮食：低盐（每日<6g）、低脂、高纤维，DASH饮食法\n运动：中等强度有氧运动，每周5次，每次30分钟\n用药：遵医嘱规律服药，不可自行停药\n监测：每日早晚各测一次血压，记录变化趋势\n预警：收缩压≥180或舒张压≥110需立即就医...',
    viewCount: 8920, likeCount: 623, collectCount: 478, publishTime: '2025-03-08', isLiked: false, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S009', title: '触电急救与心肺复苏', category: 'emergency', categoryLabel: '突发急症',
    cover: editorialCovers.cpr, author: '平台官方', authorAvatar,
    summary: '触电事故如何安全施救？切断电源是第一步，心肺复苏是关键。',
    content: '触电急救步骤：\n\n1.确保安全：先切断电源或用绝缘物挑开电线，切勿直接接触触电者\n2.判断意识：轻拍双肩，大声呼唤\n3.呼救：拨打120\n4.心肺复苏：如无呼吸无脉搏，立即进行CPR\n5.持续监护：直到专业救援到达，注意观察触电者状态...',
    viewCount: 2890, likeCount: 178, collectCount: 134, publishTime: '2025-04-28', isLiked: false, isCollected: false,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S010', title: '科学跑步：保护心脏还是伤心脏？', category: 'exercise', categoryLabel: '运动养生',
    cover: editorialCovers.wellness, author: '运动医学中心', authorAvatar,
    summary: '跑步对心脏是利大于弊还是弊大于利？如何科学跑步既锻炼又不伤身？',
    content: '科学跑步建议：\n\n强度控制：运动心率不超过最大心率(220-年龄)的85%\n频率：每周3-5次，每次30-45分钟\n热身：跑前5-10分钟热身，逐步提升心率\n恢复：跑后慢走5分钟，拉伸放松\n预警信号：胸闷、心悸、头晕应立即停止\n建议：有心血管疾病家族史者跑步前需做心脏评估...',
    viewCount: 15600, likeCount: 1023, collectCount: 678, publishTime: '2025-01-20', isLiked: true, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S011', title: '糖尿病低血糖急救处理', category: 'emergency', categoryLabel: '突发急症',
    cover: editorialCovers.monitoring, author: '张医生', authorAvatar,
    summary: '糖尿病患者低血糖发作怎么办？识别症状、紧急处理、预防复发全攻略。',
    content: '低血糖识别与急救：\n\n症状：出冷汗、手抖、心慌、面色苍白、意识模糊\n急救：意识清醒时立即进食15-20g糖（糖果、含糖饮料）\n昏迷：不可喂食，侧卧位防止窒息，拨打120\n预防：规律进餐、运动前进食、随身携带糖果\n监测：定期测血糖，了解自身低血糖规律...',
    viewCount: 4560, likeCount: 312, collectCount: 201, publishTime: '2025-05-12', isLiked: false, isCollected: false,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  },
  {
    id: 'S012', title: '办公室颈椎保健操', category: 'exercise', categoryLabel: '运动养生',
    cover: editorialCovers.wellness, author: '康复医学中心', authorAvatar,
    summary: '久坐办公颈椎酸痛？5分钟颈椎保健操，在工作间隙轻松缓解不适。',
    content: '颈椎保健操5式：\n\n1.左右转头：缓慢左右转头各10次\n2.前后点头：缓慢前屈后伸各10次\n3.侧屈拉伸：耳向肩方向倾斜，每侧保持15秒\n4.环绕运动：顺时针逆时针各转5圈\n5.抗阻训练：手抵额头互推，保持5秒\n\n注意：动作缓慢轻柔，如出现头晕立即停止...\n\n建议每工作1小时做一次，预防颈椎病发生...',
    viewCount: 11340, likeCount: 789, collectCount: 534, publishTime: '2025-02-28', isLiked: false, isCollected: true,
    media: {
      type: 'image',
      url: '',
      poster: '',
      images: []
    }
  }
]

export const scienceCategories = [
  { key: 'all', label: '全部' },
  { key: 'device', label: '设备使用' },
  { key: 'emergency', label: '突发急症' },
  { key: 'health', label: '健康管理' },
  { key: 'exercise', label: '运动养生' }
]

export const scienceQuizQuestions = [
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
