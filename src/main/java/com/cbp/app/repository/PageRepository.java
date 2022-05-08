package com.cbp.app.repository;

import com.cbp.app.model.db.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    List<Page> findAllByUrlIn(List<String> urls);

    List<Page> findAllByUrlOrderByPageId(String url);

    List<Page> findAllByWebsiteId(Integer websiteId);

    void deleteAllByWebsiteId(Integer websiteId);

    @Query(value = "SELECT COUNT(*) FROM page", nativeQuery = true)
    Integer getNumberOfPages();

    @Query(value = "SELECT * FROM page" +
    "  WHERE last_seen > now() - INTERVAL '1' HOUR" +
    "  AND split_part(url, '/', 1) IN " +
    "    (SELECT url FROM website " +
    "       WHERE content_type != 'SOCIAL_MEDIA'" +
    "           AND type NOT IN ('REDIRECT', 'INDEXING_SERVICE'))" +
    "  LIMIT 1000", nativeQuery = true)
    List<Page> getNextPagesThatNeedFetching();
}
