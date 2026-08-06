package cn.maian.science.service;

import cn.maian.science.domain.ScienceSubmission;
import cn.maian.science.dto.CreateScienceSubmissionRequest;
import cn.maian.science.dto.ScienceSubmissionCountResponse;
import cn.maian.science.dto.ScienceSubmissionResponse;
import cn.maian.science.dto.ReviewScienceSubmissionRequest;
import cn.maian.science.domain.SubmissionStatus;
import cn.maian.science.repository.ScienceSubmissionRepository;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.user.service.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ScienceSubmissionService {

    private final ScienceSubmissionRepository scienceSubmissionRepository;
    private final CurrentUserService currentUserService;

    public ScienceSubmissionService(
        ScienceSubmissionRepository scienceSubmissionRepository,
        CurrentUserService currentUserService
    ) {
        this.scienceSubmissionRepository = scienceSubmissionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ScienceSubmissionResponse create(CreateScienceSubmissionRequest request) {
        var submission = ScienceSubmission.create(
            currentUserService.currentUserId(),
            request.title().trim(),
            request.category(),
            request.content().trim(),
            request.coverImageUrl()
        );
        return ScienceSubmissionResponse.from(scienceSubmissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public ScienceSubmissionCountResponse countCurrentUserSubmissions() {
        return new ScienceSubmissionCountResponse(
            scienceSubmissionRepository.countByUserId(currentUserService.currentUserId())
        );
    }

    @Transactional(readOnly = true)
    public Page<ScienceSubmissionResponse> findAll(Pageable pageable) {
        return scienceSubmissionRepository
            .findAllByUserId(currentUserService.currentUserId(), pageable)
            .map(ScienceSubmissionResponse::from);
    }

    @Transactional(readOnly = true)
    public ScienceSubmissionResponse findById(UUID id) {
        return ScienceSubmissionResponse.from(findOwned(id));
    }

    /** 已审核通过的投稿，供科普频道展示。 */
    @Transactional(readOnly = true)
    public Page<ScienceSubmissionResponse> findApproved(Pageable pageable) {
        return scienceSubmissionRepository
            .findAllByStatus(SubmissionStatus.APPROVED, pageable)
            .map(ScienceSubmissionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ScienceSubmissionResponse> findPendingReviews(Pageable pageable) {
        currentUserService.requireAnyRole("ADMIN");
        return scienceSubmissionRepository.findAllByStatus(SubmissionStatus.PENDING, pageable)
            .map(ScienceSubmissionResponse::from);
    }

    @Transactional
    public ScienceSubmissionResponse review(UUID id, ReviewScienceSubmissionRequest request) {
        currentUserService.requireAnyRole("ADMIN");
        var submission = scienceSubmissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("投稿不存在"));
        try {
            submission.review(request.approved(), request.reviewNote());
        } catch (IllegalStateException exception) {
            throw new cn.maian.common.exception.InvalidStateTransitionException(exception.getMessage());
        }
        return ScienceSubmissionResponse.from(submission);
    }

    @Transactional
    public void delete(UUID id) {
        scienceSubmissionRepository.delete(findOwned(id));
    }

    /** 按当前用户校验归属，避免通过 id 访问他人投稿。 */
    private ScienceSubmission findOwned(UUID id) {
        var submission = scienceSubmissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("投稿不存在"));
        if (!currentUserService.currentUserId().equals(submission.getUserId())) {
            throw new ResourceNotFoundException("投稿不存在");
        }
        return submission;
    }
}
