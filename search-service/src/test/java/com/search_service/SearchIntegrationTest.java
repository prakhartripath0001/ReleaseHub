package com.search_service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.search_service.catalog.entity.SoftwareEntity;
import com.search_service.catalog.entity.VersionEntity;
import com.search_service.catalog.repository.SoftwareReadRepository;
import com.search_service.catalog.repository.VersionReadRepository;
import com.search_service.index.SoftwareDocument;
import com.search_service.index.SoftwareIndexManager;
import com.search_service.indexer.IndexAction;
import com.search_service.indexer.ReindexJob;
import com.search_service.indexer.SearchIndexer;
import com.search_service.indexer.SoftwareIndexEvent;
import com.search_service.search.FacetBucket;
import com.search_service.search.SearchHit;
import com.search_service.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SearchIntegrationTest {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.5"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false");

    @Container
    static final org.testcontainers.containers.MySQLContainer<?> MYSQL =
            new org.testcontainers.containers.MySQLContainer<>("mysql:8.0");

    static {
        ELASTICSEARCH.start();
        MYSQL.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("releasehub.elasticsearch.uri", () -> "http://" + ELASTICSEARCH.getHttpHostAddress());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SearchIndexer searchIndexer;

    @Autowired
    private ReindexJob reindexJob;

    @Autowired
    private SoftwareIndexManager indexManager;

    @Autowired
    private SoftwareReadRepository softwareRepository;

    @Autowired
    private VersionReadRepository versionRepository;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ElasticsearchClient client;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @BeforeEach
    void clean() throws Exception {
        client.deleteByQuery(request -> request
                .index(indexManager.alias())
                .query(query -> query.matchAll(all -> all))
                .refresh(true));
        versionRepository.deleteAll();
        softwareRepository.deleteAll();
    }

    @Test
    void fuzzySearchReturnsTheIntendedDocumentAndNameOutranksDescription() throws Exception {
        searchIndexer.index(document("firefox", "Firefox", "Web browser", "browser", List.of("web"), List.of("LINUX")));
        searchIndexer.index(document("chrome", "Chrome", "Another browser", "browser", List.of("web"), List.of("WINDOWS")));
        searchIndexer.index(document("notes", "Field Notes", "Mentions Firefox in the description only", "tools", List.of(), List.of("LINUX")));

        SearchResult typo = search("q", "Firefx");

        assertThat(typo.hits()).isNotEmpty();
        assertThat(typo.hits().getFirst().name()).isEqualTo("Firefox");
        assertThat(typo.hits().getFirst().score()).isPositive();

        SearchResult ranked = search("q", "Firefox");
        assertThat(ranked.hits().getFirst().id()).isEqualTo("firefox");
        assertThat(ranked.hits().getFirst().score()).isGreaterThan(scoreOf(ranked, "notes"));
    }

    @Test
    void facetCountsMatchTheIndexedDocuments() throws Exception {
        searchIndexer.index(document("firefox", "Firefox", "Browser", "browser", List.of("web"), List.of("LINUX", "WINDOWS")));
        searchIndexer.index(document("chrome", "Chrome", "Browser", "browser", List.of("web"), List.of("LINUX")));
        searchIndexer.index(document("vim", "Vim", "Editor", "tools", List.of("editor"), List.of("LINUX")));

        SearchResult result = search(null, null);

        assertThat(result.total()).isEqualTo(3);
        assertThat(count(result, "os", "LINUX")).isEqualTo(3);
        assertThat(count(result, "os", "WINDOWS")).isEqualTo(1);
        assertThat(count(result, "category", "browser")).isEqualTo(2);
        assertThat(count(result, "category", "tools")).isEqualTo(1);
    }

    @Test
    void filtersByOsAndCategory() throws Exception {
        searchIndexer.index(document("firefox", "Firefox", "Browser", "browser", List.of(), List.of("LINUX")));
        searchIndexer.index(document("vim", "Vim", "Editor", "tools", List.of(), List.of("LINUX")));
        searchIndexer.index(document("chrome", "Chrome", "Browser", "browser", List.of(), List.of("WINDOWS")));

        MvcResult result = mockMvc.perform(get("/search")
                        .param("os", "LINUX")
                        .param("category", "browser"))
                .andExpect(status().isOk())
                .andReturn();
        SearchResult body = read(result);

        assertThat(body.hits()).extracting(SearchHit::name).containsExactly("Firefox");
        assertThat(body.total()).isEqualTo(1);
    }

    @Test
    void createAndUpdateEventsRefreshTheIndex() throws Exception {
        SoftwareEntity software = saveSoftware("Alpha", "editor", "tools");
        saveVersion(software.getId(), "LINUX");

        events.publishEvent(new SoftwareIndexEvent(software.getId(), IndexAction.UPSERT));

        SearchResult created = search("q", "Alpha");
        assertThat(created.hits()).extracting(SearchHit::name).containsExactly("Alpha");
        assertThat(created.hits().getFirst().os()).containsExactly("LINUX");

        software.setName("AlphaPrime");
        softwareRepository.save(software);
        events.publishEvent(new SoftwareIndexEvent(software.getId(), IndexAction.UPSERT));

        SearchResult updated = search("q", "AlphaPrime");
        assertThat(updated.hits()).extracting(SearchHit::id).containsExactly(software.getId());

        events.publishEvent(new SoftwareIndexEvent(software.getId(), IndexAction.DELETE));
        SearchResult deleted = search("q", "AlphaPrime");
        assertThat(deleted.hits()).isEmpty();
    }

    @Test
    void internalIndexEndpointUpsertsFromMysql() throws Exception {
        SoftwareEntity software = saveSoftware("Gimp", "Image editor", "graphics");
        saveVersion(software.getId(), "MACOS");

        mockMvc.perform(post("/internal/index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"softwareId":"%s","action":"UPSERT"}
                                """.formatted(software.getId())))
                .andExpect(status().isNoContent());

        SearchResult result = search("q", "Gimp");
        assertThat(result.hits()).extracting(SearchHit::name).containsExactly("Gimp");
        assertThat(result.hits().getFirst().os()).containsExactly("MACOS");
    }

    @Test
    void reindexSwapsTheAliasWithoutDroppingTheLiveIndexFirst() throws Exception {
        SoftwareEntity software = saveSoftware("Notepad", "Plain text", "tools");
        saveVersion(software.getId(), "WINDOWS");
        reindexJob.reindex();

        software.setName("NotepadPlus");
        softwareRepository.save(software);

        SearchResult before = search("q", "Notepad");
        assertThat(before.hits()).extracting(SearchHit::name).containsExactly("Notepad");

        String liveIndex = indexManager.currentIndex();
        reindexJob.reindex((current, staging) -> {
            assertThat(current).isEqualTo(liveIndex);
            assertThat(staging).isNotEqualTo(liveIndex);
            assertThat(indexManager.currentIndex()).isEqualTo(liveIndex);
            SearchResult during = searchUnchecked("q", "Notepad");
            assertThat(during.hits()).extracting(SearchHit::name).containsExactly("Notepad");
        });

        assertThat(indexManager.currentIndex()).isNotEqualTo(liveIndex);
        assertThat(indexManager.indexExists(liveIndex)).isFalse();

        SearchResult after = search("q", "NotepadPlus");
        assertThat(after.hits()).extracting(SearchHit::name).containsExactly("NotepadPlus");
        assertThat(after.hits().getFirst().os()).containsExactly("WINDOWS");
    }

    private SoftwareDocument document(String id, String name, String description, String category,
                                      List<String> tags, List<String> os) {
        return SoftwareDocument.builder()
                .id(id)
                .name(name)
                .description(description)
                .category(category)
                .tags(tags)
                .os(os)
                .build();
    }

    private SoftwareEntity saveSoftware(String name, String description, String category) {
        SoftwareEntity entity = new SoftwareEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(name);
        entity.setDescription(description);
        entity.setCategory(category);
        entity.setTags(List.of("desktop"));
        entity.setPublisherId(UUID.randomUUID().toString());
        entity.setCreatedAt(LocalDateTime.now());
        return softwareRepository.save(entity);
    }

    private void saveVersion(String softwareId, String os) {
        VersionEntity version = new VersionEntity();
        version.setId(UUID.randomUUID().toString());
        version.setSoftwareId(softwareId);
        version.setVersionNumber("1.0.0");
        version.setOs(os);
        version.setReleasedAt(LocalDateTime.now());
        versionRepository.save(version);
    }

    private SearchResult search(String param, String value) throws Exception {
        var request = get("/search");
        if (param != null) {
            request.param(param, value);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return read(result);
    }

    private SearchResult searchUnchecked(String param, String value) {
        try {
            return search(param, value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private SearchResult read(MvcResult result) throws Exception {
        return jsonMapper.readValue(result.getResponse().getContentAsString(), SearchResult.class);
    }

    private long count(SearchResult result, String facet, String key) {
        return result.facets().get(facet).stream()
                .filter(bucket -> bucket.key().equals(key))
                .mapToLong(FacetBucket::count)
                .findFirst()
                .orElseThrow();
    }

    private double scoreOf(SearchResult result, String id) {
        return result.hits().stream()
                .filter(hit -> hit.id().equals(id))
                .map(SearchHit::score)
                .findFirst()
                .orElseThrow();
    }
}
