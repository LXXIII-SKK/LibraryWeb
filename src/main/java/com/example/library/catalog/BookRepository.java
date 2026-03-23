package com.example.library.catalog;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
            select distinct b
            from Book b
            left join b.tags tag
            where (:query = '' or lower(b.title) like concat('%', lower(:query), '%')
                or lower(b.author) like concat('%', lower(:query), '%')
                or lower(coalesce(b.category, '')) like concat('%', lower(:query), '%')
                or lower(coalesce(tag.name, '')) like concat('%', lower(:query), '%'))
              and (:category = '' or lower(coalesce(b.category, '')) = lower(:category))
              and (:tag = '' or lower(coalesce(tag.name, '')) = lower(:tag))
            order by b.title asc
            """)
    List<Book> search(@Param("query") String query, @Param("category") String category, @Param("tag") String tag);

    @Query("""
            select distinct b.category
            from Book b
            where b.category is not null and trim(b.category) <> ''
            order by b.category asc
            """)
    List<String> findDistinctCategories();

    @Query("""
            select distinct tag.name
            from BookTag tag
            order by tag.name asc
            """)
    List<String> findDistinctTags();

    @Query(
            value = """
                    select b.*
                    from book b
                    left join (
                        select bt.book_id, count(distinct bt.user_id) as borrow_count
                        from borrow_transaction bt
                        where bt.borrowed_at > :threshold
                        group by bt.book_id
                    ) weekly_borrow on weekly_borrow.book_id = b.id
                    left join (
                        select al.book_id, count(*) as view_count
                        from activity_log al
                        where al.activity_type = 'VIEWED'
                          and al.book_id is not null
                          and al.occurred_at > :threshold
                        group by al.book_id
                    ) weekly_view on weekly_view.book_id = b.id
                    order by coalesce(weekly_borrow.borrow_count, 0) * 5
                           + coalesce(weekly_view.view_count, 0) * 3
                           + b.available_quantity * 2 desc,
                             b.title asc
                    limit 4
                    """,
            nativeQuery = true)
    List<Book> findTopRecommendedBooks(@Param("threshold") Instant threshold);
}
