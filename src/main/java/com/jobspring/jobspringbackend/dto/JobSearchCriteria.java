package com.jobspring.jobspringbackend.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JobSearchCriteria(
        String title,                 // 标题模糊
        Integer status,               // 0=有效 1=无效
        Long companyId,               // 公司
        String location,              // 地点模糊
        Integer employmentType,       // 1/2/3...
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime postedFrom,     // 发布时间 >=
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime postedTo,       // 发布时间 <=
        BigDecimal salaryMin,         // 期望最低薪（与职位区间重叠）
        BigDecimal salaryMax,         // 期望最高薪（与职位区间重叠）
        String keyword                // 描述或标题关键字模糊
) {
}