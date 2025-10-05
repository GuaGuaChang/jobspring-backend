package com.jobspring.jobspringbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// HR 修改申请状态 请求体
@Data
public class UpdateApplicationStatusRequest {

    // 0 = 已投递，1 = 筛选中，2 = 通过，3 = 拒绝， 4 = 失效
    @NotNull
    private Integer status;

    // 可选：备注/原因（如拒绝原因）
    private String note;
}