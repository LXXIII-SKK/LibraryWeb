package com.example.library.catalog;

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
}
