package cn.maian.agent.service;

import cn.maian.agent.dto.AssistantContext.Pending;
import java.util.*;
import java.util.regex.Pattern;

final class AssistantContinuation {
    private AssistantContinuation() {}
    private static final Set<String> KEYS = Set.of("name", "newName", "phone", "relation", "title", "category", "content", "hospital", "checkupDate", "indicators", "rating", "comment", "reviewNote", "approved", "role", "key", "value", "address", "description", "type", "serviceTime", "dueAt");

    static Set<String> editable(Pending pending) {
        if (pending == null || pending.operation() == null || !AssistantOperations.ALLOWED.contains(pending.operation()) || pending.editableKeys() == null) return Set.of();
        var keys = new HashSet<>(pending.editableKeys()); keys.retainAll(KEYS); return keys;
    }

    static Map<String,String> local(String message, Pending pending) {
        var keys = editable(pending);
        if (keys.isEmpty() || AssistantOperations.hasMultipleInstructions(message)
            || message.matches(".*(不要|别|如果|假如|比如|取消|停止|算了|在哪|如何|怎么).*")) return Map.of();
        // Independent business requests and navigation must not become values in the old task.
        var independent = AssistantOperations.local(message);
        if (independent.size() == 1 && independent.getFirst().operation().equals(pending.operation()) && !message.matches(".*(再添加|另外|重新添加|另一个).*")) {
            var values = independent.getFirst().parameters();
            if (!values.isEmpty() && keys.containsAll(values.keySet())) return values;
            if (values.size() == 1 && pending.operation().equals("SETTINGS_UPDATE") && keys.containsAll(Set.of("key","value"))) {
                var entry = values.entrySet().iterator().next(); return Map.of("key",entry.getKey(),"value",entry.getValue());
            }
        }
        if (!independent.isEmpty() || !AssistantNavigation.find(message).isEmpty()
            || message.matches(".*(上传|导入|查看|查询|推荐|打开|关闭|删除|停用|接单|导航|提醒).*")) return Map.of();
        var result = new LinkedHashMap<String,String>();
        if (Set.of("CONTACT_UPDATE","DEVICE_UPDATE").contains(pending.operation()) && keys.containsAll(Set.of("key","value"))) {
            var labels = Map.of("手机号","phone","电话","phone","名字","newName","名称","newName","关系","relation","地址","address","服务时间","serviceTime");
            for (var entry : labels.entrySet()) {
                var match = Pattern.compile(entry.getKey() + "[是为叫：:改成设成\\s]*([^，,；;]+)").matcher(message);
                if (match.find()) return Map.of("key",entry.getValue(),"value",match.group(1).trim());
            }
        }
        var labels = Map.ofEntries(Map.entry("name", "姓名|名字|名称"), Map.entry("newName", "新名字|新名称|名字|名称"), Map.entry("phone", "手机号|电话|手机"), Map.entry("relation", "关系"), Map.entry("title", "标题"), Map.entry("hospital", "医院|机构"), Map.entry("address", "地址|位置"), Map.entry("description", "现场情况"), Map.entry("comment", "评价内容|评语"), Map.entry("serviceTime", "开放时间|服务时间"));
        labels.forEach((key,label) -> {
            if (!keys.contains(key)) return;
            var match = Pattern.compile("(?:" + label + ")(?:改成|改为|设为|是|为|叫|[:：\\s])*([^，,；;\\n]+)").matcher(message);
            if (match.find()) result.put(key, match.group(1).trim());
        });
        if (keys.contains("dueAt")) { var time = AssistantReminderLanguage.time(message, java.time.Instant.now()); if (time != null) result.put("dueAt", time.toString()); }
        var phone = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)").matcher(message);
        if (keys.contains("phone") && phone.find()) result.put("phone", phone.group());
        if (keys.contains("relation")) {
            var relation = Pattern.compile("母亲|父亲|妈妈|爸爸|家人|同学|朋友|配偶|夫妻|同事|兄弟|姐妹").matcher(message);
            if (relation.find()) result.put("relation", relation.group());
        }
        var missing = pending.missingKeys() == null ? List.<String>of() : pending.missingKeys();
        String plain = message.replaceFirst("^(?:是|叫|就叫|就是|我)(?=.{1,30}$)", "").trim();
        if (keys.contains("name") && missing.contains("name") && !result.containsKey("name") && (result.isEmpty() || plain.equals(result.get("relation"))) && plain.matches("[\\p{IsHan}A-Za-z·]{1,12}")) result.put("name", plain);
        if (missing.size() == 1 && keys.contains(missing.getFirst()) && result.isEmpty() && !Set.of("key", "role", "type", "category", "approved", "phone").contains(missing.getFirst()) && message.length() <= 120) result.put(missing.getFirst(), message.trim());
        return result;
    }
}
