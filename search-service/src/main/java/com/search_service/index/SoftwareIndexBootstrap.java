package com.search_service.index;

import com.search_service.support.ElasticsearchCalls;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SoftwareIndexBootstrap implements ApplicationRunner {

    private final SoftwareIndexManager indexManager;

    public SoftwareIndexBootstrap(SoftwareIndexManager indexManager) {
        this.indexManager = indexManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        indexManager.ensureAlias();
    }
}
