export const scienceCategories = [
  { key: 'all', label: '全部' },
  { key: 'device', label: '设备使用' },
  { key: 'emergency', label: '突发急症' },
  { key: 'health', label: '健康管理' },
  { key: 'exercise', label: '运动养生' }
]

export function scienceCategoryLabel(category?: string, fallback = category || '科普内容') {
  return scienceCategories.find(item => item.key === category)?.label || fallback
}
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
    id: 'Q010', question: '四肢大出血使用止血带后，以下哪项正确？',
    options: ['每 40 分钟自行松开', '记录时间并等专业人员处理', '转移到关节上', '出血减少就立即取下'],
    answer: 1, explanation: '止血带用于四肢危及生命的大出血。使用后应记录时间、尽快转交专业救援，非专业人员不要反复松解。'
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
