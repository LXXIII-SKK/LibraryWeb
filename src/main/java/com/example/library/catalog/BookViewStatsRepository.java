package com.example.library.catalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class BookViewStatsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    BookViewStatsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Map<Long, Long> findViewCountsByBookIds(Collection<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> viewCounts = new HashMap<>();
        jdbcTemplate.query(
                        """
                                select book_id, count(*) as view_count
                                from activity_log
                                where activity_type = 'VIEWED'
                                  and book_id in (:bookIds)
                                group by book_id
                                """,
                        Map.of("bookIds", bookIds),
                        (rs, rowNum) -> Map.entry(rs.getLong("book_id"), rs.getLong("view_count")))
                .forEach(entry -> viewCounts.put(entry.getKey(), entry.getValue()));
        return viewCounts;
    }

    long findViewCountByBookId(Long bookId) {
        Long viewCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from activity_log
                        where activity_type = 'VIEWED'
                          and book_id = :bookId
                        """,
                Map.of("bookId", bookId),
                Long.class);
        return viewCount == null ? 0L : viewCount;
    }
}
