package com.jobspring.jobspringbackend.repository.spec;


import com.jobspring.jobspringbackend.entity.User;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

public final class UserSpecs {

    private UserSpecs() {
    }

    public static Specification<User> fuzzySearch(String raw) {
        return (root, query, cb) -> {
            String escaped = escapeLike(raw.toLowerCase());
            String pattern = "%" + escaped + "%";

            // email 和 fullName 做不区分大小写的 like
            var emailLike = cb.like(cb.lower(root.get("email")), pattern, '\\');
            var nameLike = cb.like(cb.lower(root.get("fullName")), pattern, '\\');

            // id 支持两种：1) 可解析为 Long 时，等值匹配  2) 同时提供“按字符串模糊”能力
            Predicate idPredicate;
            try {
                long idVal = Long.parseLong(raw);
                var idEq = cb.equal(root.get("id"), idVal);
                // 也支持把 id 转成字符串模糊（跨库通用：as(String.class)）
                var idLike = cb.like(root.get("id").as(String.class), pattern, '\\');
                idPredicate = cb.or(idEq, idLike);
            } catch (NumberFormatException e) {
                // 非数字：只做字符串模糊
                idPredicate = cb.like(root.get("id").as(String.class), pattern, '\\');
            }

            return cb.or(emailLike, nameLike, idPredicate);
        };
    }

    /**
     * 简单转义 LIKE 的通配符，避免用户输入中含 % 或 _
     */
    private static String escapeLike(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("%", "\\%");
    }
}
