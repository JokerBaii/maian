package cn.maian.science.service;

import cn.maian.science.domain.ScienceSubmission;
import cn.maian.science.dto.CreateScienceSubmissionRequest;
import cn.maian.science.dto.ScienceSubmissionCountResponse;
import cn.maian.science.dto.ScienceSubmissionResponse;
import cn.maian.science.repository.ScienceSubmissionRepository;
import cn.maian.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
