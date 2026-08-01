package cn.maian.science.service;

import cn.maian.science.domain.ScienceSubmission;
import cn.maian.science.dto.CreateScienceSubmissionRequest;
import cn.maian.science.dto.ScienceSubmissionCountResponse;
import cn.maian.science.dto.ScienceSubmissionResponse;
import cn.maian.science.repository.ScienceSubmissionRepository;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.user.service.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ScienceSubmissionService {

    private final ScienceSubmissionRepository scienceSubmissionRepository;

    public ScienceSubmissionService(ScienceSubmissionRepository scienceSubmissionRepository) {
        this.scienceSubmissionRepository = scienceSubmissionRepository;
    }

    @Transactional
    public ScienceSubmissionResponse create(CreateScienceSubmissionRequest request) {
        var submission = ScienceSubmission.create(
            UserProfileService.CURRENT_USER_ID,
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
            scienceSubmissionRepository.countByUserId(UserProfileService.CURRENT_USER_ID)
        );
    }

    @Transactional(readOnly = true)
    public Page<ScienceSubmissionResponse> findAll(Pageable pageable) {
        return scienceSubmissionRepository
            .findAllByUserId(UserProfileService.CURRENT_USER_ID, pageable)
            .map(ScienceSubmissionResponse::from);
    }

    @Transactional(readOnly = true)
    public ScienceSubmissionResponse findById(UUID id) {
        return ScienceSubmissionResponse.from(findOwned(id));
    }

    @Transactional
    public void delete(UUID id) {
        scienceSubmissionRepository.delete(findOwned(id));
    }

    private ScienceSubmission findOwned(UUID id) {
        var submission = scienceSubmissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("投稿不存在"));
        if (!UserProfileService.CURRENT_USER_ID.equals(submission.getUserId())) {
            throw new ResourceNotFoundException("投稿不存在");
        }
        return submission;
    }
}
