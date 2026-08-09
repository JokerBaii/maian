package cn.maian.science.service;

import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.science.domain.ScienceContentKind;
import cn.maian.science.dto.ScienceContentResponse;
import cn.maian.science.repository.ScienceContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScienceContentService {

    private final ScienceContentRepository repository;

    public ScienceContentService(ScienceContentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ScienceContentResponse> findAll(ScienceContentKind kind) {
        return repository.findAllByActiveTrueAndKindOrderBySortOrderAscPublishedAtDesc(kind)
            .stream().map(ScienceContentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ScienceContentResponse findById(String id) {
        var content = repository.findById(id)
            .filter(item -> item.isActive())
            .orElseThrow(() -> new ResourceNotFoundException("科普内容不存在"));
        return ScienceContentResponse.from(content);
    }
}
