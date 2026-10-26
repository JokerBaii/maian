package cn.maian.agent.service;

import cn.maian.agent.dto.AssistantResponse.Action;
import java.util.List;
import java.util.Locale;

public final class AssistantNavigation {
    private AssistantNavigation() {}
    public static List<Action> find(String query) {
        String text = query.toLowerCase(Locale.ROOT);
        if (contains(text, "紧急呼救", "发起求救", "急救界面")) return List.of(new Action("PAGE", "紧急呼救", "打开紧急呼救页面", "/pages/rescue/index"));
        if (contains(text, "急救地图", "附近地图")) return List.of(new Action("PAGE", "急救地图", "查看附近急救设备", "/pages/map/index"));
        if (contains(text, "健康档案", "体检报告")) return List.of(new Action("PAGE", "健康档案", "查看已保存的体检报告", "/pages/checkup/archive"));
        if (contains(text, "救援记录", "救援历史")) return List.of(new Action("PAGE", "救援记录", "查看自己的救援记录", "/pages/mine/records"));
        if (contains(text, "健康首页", "健康监测")) return List.of(new Action("PAGE", "健康监测", "查看健康监测记录", "/pages/health/index"));
        if (contains(text, "科普中心", "科普首页")) return List.of(new Action("PAGE", "科普中心", "查看急救与健康知识", "/pages/science/index"));
        if (contains(text, "资源协同", "协同记录", "协同进展", "协同任务", "故障处理进度")) {
            return List.of(new Action("PAGE", "资源协同", "查看设备变化后的检查过程与处理结果", "/pages/agent/index"));
        }
        if (contains(text, "阈值", "最高心率", "最低心率", "心率上限", "心率下限")) {
            return List.of(setting("心率预警阈值", "查看并手动调整最高、最低心率阈值", "thresholds"));
        }
        if (contains(text, "隐私", "数据共享", "位置共享", "授权", "定位", "位置权限")) {
            return List.of(setting("隐私设置", "查看位置共享与健康数据共享；系统定位权限需在手机设置中管理", "privacy"));
        }
        if (contains(text, "通知", "推送", "提醒", "预警", "科普更新")) {
            return List.of(setting("通知设置", "查看呼救推送、健康预警与科普更新开关", "notifications"));
        }
        if (contains(text, "联系人", "联系家人")) {
            return List.of(new Action("PAGE", "紧急联系人", "管理紧急联系人信息", "/pages/mine/contacts"));
        }
        if (contains(text, "绑定", "手环", "穿戴", "蓝牙")) {
            return List.of(new Action("PAGE", "穿戴设备绑定", "连接或管理心率监测设备", "/pages/health/bind"));
        }
        if (contains(text, "维护", "我的设备", "设备管理", "设备故障", "共享设备")) {
            return List.of(new Action("PAGE", "我的急救设备", "管理设备、上报故障并查看协同结果", "/pages/device/manage"));
        }
        if (contains(text, "设置", "缓存", "退出登录")) {
            return List.of(new Action("PAGE", "设置", "通知、隐私、预警阈值及账户操作", "/pages/mine/settings"));
        }
        return List.of();
    }
    private static Action setting(String title, String description, String section) {
        return new Action("PAGE", title, description, "/pages/mine/settings?section=" + section);
    }
    public static boolean contains(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
}
