CREATE TABLE science_contents (
  id VARCHAR(80) NOT NULL PRIMARY KEY,
  kind VARCHAR(20) NOT NULL,
  category VARCHAR(30) NULL,
  title VARCHAR(80) NOT NULL,
  summary VARCHAR(500) NOT NULL,
  body LONGTEXT NULL,
  author VARCHAR(80) NOT NULL,
  cover_path VARCHAR(255) NULL,
  cover_media_id CHAR(36) NULL,
  media_url VARCHAR(1000) NULL,
  source_name VARCHAR(120) NULL,
  source_page VARCHAR(1000) NULL,
  duration_label VARCHAR(40) NULL,
  view_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  collect_count INT NOT NULL DEFAULT 0,
  published_at DATETIME(6) NOT NULL,
  sort_order INT NOT NULL DEFAULT 500,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  source_submission_id CHAR(36) NULL,
  UNIQUE KEY uk_science_content_submission (source_submission_id),
  KEY idx_science_content_kind_order (kind, active, sort_order, published_at),
  CONSTRAINT fk_science_content_media FOREIGN KEY (cover_media_id) REFERENCES media_assets(id) ON DELETE RESTRICT,
  CONSTRAINT fk_science_content_submission FOREIGN KEY (source_submission_id) REFERENCES science_submissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO science_contents (id, kind, category, title, summary, body, author, cover_path, cover_media_id, media_url, source_name, source_page, duration_label, view_count, like_count, collect_count, published_at, sort_order, active, source_submission_id) VALUES
  ('S001', 'ARTICLE', 'device', 'AED自动体外除颤器使用教程', '详细讲解AED的开箱、电极片粘贴、语音提示跟随操作全流程，人人都能学会的救命技能。', '看到 AED 不要等专业人员“授权”。它会自动分析心律，只有判断需要除颤时才会建议放电。确保现场安全后，确认患者无反应且没有正常呼吸，立即呼叫 120、取 AED 并开始胸外按压。

打开 AED，露出并擦干胸部。按图示将一片电极片贴在右胸上部，另一片贴在左胸下外侧。设备提示分析心律时，大声提醒所有人离开患者；建议放电时，再次确认无人接触后按下闪烁按键。

放电完成或设备提示不建议放电后，立即恢复胸外按压。不要擅自关机或撕下电极片，持续跟随语音，直到患者恢复正常呼吸或专业救援人员接手。线上学习不能代替 CPR+AED 实操培训。', '平台官方', '/static/editorial/editorial-aed-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 12580, 892, 567, '2025-03-01 08:00:00', 10, TRUE, NULL),
  ('S002', 'ARTICLE', 'emergency', '心肺复苏CPR黄金四分钟', '心搏骤停4分钟后脑细胞开始不可逆损伤，掌握CPR正确手法，在黄金时间内挽救生命。', '一个人突然倒地，需要马上启动“生命链”：识别、呼救、按压、AED。先确保环境安全，再轻拍双肩大声呼唤。患者无反应且没有正常呼吸，或只有叹息样喘气，应按心搏骤停处理。

指定一人拨打 120，另一人寻找 AED。让患者仰卧在坚硬平面，一手掌根放在胸部中央、胸骨下半部，另一手叠在上面。手臂伸直，垂直按压 5–6 厘米，频率每分钟 100–120 次，每次让胸廓充分回弹。

受过训练并愿意人工呼吸时，按“30 次按压 + 2 次吹气”循环；未受训练时，按 120 调度员指导持续做胸外按压。AED 到达后尽快使用，尽量减少按压中断，直到患者恢复正常呼吸或专业人员接手。', '平台官方', '/static/editorial/editorial-cpr-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 9870, 723, 445, '2025-03-15 08:00:00', 20, TRUE, NULL),
  ('S003', 'ARTICLE', 'emergency', '海姆立克急救法：异物卡喉怎么办', '异物卡喉窒息是常见急症，海姆立克法简单有效，适用于成人和儿童不同场景。', '吃饭时突然说不出话、不能呼吸或明显发绀，才是需要立即干预的严重气道异物梗阻。如果当事人还能有效咳嗽和说话，鼓励他继续用力咳嗽，不要盲目拍背或伸手抠取。

清醒的成人或 1 岁以上儿童出现严重梗阻时，呼叫 120。施救者站在其身后，一手握拳放在肚脐上方、胸骨下方，另一手包住拳头，向内向上快速冲击，直到异物排出或患者失去反应。

1 岁以下婴儿不做腹部冲击，应交替进行 5 次背部拍击和 5 次胸部冲击。如患者失去反应，轻放到坚硬地面，按 120 调度指导开始心肺复苏并寻找 AED。只移除看得见、容易取出的异物，不要盲目深挖。', '李医生', '/static/editorial/editorial-cpr-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 7650, 534, 389, '2025-04-01 08:00:00', 30, TRUE, NULL),
  ('S004', 'ARTICLE', 'health', '日常心率监测：了解你的心脏健康', '静息心率、运动心率、最大心率分别代表什么？如何通过日常监测发现心脏异常信号？', '心率是“趋势指标”，不是一次测量就能得出结论的成绩单。起床、咖啡、情绪、发热、脱水和运动都可能让数字短时变化，更重要的是在相似条件下观察长期基线。

测静息心率时，尽量选每天同一时段，静坐 5 分钟后再记录。穿戴设备适合看趋势，但不能代替心电图。数据明显偏离平时水平时，先静息复测，同时留意是否有不适。

如出现胸痛、呼吸困难、晕厥或意识异常，不要继续盯着手表数字，应立即停止活动并求助。若无明显不适但心率反复异常、心律不齐提示频繁出现，带上记录咨询医生。', '王医生', '/static/editorial/editorial-monitoring-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 5430, 367, 234, '2025-04-15 08:00:00', 40, TRUE, NULL),
  ('S005', 'ARTICLE', 'health', '运动损伤急救处理指南', '扭伤、拉伤、骨折等常见运动损伤的现场急救处理方法，RICE原则详解。', '运动后立即肿痛，首先停止活动并保护受伤部位。如有明显畸形、骨端外露、剧痛无法承重或远端麻木苍白，按骨折处理并尽快就医。

轻度扭拉伤可用保护、适度休息、冷敷、弹力加压和抬高患肢减轻初期肿痛。冷敷不要让冰袋直接接触皮肤；加压后如出现麻木、发冷或颜色变化，立即放松。

怀疑骨折时保持受伤后的姿势，不要强行拉直、按摩或把外露骨端推回。脊柱或骨盆疑似损伤者，除非环境存在继发危险，应等待专业救援。', '运动医学中心', '/static/editorial/editorial-wellness-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 4320, 289, 178, '2025-05-01 08:00:00', 50, TRUE, NULL),
  ('S006', 'ARTICLE', 'emergency', '中暑急救：高温天气如何自救互救', '夏季高温中暑频发，识别先兆中暑、轻症中暑、重症中暑的不同表现与急救措施。', '热射病是可能迅速危及生命的急症。高温环境中出现意识模糊、言语异常、抽搐或昏迷，无论是否测到高体温，都应立即拨打 120 并开始降温。

将患者转移到阴凉通风处，脱去多余衣物，用冷水浸湿皮肤、持续扇风，在颈部、腋下和腹股沟等部位辅助冷敷。不要因为等救护车而推迟现场降温。

只有意识清醒且能正常吞咽时，才可小口补充凉水或含电解质的饮品。意识不清、呃吐或吞咽困难时禁止喂水喂药，应侧卧、保持气道通畅并观察呼吸。', '平台官方', '/static/editorial/editorial-cpr-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 3210, 198, 145, '2025-05-20 08:00:00', 60, TRUE, NULL),
  ('S007', 'ARTICLE', 'device', '如何正确使用急救箱中的物品', '急救箱里有什么？绷带、消毒液、止血贴、三角巾各自怎么用？一文讲清楚。', '急救箱的价值不在“东西多”，而在紧急时找得到、会使用。家庭建议放在成人容易取用、儿童无法随意触及的固定位置，并让家人都知道。

一次性手套用于减少与血液直接接触；无菌纱布和绷带用于覆盖伤口、加压止血；三角巾可用于悬吊和辅助固定。创可贴只适合清洁的小伤口，不要覆盖深部或明显污染伤口。

严重出血首选持续直接加压并拨打 120。商用止血带仅用于四肢危及生命的大出血，按说明使用并记录时间；一旦有效止血，不要由非专业人员反复松解。每三个月检查耗材有效期并及时补齐。', '红十字会', '/static/editorial/editorial-aed-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 6890, 456, 312, '2025-02-10 08:00:00', 70, TRUE, NULL),
  ('S008', 'ARTICLE', 'health', '高血压患者日常管理指南', '高血压是最常见的慢性病之一，科学管理血压从饮食、运动、用药三方面入手。', '血压管理的核心是“长期、稳定、可追溯”。一次偏高不等于诊断，一次正常也不意味着可以自行停药，应在医生指导下结合多日记录评估。

家庭测量前 30 分钟避免吸烟、咖啡和运动，排空膀胱后静坐 5 分钟。背部有支撑、双脚平放，手臂放在与心脏同高的桌面，选对袖带尺寸。每次同时记录日期、时间和当时状态。

如血压异常的同时出现胸痛、呼吸困难、肢体无力、言语不清、意识改变或剧烈头痛，立即呼叫 120。没有急性症状但多次测量持续偏高时，也应尽快就医调整方案。', '陈医生', '/static/editorial/editorial-monitoring-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 8920, 623, 478, '2025-03-08 08:00:00', 80, TRUE, NULL),
  ('S009', 'ARTICLE', 'emergency', '触电急救与心肺复苏', '触电事故如何安全施救？切断电源是第一步，心肺复苏是关键。', '触电现场最危险的误区，是还没断电就伸手拉人。施救者必须先保护自己：尽快切断电源，无法断电时不要靠近高压设备或掉落电线，拨打 119 和 120 等待专业人员。

确认已脱离电源后，判断患者意识和正常呼吸。无反应且没有正常呼吸时，立即开始胸外按压并获取 AED，不要因为只看到小的入口灼伤就低估内部损伤。

电灼伤处用干净、不粘连的敷料轻盖，不要涂抹牙膏、油脂或刺破水疱。即使患者看起来恢复，也可能出现心律失常和深部组织损伤，应接受医疗评估。', '平台官方', '/static/editorial/editorial-cpr-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 2890, 178, 134, '2025-04-28 08:00:00', 90, TRUE, NULL),
  ('S010', 'ARTICLE', 'exercise', '科学跑步：保护心脏还是伤心脏？', '跑步对心脏是利大于弊还是弊大于利？如何科学跑步既锻炼又不伤身？', '跑步对大多数人有益，但“越累越有效”不是科学训练。建立心肺适应需要负荷与恢复交替，初学者用跑走结合比一次跑到力竭更容易长期坚持。

每次从 5–10 分钟快走和动态热身开始，运动中以能说出短句的主观感受调节强度。速度、距离和频率不要同时快速增加，跑后慢走几分钟，让心率逐渐回落。

如有心血管疾病、不明原因晕厥、运动性胸痛或长期未运动，开始高强度训练前先咨询医生。运动时出现胸闷胸痛、呼吸明显困难、头晕或强烈心悸，立即停止并求助。', '运动医学中心', '/static/editorial/editorial-wellness-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 15600, 1023, 678, '2025-01-20 08:00:00', 100, TRUE, NULL),
  ('S011', 'ARTICLE', 'emergency', '糖尿病低血糖急救处理', '糖尿病患者低血糖发作怎么办？识别症状、紧急处理、预防复发全攻略。', '低血糖常表现为出汗、手抖、心慌、强烈饥饿，也可能出现行为异常、言语不清和意识模糊。对使用胰岛素或某些降糖药的人，这些信号尤其需要快速处理。

患者清醒、能安全吞咽时，可补充约 15 克快速吸收的碳水，如葡萄糖片或定量含糖饮料，15 分钟后复测或再评估。仍低或症状未缓解时再重复一次。

患者昏迷、抽搐或无法正常吞咽时，绝对不要喂水、喂糖或把食物塞入口中。立即拨打 120，将其置于稳定侧卧位并观察呼吸。发作后与医生回顾药物、运动和进食情况。', '张医生', '/static/editorial/editorial-monitoring-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 4560, 312, 201, '2025-05-12 08:00:00', 110, TRUE, NULL),
  ('S012', 'ARTICLE', 'exercise', '办公室颈椎保健操', '久坐办公颈椎酸痛？5分钟颈椎保健操，在工作间隙轻松缓解不适。', '久坐后的颈肩紧绷，往往更需要频繁而温和的活动，不是大幅度“甩脖子”。把每 30–60 分钟起身一次设成日常节奏，比睡前一次做很多动作更稳妥。

坐好后让下巴轻轻回收，像是让后脑勺向上延伸，保持 5 秒后放松，重复 5 次。然后缓慢向左右转头，只到舒适范围；肩胛向后下收紧 5 秒，重复 8 次。

屏幕上缘尽量接近视线高度，手机尽量抬高。如出现上肢麻木无力、走路不稳、明显外伤后颈痛，或疼痛持续加重，不要继续自行拉伸或接受大力按摩，应尽快就医。', '康复医学中心', '/static/editorial/editorial-wellness-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 11340, 789, 534, '2025-02-28 08:00:00', 120, TRUE, NULL),
  ('S013', 'ARTICLE', 'emergency', '识别卒中：记住“中风 120”', '脸歪、手臂无力、语言不清，任何一项都要立即记录时间并拨打 120。', '卒中的救治与时间密切相关。当一个人突然出现脸歪、一侧手臂无力或说话含糊，不要等症状自行消失，立即记住最后正常的时间并拨打 120。

可用“中风 120”快速识别：“1”看一张脸是否不对称；“2”查两只手臂是否有一侧无力下垂；“0”听语言是否不清，有任何一项就立即拨打 120。

等待救护时让患者安静休息，不要喂食、喂水或自行给阿司匹林。即使症状几分钟后完全消失，也可能是短暂性脑缺血发作，同样需要紧急医疗评估。', '平台官方', '/static/editorial/editorial-monitoring-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 6380, 521, 402, '2026-06-12 08:00:00', 130, TRUE, NULL),
  ('S014', 'ARTICLE', 'emergency', '严重出血：先加压，再谈其他', '看懂危及生命的出血信号，掌握直接加压、止血带和保暖等现场要点。', '伤口持续喷射或涌出血液、衣物快速被浸透，或伤者苍白、冷汗、反应变慢，都要按危及生命的出血处理。先呼叫 120，同时戴上手套或尽量减少直接接触血液。

用纱布或干净布料直接覆盖伤口，用手掌持续、稳定地加压。敷料被浸透时不要揭掉底层，可在上面继续覆盖并加压。如伤口内有较大异物，不要拔出。

四肢大出血经持续加压仍无法控制，且有合格商用止血带时，按说明使用并记录时间，不自行反复松解。让伤者平卧、保暖，不要喂食或喂水，持续观察意识和呼吸。', '平台官方', '/static/editorial/editorial-cpr-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 5820, 468, 377, '2026-06-28 08:00:00', 140, TRUE, NULL),
  ('S015', 'ARTICLE', 'emergency', '烧烫伤后，先做对这三件事', '脱离热源、流动凉水冲洗、清洁覆盖，远离冰块和牙膏等常见误区。', '烧烫伤后的第一目标是终止热源，而不是马上涂东西。先确保环境安全，远离明火、热液或蒸汽；粘在皮肤上的衣物不要强行撕掉。

尽快用清洁的流动凉水冲洗受伤处约 20 分钟，同时取下附近的戒指、手表等紧身物品。不要用冰块直接冰敷，不要涂牙膏、酱油、食用油或不明药膏。

冷却后用无菌、不粘连的敷料松散覆盖，不要刺破水疱。如受伤面积较大、皮肤发白或焦黑，或伴吸入烟雾、声音嘶哑和呼吸困难，立即拨打 120。', '平台官方', '/static/editorial/editorial-wellness-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 7190, 603, 446, '2026-07-08 08:00:00', 150, TRUE, NULL),
  ('S016', 'ARTICLE', 'emergency', '遇到抽搐，别按住，别塞东西', '保护现场、准确计时、抽搐后侧卧，并识别必须立即呼叫 120 的情况。', '抽搐看起来很紧急，但强行按住四肢或往嘴里塞东西会带来额外伤害。首先记下开始时间，移开周围尖锐、坚硬物体，在头下垫柔软衣物，让现场保持通风。

不要强行制止抽动，不要掀开牙关、喂水喂药或做人工呼吸。抽搐停止后，让患者侧卧，松开过紧衣领，陪伴其直到意识恢复，不要立即让他站起。

首次抽搐、持续超过 5 分钟、短时内反复发作且中间未清醒、受伤、怀孕、水中发作或抽搐后呼吸异常时，立即拨打 120。抽搐停止后无正常呼吸时，开始心肺复苏。', '平台官方', '/static/editorial/editorial-monitoring-light-v1.webp', NULL, NULL, NULL, NULL, NULL, 4910, 386, 318, '2026-07-22 08:00:00', 160, TRUE, NULL),
  ('V001', 'VIDEO', NULL, 'AED 急救使用培训', '从开机、电极片粘贴到听从语音指令，完整演示 AED 操作流程。', NULL, '中共广西区委党校', '/static/editorial/editorial-cpr-light-v1.webp', NULL, 'https://www.gxdx.gov.cn/UploadFiles/files/20250522/080396AED%E6%80%A5%E6%95%91%E4%BD%BF%E7%94%A8%E5%9F%B9%E8%AE%AD%E8%A7%86%E9%A2%91.mp4', '中共广西区委党校', 'https://www.gxdx.gov.cn/newsview.html?NewsId=70133', '培训视频', 0, 0, 0, '2026-06-16 09:51:02', 200, TRUE, NULL),
  ('V002', 'VIDEO', NULL, 'AED 自己会教你怎么用', '北京市红十字会讲师示范如何识别设备提示并完成除颤操作。', NULL, '新华网 · 北京市红十字会讲师', '/static/editorial/editorial-aed-light-v1.webp', NULL, 'https://vodpub6.v.news.cn/yqfbzx-original/20260616/2026061655fe05871980481da03d478ad7a4094e_6f6f83c940214384b0df345c5eb81783.mp4', '新华网 · 北京市红十字会讲师', 'https://www.news.cn/tech/20260616/55fe05871980481da03d478ad7a4094e/c.html', '竖屏短课', 0, 0, 0, '2026-06-16 09:51:02', 210, TRUE, NULL),
  ('V003', 'VIDEO', NULL, '心脏骤停，救人不能只等医生', '认识院外心脏骤停，理解第一目击者立即呼救和施救的重要性。', NULL, '新华网 · 北京市红十字会讲师', '/static/editorial/editorial-monitoring-light-v1.webp', NULL, 'https://vodpub6.v.news.cn/yqfbzx-original/20260616/2026061655fe05871980481da03d478ad7a4094e_b141713644794a4598a857c5abcaf54b.mp4', '新华网 · 北京市红十字会讲师', 'https://www.news.cn/tech/20260616/55fe05871980481da03d478ad7a4094e/c.html', '竖屏短课', 0, 0, 0, '2026-06-16 09:51:02', 220, TRUE, NULL),
  ('V004', 'VIDEO', NULL, '施救怕担责？“好人法”给你底气', '结合公益性紧急救助场景，讲清公众最担心的施救责任问题。', NULL, '新华网 · 北京市红十字会讲师', '/static/editorial/editorial-cpr-light-v1.webp', NULL, 'https://vodpub6.v.news.cn/yqfbzx-original/20260616/2026061655fe05871980481da03d478ad7a4094e_c074696e5e284a08af9405969d0a76fd.mp4', '新华网 · 北京市红十字会讲师', 'https://www.news.cn/tech/20260616/55fe05871980481da03d478ad7a4094e/c.html', '竖屏短课', 0, 0, 0, '2026-06-16 09:51:02', 230, TRUE, NULL),
  ('V005', 'VIDEO', NULL, '公共场所的 AED 怎么找', '识别 AED 标识、柜体与导航线索，减少紧急时刻的寻找时间。', NULL, '新华网 · 北京市红十字会讲师', '/static/editorial/editorial-aed-light-v1.webp', NULL, 'https://vodpub6.v.news.cn/yqfbzx-original/20260616/2026061655fe05871980481da03d478ad7a4094e_9cdd5d134ccd4245b7686a551b123314.mp4', '新华网 · 北京市红十字会讲师', 'https://www.news.cn/tech/20260616/55fe05871980481da03d478ad7a4094e/c.html', '竖屏短课', 0, 0, 0, '2026-06-16 09:51:02', 240, TRUE, NULL);

INSERT IGNORE INTO science_contents (
  id, kind, category, title, summary, body, author, cover_media_id,
  view_count, like_count, collect_count, published_at, sort_order, active, source_submission_id
)
SELECT CONCAT('submission-', id), 'ARTICLE', category, title,
       LEFT(REPLACE(REPLACE(content, CHAR(10), ' '), CHAR(13), ' '), 100),
       content, '用户投稿', cover_media_id, 0, 0, 0,
       COALESCE(reviewed_at, submitted_at), 500, TRUE, id
FROM science_submissions
WHERE status = 'APPROVED';
