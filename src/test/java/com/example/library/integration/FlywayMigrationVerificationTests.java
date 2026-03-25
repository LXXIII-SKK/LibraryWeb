package com.example.library.integration;

import java.io.IOException;
import java.util.Arrays;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationVerificationTests extends PostgresBackedIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void flywayHasNoPendingOrFailedMigrationsAndMatchesLatestScript() throws Exception {
        MigrationInfo[] pending = flyway.info().pending();
        MigrationInfo[] failed = Arrays.stream(flyway.info().all())
                .filter(info -> info.getState().isFailed())
                .toArray(MigrationInfo[]::new);

        assertThat(failed).isEmpty();
        assertThat(pending).isEmpty();
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo(latestResolvedVersion());
    }

    private String latestResolvedVersion() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/V*__*.sql");
        return Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(name -> name != null && name.startsWith("V"))
                .map(name -> {
                    int separatorIndex = name.indexOf("__");
                    return separatorIndex > 1 ? name.substring(1, separatorIndex) : "0";
                })
                .mapToInt(Integer::parseInt)
                .max()
                .stream()
                .mapToObj(String::valueOf)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No versioned Flyway migration scripts were found"));
    }
}
