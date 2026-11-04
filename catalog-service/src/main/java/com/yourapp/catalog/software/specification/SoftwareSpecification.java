package com.yourapp.catalog.software.specification;

import com.yourapp.catalog.software.entity.SoftwareEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class SoftwareSpecification {

    private SoftwareSpecification() {}

    public static Specification<SoftwareEntity> nameLike(String name) {
        return (root, query, cb) ->
                StringUtils.hasText(name)
                        ? cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%")
                        : cb.conjunction();
    }

    public static Specification<SoftwareEntity> hasCategory(String category) {
        return (root, query, cb) ->
                StringUtils.hasText(category)
                        ? cb.equal(root.get("category"), category)
                        : cb.conjunction();
    }

    public static Specification<SoftwareEntity> hasOs(String os) {
        return (root, query, cb) -> cb.conjunction();
    }
}
