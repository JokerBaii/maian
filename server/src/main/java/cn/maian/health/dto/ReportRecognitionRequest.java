package cn.maian.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 体检报告原图识别请求。传入已上传的图片地址。 */
public record ReportRecognitionRequest(
    @NotBlank @Size(max = 500) String sourceImageUrl
) {
}
