package cn.maian.agent.service;

import cn.maian.agent.domain.RescueReview;
import cn.maian.agent.dto.CoordinationTaskResponse;
import cn.maian.agent.dto.RescueReviewResponse;
import cn.maian.agent.dto.RescueReviewResponse.*;
import cn.maian.agent.repository.RescueReviewRepository;
import cn.maian.common.exception.InvalidStateTransitionException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.rescue.domain.*;
import cn.maian.rescue.repository.*;
import cn.maian.science.domain.ScienceContentKind;
import cn.maian.science.repository.ScienceContentRepository;
import cn.maian.user.service.CurrentUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RescueReviewService {
    private final RescueReviewRepository reviews;
    private final RescueCallRepository calls;
    private final RescueEventRepository events;
    private final RescueFeedbackRepository feedback;
    private final ScienceContentRepository contents;
    private final CurrentUserService users;
    private final ObjectMapper json;
    private final Clock clock;

    public RescueReviewService(RescueReviewRepository reviews, RescueCallRepository calls, RescueEventRepository events,
        RescueFeedbackRepository feedback, ScienceContentRepository contents, CurrentUserService users, ObjectMapper json, Clock clock) {
        this.reviews = reviews; this.calls = calls; this.events = events; this.feedback = feedback;
        this.contents = contents; this.users = users; this.json = json; this.clock = clock;
    }

    @Transactional
    public RescueReviewResponse request(UUID id, boolean useModel) {
        var call = calls.findDetailedForUpdateById(id).orElseThrow(() -> missing());
        requireParticipant(call);
        if (!call.getStatus().isTerminal()) throw new InvalidStateTransitionException("救援结束后才能生成复盘");
        var review = reviews.findForUpdate(id).orElse(null);
        if (review == null) reviews.save(RescueReview.create(id, useModel, clock.instant()));
        else if (!review.inProgress(clock.instant())
            && (!"COMPLETED".equals(review.getStatus()) || stale(review, call) || review.isUseModel() != useModel)) {
            review.request(useModel, clock.instant());
        }
        return read(id);
    }

    @Transactional(readOnly = true)
    public RescueReviewResponse read(UUID id) {
        var call = calls.findDetailedById(id).orElseThrow(() -> missing());
        requireParticipant(call);
        var review = reviews.findById(id).orElse(null);
        if (review == null) return null;
        Data data = review.getResultJson() == null || !"COMPLETED".equals(review.getStatus())
            ? null : decode(review.getResultJson());
        var articles = data == null ? List.<Article>of() : liveArticles(data.articleIds());
        return new RescueReviewResponse(id, review.getStatus(), review.getExecutionMode(), review.getSummary(),
            data != null && stale(review, call), review.getUpdatedAt(), review.getCompletedAt(),
            review.getSteps().stream().map(step -> new CoordinationTaskResponse.Step(step.getModule(), step.getSummary(), step.getOccurredAt())).toList(),
            data, articles);
    }

    @Transactional(readOnly = true)
    public List<UUID> pending() { return reviews.findExecutable(clock.instant(), PageRequest.of(0, 5)); }

    public record Claim(UUID token, boolean useModel) {}
    @Transactional
    public Claim claim(UUID id) {
        var review = locked(id);
        var token = review.claim(clock.instant());
        return token == null ? null : new Claim(token, review.isUseModel());
    }

    @Transactional
    public String inspectTimeline(UUID id, UUID token) {
        var review = running(id, token);
        var call = calls.findDetailedById(id).orElseThrow(() -> missing());
        var timeline = timeline(id);
        review.addStep("救援事件", "核对 " + timeline.size() + " 条最近事件（最多 100 条）及真实记录的响应、到场和确认时间", clock.instant());
        return "terminal_status=" + call.getStatus() + "; event_count=" + timeline.size();
    }

    @Transactional
    public String inspectFollowUps(UUID id, UUID token) {
        var review = running(id, token);
        var call = calls.findDetailedById(id).orElseThrow(() -> missing());
        boolean returnPending = call.getAedCustodyStatus() == AedCustodyStatus.RETURNING;
        boolean feedbackPending = call.getStatus() == RescueStatus.COMPLETED && !feedback.existsByRescueCallId(id);
        review.addStep("设备与评价", (returnPending ? "固定 AED 仍待志愿者确认归还" : "当前无待归还设备")
            + "；" + (feedbackPending ? "求救者尚未评价" : call.getStatus() == RescueStatus.COMPLETED ? "求救者已提交评价" : "本次结束状态无需评价"), clock.instant());
        return "return_pending=" + returnPending + "; feedback_pending=" + feedbackPending;
    }

    @Transactional
    public String findLearningMaterials(UUID id, UUID token) {
        var review = running(id, token);
        var articles = contents.searchPublishedArticles("AED", PageRequest.of(0, 3));
        review.addStep("科普检索", "检索已审核、已发布的 AED 学习文章，找到 " + articles.size() + " 篇", clock.instant());
        return "article_count=" + articles.size();
    }

    @Transactional
    public String assemble(UUID id, UUID token) {
        var review = running(id, token);
        var call = calls.findDetailedById(id).orElseThrow(() -> missing());
        if (!call.getStatus().isTerminal()) throw new InvalidStateTransitionException("救援尚未结束");
        boolean present = feedback.existsByRescueCallId(id);
        var data = new Data(List.of(metric("请求至响应", call.getCreatedAt(), call.getAcceptedAt()),
                metric("响应至到场", call.getAcceptedAt(), call.getArrivedAt()),
                metric("提交至确认", call.getCompletionSubmittedAt(), call.getCompletedAt())), timeline(id),
            call.getAedCustodyStatus() == AedCustodyStatus.RETURNING,
            call.getStatus() == RescueStatus.COMPLETED && !present,
            call.getAedCustodyStatus() == null ? null : call.getAedCustodyStatus().name(),
            contents.searchPublishedArticles("AED", PageRequest.of(0, 3)).stream().map(item -> item.getId()).toList());
        int pending = (data.returnPending() ? 1 : 0) + (data.feedbackPending() ? 1 : 0);
        String summary = "已整理 " + data.timeline().size() + " 条最近流程事件，核对 " + pending + " 项业务待办";
        review.record(encode(data), call.getEventSequence(), present, summary, clock.instant());
        return "review_assembled; pending_count=" + pending;
    }

    @Transactional
    public void complete(UUID id, UUID token, String mode) { running(id, token).complete(mode, clock.instant()); }
    @Transactional
    public void fail(UUID id, UUID token) { locked(id).fail(token, clock.instant()); }

    private List<Event> timeline(UUID id) {
        var values = new ArrayList<>(events.findTop100ByRescueCallIdOrderBySequenceDesc(id));
        Collections.reverse(values);
        return values.stream().map(event -> new Event(event.getType().name(), event.getSummary(), event.getCreatedAt())).toList();
    }
    private Metric metric(String label, Instant from, Instant to) {
        return new Metric(label, from == null || to == null || to.isBefore(from) ? null : Duration.between(from, to).toSeconds());
    }
    private boolean stale(RescueReview review, RescueCall call) {
        return review.getSourceSequence() != call.getEventSequence()
            || review.isFeedbackPresent() != feedback.existsByRescueCallId(call.getId());
    }
    private List<Article> liveArticles(List<String> ids) {
        var visible = contents.findAllById(ids).stream().filter(item -> item.isActive() && item.getKind() == ScienceContentKind.ARTICLE)
            .collect(Collectors.toMap(item -> item.getId(), Function.identity()));
        return ids.stream().filter(visible::containsKey).map(id -> {
            var item = visible.get(id); return new Article(item.getId(), item.getTitle(), item.getSummary());
        }).toList();
    }
    private void requireParticipant(RescueCall call) {
        UUID user = users.currentUserId();
        if (!user.equals(call.getRequestedByUserId()) && !user.equals(call.getResponderUserId())) throw missing();
    }
    private RescueReview running(UUID id, UUID token) {
        var review = locked(id); review.requireExecution(token, clock.instant()); return review;
    }
    private RescueReview locked(UUID id) { return reviews.findForUpdate(id).orElseThrow(() -> missing()); }
    private ResourceNotFoundException missing() { return new ResourceNotFoundException("救援复盘不存在"); }
    private String encode(Data data) {
        try { return json.writeValueAsString(data); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("无法保存复盘结果", exception); }
    }
    private Data decode(String value) {
        try { return json.readValue(value, Data.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("无法读取复盘结果", exception); }
    }
}
