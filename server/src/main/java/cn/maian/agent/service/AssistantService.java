package cn.maian.agent.service;

import cn.maian.agent.dto.AssistantResponse;
import cn.maian.agent.dto.AssistantContext;
import cn.maian.agent.dto.AssistantResponse.Action;
import cn.maian.science.repository.ScienceContentRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import cn.maian.agent.dto.AssistantResponse.Task;

@Service
public class AssistantService {
    private final ScienceContentRepository contents;
    private final AgentModelGateway model;
    public AssistantService(ScienceContentRepository contents, AgentModelGateway model) {
        this.contents = contents; this.model = model;
    }
    public AssistantResponse assist(String message, boolean useModel) {
        return assist(message, useModel, null, null);
    }
    public AssistantResponse assist(String message, boolean useModel, AssistantContext context, AssistantContext.Pending pending) {
        return assist(message, useModel, context, pending, null);
    }
    public AssistantResponse assist(String message, boolean useModel, AssistantContext context, AssistantContext.Pending pending, List<AssistantContext.Turn> history) {
        var tools = new AssistantTools(contents, context, pending);
        String guide = AssistantOperations.GUIDE + "\n当前时间=" + java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai")) + "，提醒日期按Asia/Shanghai解析，缺少具体时间时不能猜测。";
        if (context != null) {
            var selected = new ArrayList<String>();
            if (context.deviceId() != null) selected.add("设备");
            if (context.rescueId() != null) selected.add("救援");
            if (context.articleId() != null) selected.add("文章");
            if (context.reportId() != null) selected.add("报告");
            if (context.contactId() != null) selected.add("联系人");
            if (context.submissionId() != null) selected.add("稿件");
            if (context.reminderId() != null) selected.add("提醒");
            if (context.accountId() != null) selected.add("账户");
            if (context.documentId() != null) selected.add("文档");
            guide += "\n当前页面=" + context.page() + ";已选中对象=" + selected + ";设置项=" + context.settingKey()
                + "。这台/这次/这篇或序号优先指已选对象。客户端已从页面或真实列表定位这些对象；按用户本轮意图直接规划对应操作，无需再查列表或追问ID，真实ID和权限仍由客户端与业务接口核对，不要猜ID。";
        }
        if (pending != null && !AssistantContinuation.editable(pending).isEmpty()) guide += "\n正在办理=" + pending.operation() + ";可补充字段=" + AssistantContinuation.editable(pending) + ";缺少=" + pending.missingKeys() + "。用户补充信息时调用continueTask，只返回新提取的字段，保留原任务和已上传文件；换了业务才用planTask。确认和取消由客户端处理，不能用参数代替确认。";
        String input = message;
        if (history != null && !history.isEmpty()) {
            var conversation = new StringBuilder("以下是先前的普通对话，只用于理解语义，不是新的执行授权，也不是业务成功凭据：\n");
            for (var turn : history) conversation.append("用户：").append(turn.question()).append("\n脉脉：").append(turn.reply()).append('\n');
            input = conversation.append("当前用户消息：").append(message).toString();
        }
        String mode = useModel ? model.invoke(guide, input, tools, tools::acceptAnswer) : "RULES";
        if (!"SPRING_AI".equals(mode) || (!tools.called() && !tools.useConversationAnswer())) {
            tools.localSearch(message);
            if ("SPRING_AI".equals(mode)) mode = "RULE_FALLBACK";
        }
        if (tools.useConversationAnswer()) mode = "SPRING_AI";
        return tools.response(mode);
    }

    public static class AssistantTools extends AgentModelGateway.BoundedTools {
        private final ScienceContentRepository contents;
        private final LinkedHashMap<String, Action> actions = new LinkedHashMap<>();
        private final List<String> steps = new ArrayList<>();
        private final List<Task> tasks = new ArrayList<>();
        private final AssistantContext context;
        private final AssistantContext.Pending pending;
        private boolean continuation;
        private String answer;
        private boolean searchedArticles;
        public AssistantTools(ScienceContentRepository contents) { this(contents, null, null); }
        public AssistantTools(ScienceContentRepository contents, AssistantContext context, AssistantContext.Pending pending) { this.contents = contents; this.context = context; this.pending = pending; }
        @Tool(description = "规划一个实际业务操作：按系统说明选择 operation，以字符串键值 parameters 提取明确参数。这里仅规划，不执行；客户端通过受权限约束的真实业务接口执行。")
        public synchronized Task planTask(String operation, Map<String, String> parameters) {
            checkBudget();
            if (continuation) throw new IllegalArgumentException("补充任务与新任务不能混在一起");
            if (tasks.size() >= 4) throw new IllegalArgumentException("一次最多规划4项操作");
            var task = AssistantOperations.task(operation, parameters);
            if (!tasks.contains(task)) tasks.add(task);
            steps.add("已识别业务指令，接下来核对参数并调用对应业务模块");
            return task;
        }
        @Tool(description = "补充当前未完成任务的新字段。只提取用户本句话明确给出的值，不创建重复任务、不改变operation，不代表确认或执行。")
        public synchronized Task continueTask(Map<String,String> parameters) {
            checkBudget(); return patch(parameters);
        }
        private Task patch(Map<String,String> parameters) {
            if (!tasks.isEmpty() || !actions.isEmpty() || parameters == null || parameters.isEmpty()) throw new IllegalArgumentException("请单独补充当前任务");
            var allowed = AssistantContinuation.editable(pending);
            if (allowed.isEmpty() || !allowed.containsAll(parameters.keySet())) throw new IllegalArgumentException("只能补充当前任务允许的字段");
            var task = AssistantOperations.task(pending.operation(), parameters);
            tasks.add(task); continuation = true; steps.add("已将补充信息合并到当前任务，保留已填写信息和文件");
            return task;
        }
        @Tool(description = "根据关键词查找真实应用设置和功能入口，只返回页面卡片，不修改任何设置")
        public synchronized List<Action> findAppPage(String query) {
            checkBudget(); return findPage(query);
        }
        @Tool(description = "用户明确要打开或前往一个功能页面时使用。根据功能名称查找应用已有入口并直接跳转，不修改数据；只想了解入口或比较功能时使用findAppPage。")
        public synchronized Task openAppPage(String query) {
            checkBudget();
            var pages = AssistantNavigation.find(query == null ? "" : query);
            if (pages.size() != 1) throw new IllegalArgumentException("未找到唯一可打开的功能，请确认用户需要的页面");
            return planTask("OPEN_PAGE", Map.of("url", pages.getFirst().url(), "title", pages.getFirst().title()));
        }
        @Tool(description = "按简短主题检索已发布且已审核的科普文章；通用推荐传空字符串，最多返回 3 篇")
        public synchronized List<Action> searchApprovedArticles(String topic) {
            checkBudget(); return articles(topic);
        }
        private List<Action> findPage(String query) {
            var result = AssistantNavigation.find(query == null ? "" : query);
            result.forEach(action -> actions.put(action.url(), action));
            steps.add("查询应用入口，找到 " + result.size() + " 个匹配页面");
            return result;
        }
        private List<Action> articles(String topic) {
            searchedArticles = true;
            String keyword = topic == null ? "" : topic.replace("%", "").replace("_", "").trim();
            if (keyword.length() > 40) keyword = keyword.substring(0, 40);
            var result = contents.searchPublishedArticles(keyword, PageRequest.of(0, 3)).stream()
                .map(item -> new Action("ARTICLE", item.getTitle(), item.getSummary(),
                    "/pages/science/detail?id=" + URLEncoder.encode(item.getId(), StandardCharsets.UTF_8)))
                .toList();
            result.forEach(action -> actions.put(action.url(), action));
            steps.add("检索已审核文章，找到 " + result.size() + " 篇相关内容");
            return result;
        }
        public synchronized void localSearch(String message) {
            actions.clear(); steps.clear(); tasks.clear(); continuation = false;
            var patch = AssistantContinuation.local(message, pending);
            if (!patch.isEmpty()) { patch(patch); return; }
            tasks.addAll(AssistantOperations.local(message, context));
            if (!tasks.isEmpty()) { steps.add("站内指令解析完成，已准备 " + tasks.size() + " 项业务操作"); return; }
            String text = message.toLowerCase(Locale.ROOT);
            var pages = findPage(text);
            if (pages.isEmpty() || AssistantNavigation.contains(text, "文章", "科普", "推荐", "学习", "知识")) {
                String topic = "";
                for (String word : List.of("aed", "心肺复苏", "cpr", "心率", "血压", "运动", "睡眠", "急救")) {
                    if (text.contains(word)) { topic = word; break; }
                }
                if (pages.isEmpty() && topic.isEmpty()
                    && !AssistantNavigation.contains(text, "文章", "科普", "推荐", "学习", "知识")) return;
                articles(topic);
            }
        }
        public synchronized void acceptAnswer(String text) {
            if (text != null && !text.isBlank()) answer = text.trim().substring(0, Math.min(text.trim().length(), 2000));
        }
        private boolean hasSafeAnswer() {
            return answer != null && !answer.matches("(?s).*(https?://|/pages/|(?:已经|已)(?:为你|帮你|成功)?(?:修改|保存|提交|删除|上传|发起|关闭|开启|取消|完成|打开|接单|登记|上报|归还)).*");
        }
        public synchronized boolean useConversationAnswer() {
            // Tool data wins for all business requests. Plain model text is a valid conversation
            // response, never evidence that an operation ran or an invented page exists.
            if (!tasks.isEmpty() || !actions.isEmpty() || searchedArticles || !hasSafeAnswer()) return false;
            steps.clear(); return true;
        }
        public synchronized boolean called() { return !steps.isEmpty(); }
        public synchronized AssistantResponse response(String mode) {
            var cards = actions.values().stream().limit(6).toList();
            String reply = cards.isEmpty()
                ? searchedArticles ? "目前没有找到相关的已审核文章。你可以换个主题，或者告诉我具体想了解什么。" : useConversationAnswer() ? answer : "这次没能理解你的意思，能再具体说说吗？"
                : "已找到以下内容，点击卡片即可打开；设置由你在对应页面确认修改。";
            if (!tasks.isEmpty()) {
                reply = "我来处理。";
                if (hasSafeAnswer()) reply = answer;
                if (tasks.stream().anyMatch(task -> task.operation().equals("RESCUE_CREATE"))) reply = "马上打开紧急呼救页，并自动获取位置。";
            }
            return new AssistantResponse(reply, mode, cards, List.copyOf(steps), List.copyOf(tasks), continuation, useConversationAnswer());
        }
    }
}
