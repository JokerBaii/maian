package cn.maian.science.controller;

import cn.maian.common.api.ApiResponse;
import cn.maian.science.domain.ScienceContentKind;
import cn.maian.science.dto.ScienceContentResponse;
import cn.maian.science.service.ScienceContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/science-content")
public class ScienceContentController {

    private final ScienceContentService service;

    public ScienceContentController(ScienceContentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ScienceContentResponse>> findAll(
        @RequestParam(defaultValue = "ARTICLE") ScienceContentKind kind
    ) {
        return ApiResponse.ok(service.findAll(kind));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScienceContentResponse> findById(@PathVariable String id) {
        return ApiResponse.ok(service.findById(id));
    }
}
