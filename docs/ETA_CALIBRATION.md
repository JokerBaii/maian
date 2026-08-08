# AED 调度 ETA 与标定说明

## 适用范围

ETA 不是校园专用参数。当前调度模型适用于校园、社区、体育场馆、交通枢纽、园区和城市道路等场景。部署时应根据当地的道路密度、门禁/楼梯、交通方式、时段和天气重新标定，不应把一组校园样本参数直接外推到其他地区。

## 当前计算语义

- 移动 AED：`dispatch overhead + route distance / mobile speed + freshness uncertainty`。
- 固定 AED：`pickup overhead + 2 × route distance / retrieval speed + access uncertainty`，其中两倍距离表示施救者到设备点后还需将 AED 带回求救者。
- `route distance = Haversine straight distance × route-distance-factor`。
- 移动设备定位越旧、越接近服务半径边界，不确定性惩罚越高。
- Fixed/Mobile 先分池各取 Top-K，再使用统一的风险 ETA 评分，避免粗筛阶段误删真正最优的移动设备。

## 跨场景标定数据

每条实测样本至少记录：

| 字段 | 说明 |
| --- | --- |
| `scene_type` | 校园/社区/场馆/园区/城市道路等 |
| `device_type` | `FIXED` 或 `MOBILE` |
| `straight_distance_m` | 坐标直线距离 |
| `route_distance_m` | 实际可通行路线距离 |
| `dispatch_overhead_s` | 响应确认、启动、门禁、取用和交接时间 |
| `travel_time_s` | 实际移动耗时 |
| `location_age_s` | 移动 AED 最后定位延迟 |
| `time_bucket` | 工作日/周末与高峰/平峰/夜间 |
| `weather` | 晴/雨/雪/极端高温等 |
| `transport_mode` | 步行/自行车/电动车/机动车 |

使用训练集拟合 `route-distance-factor`、速度和 overhead，使用独立验证集报告 MAE、P50/P90 绝对误差和“最优设备命中率”。当 P90 低估明显高于高估时，应优先增大不确定性缓冲，避免对用户给出过度乐观的到达承诺。

## 配置与运行时语义

| 环境变量 | 用途 |
| --- | --- |
| `DISPATCH_ROUTE_DISTANCE_FACTOR` | 直线距离到可通行距离的系数 |
| `DISPATCH_MOBILE_SPEED_KMH` | 移动 AED 当地标定速度 |
| `DISPATCH_RUNNER_SPEED_KMH` | 固定 AED 取送的当地标定速度 |
| `DISPATCH_MOBILE_OVERHEAD_SECONDS` | 移动资源响应与启动耗时 |
| `DISPATCH_FIXED_OVERHEAD_SECONDS` | 门禁、楼梯、取用与交接耗时 |

当前接口返回的 ETA 是匹配瞬间快照，`liveTracking` 是后续实时位置，两者不混为同一字段。要进一步支持城市拥堵和逐路段计算，应接入路网规划服务，并按区域、时段选择动态参数配置；在没有这些数据前，界面必须标注“预计”而非“承诺到达”。
