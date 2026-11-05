package com.yourapp.catalog.software.search;

import com.yourapp.catalog.software.event.SoftwareIndexEvent;
import com.yourapp.catalog.software.event.SoftwareIndexEvent.Action;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class SearchIndexNotifierTest {

    @Test
    void postsSoftwareChangesToTheSearchService() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://search");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SearchIndexNotifier notifier = new SearchIndexNotifier(builder.build());

        server.expect(requestTo("http://search/internal/index"))
                .andExpect(method(POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"softwareId\":\"sw-1\",\"action\":\"UPSERT\"}"))
                .andRespond(withSuccess());

        notifier.onSoftwareIndex(new SoftwareIndexEvent("sw-1", Action.UPSERT));

        server.verify();
    }

    @Test
    void keepsTheCatalogWriteSuccessfulWhenSearchIsDown() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://search");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SearchIndexNotifier notifier = new SearchIndexNotifier(builder.build());

        server.expect(requestTo("http://search/internal/index"))
                .andRespond(withServerError());

        assertThatCode(() -> notifier.onSoftwareIndex(new SoftwareIndexEvent("sw-1", Action.DELETE)))
                .doesNotThrowAnyException();
    }
}
