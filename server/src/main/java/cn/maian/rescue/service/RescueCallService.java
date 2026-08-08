package cn.maian.rescue.service;

import cn.maian.common.exception.ForbiddenOperationException;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.config.DispatchProperties;
import cn.maian.device.domain.DeviceType;
import cn.maian.rescue.domain.ActiveRescueLock;
import cn.maian.rescue.domain.RescueCall;
import cn.maian.rescue.domain.RescueEvent;
import cn.maian.rescue.domain.RescueEventType;
import cn.maian.rescue.domain.RescueStatus;
import cn.maian.rescue.domain.ResponderPresence;
import cn.maian.rescue.dto.CreateRescueCallRequest;
import cn.maian.rescue.dto.RequesterRescueResponse;
import cn.maian.rescue.dto.ResponderTaskResponse;
import cn.maian.rescue.repository.ActiveRescueLockRepository;
import cn.maian.rescue.repository.RescueCallRepository;
import cn.maian.rescue.repository.RescueEventRepository;
import cn.maian.rescue.repository.ResponderPresenceRepository;
import cn.maian.user.repository.UserProfileRepository;
import cn.maian.user.service.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class RescueCallService {

    private static final EnumSet<RescueStatus> ACTIVE_STATUSES = EnumSet.of(
        RescueStatus.PENDING,
        RescueStatus.MATCHING,
        RescueStatus.EN_ROUTE_TO_AED,
        RescueStatus.EN_ROUTE_TO_REQUESTER,
        RescueStatus.ARRIVED,
        RescueStatus.RESCUING,
        RescueStatus.PENDING_CONFIRMATION
    );
    private static final int OFFER_CANDIDATE_LIMIT = 200;
    private static final double LATITUDE_KM_PER_DEGREE = 111.32;

    private final RescueCallRepository rescueCallRepository;
    private final AedDispatchService aedDispatchService;
    private final CurrentUserService currentUserService;
    private final UserProfileRepository userProfileRepository;
    private final ActiveRescueLockRepository activeRescueLockRepository;
    private final ResponderPresenceRepository responderPresenceRepository;
    private final RescueEventRepository rescueEventRepository;
    private final DispatchProperties dispatchProperties;
    private final Clock clock;
    private final RescueRealtimeHub rescueRealtimeHub;

    public RescueCallService(
        RescueCallRepository rescueCallRepository,
        AedDispatchService aedDispatchService,
        CurrentUserService currentUserService,
        UserProfileRepository userProfileRepository,
        ActiveRescueLockRepository activeRescueLockRepository,
        ResponderPresenceRepository responderPresenceRepository,
        RescueEventRepository rescueEventRepository,
        DispatchProperties dispatchProperties,
        Clock clock,
        RescueRealtimeHub rescueRealtimeHub
    ) {
        this.rescueCallRepository = rescueCallRepository;
        this.aedDispatchService = aedDispatchService;
        this.currentUserService = currentUserService;
        this.userProfileRepository = userProfileRepository;
        this.activeRescueLockRepository = activeRescueLockRepository;
        this.responderPresenceRepository = responderPresenceRepository;
        this.rescueEventRepository = rescueEventRepository;
        this.dispatchProperties = dispatchProperties;
        this.clock = clock;
        this.rescueRealtimeHub = rescueRealtimeHub;
    }

    @Transactional
    public RequesterRescueResponse create(CreateRescueCallRequest request) {
        UUID requesterId = currentUserService.currentUserId();
        userProfileRepository.findForUpdateById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("当前用户不存在"));

        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var idempotent = rescueCallRepository.findByClientRequestIdAndRequestedByUserId(
                request.clientRequestId(), requesterId
            );
            if (idempotent.isPresent()) {
                return RequesterRescueResponse.from(idempotent.orElseThrow());
            }
        }

        var activeCalls = rescueCallRepository.findActiveOwned(
            requesterId, ACTIVE_STATUSES, PageRequest.of(0, 1)
        );
        if (!activeCalls.isEmpty()) {
            return RequesterRescueResponse.from(activeCalls.getFirst());
        }

        Instant now = clock.instant();
        RescueCall rescueCall = RescueCall.create(
            request.urgency(), request.latitude(), request.longitude(), request.address(),
            request.description(), request.symptoms(), request.clientRequestId(),
            requesterId, now
        );
        rescueCall.beginMatching(now);
        rescueCall = rescueCallRepository.saveAndFlush(rescueCall);
        activeRescueLockRepository.save(new ActiveRescueLock(requesterId, rescueCall.getId(), now));
        appendEvent(rescueCall, 1, RescueEventType.CREATED, requesterId, "求救请求已创建", now);
        appendEvent(rescueCall, 2, RescueEventType.MATCHING_STARTED, null, "系统开始匹配 AED 和施救者", now);
        matchAndAssign(rescueCall);
        return RequesterRescueResponse.from(rescueCall);
    }

    @Transactional(readOnly = true)
    public RequesterRescueResponse findById(UUID id) {
        return RequesterRescueResponse.from(findOwned(id));
    }

    @Transactional(readOnly = true)
    public Page<RequesterRescueResponse> findAll(Pageable pageable) {
        return rescueCallRepository.findAllOwnedDetailed(currentUserService.currentUserId(), pageable)
            .map(RequesterRescueResponse::from);
    }

    @Transactional
    public void updatePresence(double latitude, double longitude, boolean available) {
        currentUserService.requireAnyRole("VOLUNTEER");
        UUID userId = currentUserService.currentUserId();
        Instant now = clock.instant();
        ResponderPresence presence = responderPresenceRepository.findById(userId)
            .orElseGet(() -> new ResponderPresence(userId, latitude, longitude, available, now));
        presence.update(latitude, longitude, available, now);
        responderPresenceRepository.save(presence);
    }

    @Transactional(readOnly = true)
    public Page<ResponderTaskResponse> findResponderTasks(Pageable pageable) {
        currentUserService.requireAnyRole("VOLUNTEER");
        UUID responderId = currentUserService.currentUserId();
        List<ResponderTaskResponse> tasks = new ArrayList<>();
        rescueCallRepository.findAssignedToResponder(
            responderId, ACTIVE_STATUSES, PageRequest.of(0, OFFER_CANDIDATE_LIMIT)
        ).stream().map(ResponderTaskResponse::assigned).forEach(tasks::add);

        responderPresenceRepository.findById(responderId)
            .filter(presence -> presence.isEligibleAt(
                clock.instant().minusSeconds(dispatchProperties.volunteerPresenceMaxAgeSeconds())
            ))
            .ifPresent(presence -> addNearbyOffers(tasks, responderId, presence));

        int start = Math.min((int) pageable.getOffset(), tasks.size());
        int end = Math.min(start + pageable.getPageSize(), tasks.size());
        return new PageImpl<>(tasks.subList(start, end), pageable, tasks.size());
    }

    @Transactional(readOnly = true)
    public ResponderTaskResponse findResponderTask(UUID id) {
        currentUserService.requireAnyRole("VOLUNTEER");
        UUID responderId = currentUserService.currentUserId();
        RescueCall call = rescueCallRepository.findDetailedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援任务：" + id));
        if (responderId.equals(call.getResponderUserId())) {
            return ResponderTaskResponse.assigned(call);
        }
        ResponderPresence presence = responderPresenceRepository.findById(responderId)
            .filter(value -> value.isEligibleAt(
                clock.instant().minusSeconds(dispatchProperties.volunteerPresenceMaxAgeSeconds())
            ))
            .orElseThrow(() -> new ForbiddenOperationException("请先上报实时位置并设为可接单"));
        ensureOfferVisible(call, responderId, presence);
        return ResponderTaskResponse.offer(call, distanceMeters(presence, call));
    }

    @Transactional
    public ResponderTaskResponse accept(UUID id) {
        currentUserService.requireAnyRole("VOLUNTEER");
        UUID responderId = currentUserService.currentUserId();
        RescueCall call = findForUpdate(id);
        ensureEligibleOffer(call, responderId);
        Instant now = clock.instant();
        call.acceptBy(responderId, now);
        appendCurrentEvent(call, RescueEventType.ACCEPTED, responderId, "施救者已接单", now);
        return ResponderTaskResponse.assigned(call);
    }

    @Transactional
    public ResponderTaskResponse arriveAtAed(UUID id) {
        return responderAction(id, RescueEventType.ARRIVED_AT_AED, "施救者已到达 AED 取用点", RescueCall::arriveAtAed);
    }

    @Transactional
    public ResponderTaskResponse pickUpAed(UUID id) {
        return responderAction(id, RescueEventType.AED_PICKED_UP, "AED 已取出，正在赶往求救者", RescueCall::pickUpAed);
    }

    @Transactional
    public ResponderTaskResponse arriveAtRequester(UUID id) {
        return responderAction(id, RescueEventType.ARRIVED_AT_REQUESTER, "施救者已到达现场", RescueCall::arriveAtRequester);
    }

    @Transactional
    public ResponderTaskResponse startRescue(UUID id) {
        return responderAction(id, RescueEventType.RESCUE_STARTED, "现场救援已开始", RescueCall::startRescue);
    }

    @Transactional
    public ResponderTaskResponse submitCompletion(UUID id) {
        return responderAction(id, RescueEventType.COMPLETION_SUBMITTED, "施救者已提交完成，等待求救者确认", RescueCall::submitCompletion);
    }

    @Transactional
    public RequesterRescueResponse confirmCompletion(UUID id) {
        UUID requesterId = currentUserService.currentUserId();
        RescueCall call = rescueCallRepository.findDetailedForUpdateById(id)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + id));
        if (!requesterId.equals(call.getRequestedByUserId())) {
            throw new ForbiddenOperationException("只有求救者可以确认救援完成");
        }
        Instant now = clock.instant();
        call.confirmCompletion(now);
        activeRescueLockRepository.deleteByRescueCallId(call.getId());
        appendCurrentEvent(call, RescueEventType.COMPLETION_CONFIRMED, requesterId, "求救者已确认救援完成", now);
        return RequesterRescueResponse.from(call);
    }

    @Transactional
    public ResponderTaskResponse returnAed(UUID id) {
        return responderAction(id, RescueEventType.AED_RETURNED, "AED 已归还", RescueCall::returnAed);
    }

    @Transactional
    public ResponderTaskResponse updateResponderLocation(UUID id, double latitude, double longitude) {
        currentUserService.requireAnyRole("VOLUNTEER");
        UUID responderId = currentUserService.currentUserId();
        RescueCall call = findForUpdate(id);
        call.updateResponderLocation(responderId, latitude, longitude, clock.instant());
        return ResponderTaskResponse.assigned(call);
    }

    @Transactional
    public RequesterRescueResponse cancel(UUID id) {
        UUID requesterId = currentUserService.currentUserId();
        RescueCall call = rescueCallRepository.findDetailedForUpdateById(id)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + id));
        Instant now = clock.instant();
        call.cancelByRequester(requesterId, now);
        activeRescueLockRepository.deleteByRescueCallId(call.getId());
        appendCurrentEvent(call, RescueEventType.USER_CANCELLED, requesterId, "求救者已取消任务", now);
        return RequesterRescueResponse.from(call);
    }

    private ResponderTaskResponse responderAction(
        UUID id,
        RescueEventType eventType,
        String summary,
        TimedRescueAction action
    ) {
        currentUserService.requireAnyRole("VOLUNTEER");
        UUID responderId = currentUserService.currentUserId();
        RescueCall call = findForUpdate(id);
        call.requireResponder(responderId);
        Instant now = clock.instant();
        action.apply(call, now);
        appendCurrentEvent(call, eventType, responderId, summary, now);
        return ResponderTaskResponse.assigned(call);
    }

    private void addNearbyOffers(
        List<ResponderTaskResponse> tasks,
        UUID responderId,
        ResponderPresence presence
    ) {
        double radiusKm = dispatchProperties.volunteerOfferRadiusKm();
        double latitudeRadius = radiusKm / LATITUDE_KM_PER_DEGREE;
        double longitudeRadius = radiusKm / (
            LATITUDE_KM_PER_DEGREE * Math.max(0.05, Math.cos(Math.toRadians(presence.getLatitude())))
        );
        var offers = rescueCallRepository.findMatchingOfferCandidates(
            presence.getLatitude() - latitudeRadius,
            presence.getLatitude() + latitudeRadius,
            presence.getLongitude() - longitudeRadius,
            presence.getLongitude() + longitudeRadius,
            clock.instant(),
            PageRequest.of(0, OFFER_CANDIDATE_LIMIT)
        );
        offers.stream()
            .filter(call -> isDeviceEligibleForResponder(call, responderId))
            .map(call -> new OfferDistance(call, distanceMeters(presence, call)))
            .filter(offer -> offer.distanceMeters() <= radiusKm * 1_000)
            .map(offer -> ResponderTaskResponse.offer(offer.call(), offer.distanceMeters()))
            .forEach(tasks::add);
    }

    private void ensureEligibleOffer(RescueCall call, UUID responderId) {
        if (call.getStatus() != RescueStatus.MATCHING || call.getResponderUserId() != null) {
            throw new cn.maian.common.exception.InvalidStateTransitionException("该任务已被接单或不可接取");
        }
        if (call.getMatchDeadlineAt() == null || !call.getMatchDeadlineAt().isAfter(clock.instant())) {
            throw new cn.maian.common.exception.InvalidStateTransitionException("该任务已超时");
        }
        ResponderPresence presence = responderPresenceRepository.findById(responderId)
            .filter(value -> value.isEligibleAt(
                clock.instant().minusSeconds(dispatchProperties.volunteerPresenceMaxAgeSeconds())
            ))
            .orElseThrow(() -> new ForbiddenOperationException("请先上报实时位置并设为可接单"));
        ensureOfferVisible(call, responderId, presence);
    }

    private void ensureOfferVisible(
        RescueCall call,
        UUID responderId,
        ResponderPresence presence
    ) {
        if (call.getStatus() != RescueStatus.MATCHING || call.getResponderUserId() != null) {
            throw new cn.maian.common.exception.InvalidStateTransitionException("该任务已被接单或不可接取");
        }
        if (call.getMatchDeadlineAt() == null || !call.getMatchDeadlineAt().isAfter(clock.instant())) {
            throw new cn.maian.common.exception.InvalidStateTransitionException("该任务已超时");
        }
        if (!isDeviceEligibleForResponder(call, responderId)) {
            throw new ForbiddenOperationException("该移动 AED 仅可由设备携带者接单");
        }
        if (distanceMeters(presence, call) > dispatchProperties.volunteerOfferRadiusKm() * 1_000) {
            throw new ForbiddenOperationException("该救援任务不在当前可接单范围内");
        }
    }

    private boolean isDeviceEligibleForResponder(RescueCall call, UUID responderId) {
        return call.getMatchedDevice() != null
            && (call.getMatchedDevice().getType() == DeviceType.FIXED
                || responderId.equals(call.getMatchedDevice().getRegisteredByUserId()));
    }

    private int distanceMeters(ResponderPresence presence, RescueCall call) {
        return Math.max(0, (int) Math.round(AedDispatchScorer.haversineMeters(
            presence.getLatitude(), presence.getLongitude(), call.getLatitude(), call.getLongitude()
        )));
    }

    private RescueCall findOwned(UUID id) {
        return rescueCallRepository.findOwnedDetailedById(id, currentUserService.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援请求：" + id));
    }

    private RescueCall findForUpdate(UUID id) {
        return rescueCallRepository.findDetailedForUpdateById(id)
            .orElseThrow(() -> new ResourceNotFoundException("未找到救援任务：" + id));
    }

    private void matchAndAssign(RescueCall call) {
        aedDispatchService.matchFastest(call).ifPresent(match -> {
            call.assignDevice(
                match.device(), match.matchedAt(), match.score().distanceMeters(),
                match.score().estimatedArrivalSeconds(), match.score().strategy()
            );
            appendCurrentEvent(call, RescueEventType.AED_ASSIGNED, null, "系统已锁定最快 AED", match.matchedAt());
        });
    }

    private void appendCurrentEvent(
        RescueCall call,
        RescueEventType type,
        UUID actor,
        String summary,
        Instant now
    ) {
        appendEvent(call, call.getEventSequence(), type, actor, summary, now);
    }

    private void appendEvent(
        RescueCall call,
        long sequence,
        RescueEventType type,
        UUID actor,
        String summary,
        Instant now
    ) {
        rescueEventRepository.save(new RescueEvent(call.getId(), sequence, type, actor, summary, now));
        rescueRealtimeHub.publishAfterCommit(call, type);
    }

    @FunctionalInterface
    private interface TimedRescueAction {
        void apply(RescueCall call, Instant now);
    }

    private record OfferDistance(RescueCall call, int distanceMeters) {
    }
}
