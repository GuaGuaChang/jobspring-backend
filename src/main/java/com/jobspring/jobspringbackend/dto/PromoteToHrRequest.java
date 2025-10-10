package com.jobspring.jobspringbackend.dto;

import lombok.Data;

// Admin 将用户设为 HR 时的可选参数
@Data
public class PromoteToHrRequest {
    // 要绑定的公司ID，可选
    private Long companyId;

    // 若用户已有关联公司，是否允许覆盖（默认 true）
    private Boolean overwriteCompany = true;
}