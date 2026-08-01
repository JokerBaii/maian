package cn.maian.science.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.science.dto.ScienceArticleInteractionResponse;
import cn.maian.science.dto.UpdateScienceArticleInteractionRequest;
import cn.maian.science.service.ScienceArticleInteractionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/science-articles/{articleId}/interaction")
public class ScienceArticleInteractionController {

    private final ScienceArticleInteractionService interactionService;

    public ScienceArticleInteractionController(ScienceArticleInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping
    public ApiResponse<ScienceArticleInteractionResponse> get(@PathVariable String articleId) {
        return ApiResponse.ok(interactionService.get(articleId));
    }

    @PutMapping
    public ApiResponse<ScienceArticleInteractionResponse> update(
        @PathVariable String articleId,
        @RequestBody UpdateScienceArticleInteractionRequest request
    ) {
        return ApiResponse.ok(interactionService.update(articleId, request));
    }
}
