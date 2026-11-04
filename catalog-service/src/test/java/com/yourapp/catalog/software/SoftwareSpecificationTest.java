package com.yourapp.catalog.software;

import com.yourapp.catalog.software.specification.SoftwareSpecification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoftwareSpecificationTest {

    @Test
    void testSpecificationsNotNull() {
        assertThat(SoftwareSpecification.nameLike("test")).isNotNull();
        assertThat(SoftwareSpecification.nameLike(null)).isNotNull();
        assertThat(SoftwareSpecification.hasCategory("tools")).isNotNull();
        assertThat(SoftwareSpecification.hasCategory(null)).isNotNull();
        assertThat(SoftwareSpecification.hasOs("linux")).isNotNull();
    }
}

