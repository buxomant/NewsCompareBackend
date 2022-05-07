package com.cbp.app.repository;

import com.cbp.app.model.db.WebsiteToWebsite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebsiteToWebsiteRepository extends JpaRepository<WebsiteToWebsite, Integer> {
    void deleteAllByWebsiteIdFrom(int websiteIdFrom);

    List<WebsiteToWebsite> findAllByWebsiteIdTo(int websiteIdTo);

    @Query(value = "SELECT DISTINCT * FROM website_to_website wtw " +
        " WHERE website_id_from IN (SELECT website_id FROM website WHERE type = :websiteType AND content_type = :websiteContentType)" +
        " AND content_id IN (SELECT MAX(content_id) FROM website_to_website WHERE content_id != 0 GROUP BY website_id_from)", nativeQuery = true)
    List<WebsiteToWebsite> findAllByWebsiteTypeAndContentTypeForLatestContentId(
        @Param("websiteType") String websiteType,
        @Param("websiteContentType") String websiteContentType
    );

    @Query(value =
        "SELECT " +
        "  MAX(wtw.link_id) AS \"link_id\", " +
        "  COALESCE(MAX(soFrom.website_id_parent), MAX(wtw.website_id_from)) AS \"website_id_from\", " +
        "  COALESCE(MAX(soTo.website_id_parent), MAX(wtw.website_id_to)) AS \"website_id_to\", " +
        "  MAX(wtw.content_id) AS \"content_id\", " +
        "  MAX(wtw.title) AS \"title\" " +
        "FROM website_to_website wtw " +
        "  JOIN website w ON wtw.website_id_from = w.website_id " +
        "  LEFT JOIN subdomain_of soFrom ON soFrom.website_id_child = wtw.website_id_from " +
        "  LEFT JOIN subdomain_of soTo ON soTo.website_id_child = wtw.website_id_to " +
        "WHERE w.type = :websiteType " +
        "  AND wtw.website_id_from != wtw.website_id_to" +
        "  AND w.content_type = :websiteContentType " +
        "  AND content_id IN (SELECT MAX(content_id) FROM website_to_website WHERE content_id != 0 GROUP BY website_id_from) " +
        "GROUP BY CONCAT(wtw.website_id_from, '-', wtw.website_id_to)" +
        "UNION " +
        "SELECT " +
        "  MAX(wtw.link_id) AS \"link_id\", " +
        "  COALESCE(MAX(soFrom.website_id_parent), MAX(wtw.website_id_from)) AS \"website_id_from\", " +
        "  COALESCE(MAX(soTo.website_id_parent), MAX(wtw.website_id_to)) AS \"website_id_to\", " +
        "  MAX(wtw.content_id) AS \"content_id\", " +
        "  MAX(wtw.title) AS \"title\" " +
        "FROM website_to_website wtw " +
        "  JOIN website w ON wtw.website_id_to = w.website_id " +
        "  LEFT JOIN subdomain_of soFrom ON soFrom.website_id_child = wtw.website_id_from " +
        "  LEFT JOIN subdomain_of soTo ON soTo.website_id_child = wtw.website_id_to " +
        "WHERE w.type = :websiteType " +
        "  AND wtw.website_id_from != wtw.website_id_to" +
        "  AND w.content_type = :websiteContentType " +
        "  AND content_id IN (SELECT MAX(content_id) FROM website_to_website WHERE content_id != 0 GROUP BY website_id_to) " +
        "GROUP BY CONCAT(wtw.website_id_from, '-', wtw.website_id_to)", nativeQuery = true)
    List<WebsiteToWebsite> findAllByWebsiteTypeAndContentTypeForLatestContentIdCoalesced(
        @Param("websiteType") String websiteType,
        @Param("websiteContentType") String websiteContentType
    );

    @Query(value =
        "SELECT " +
        "  MAX(wtw.link_id) AS \"link_id\", " +
        "  COALESCE(MAX(soFrom.website_id_parent), MAX(wtw.website_id_from)) AS \"website_id_from\", " +
        "  COALESCE(MAX(soTo.website_id_parent), MAX(wtw.website_id_to)) AS \"website_id_to\", " +
        "  MAX(wtw.content_id) AS \"content_id\", " +
        "  MAX(wtw.title) AS \"title\" " +
        "FROM website_to_website wtw " +
        "  JOIN website w ON wtw.website_id_from = w.website_id " +
        "  LEFT JOIN subdomain_of soFrom ON soFrom.website_id_child = wtw.website_id_from " +
        "  LEFT JOIN subdomain_of soTo ON soTo.website_id_child = wtw.website_id_to " +
        "WHERE wtw.website_id_from != wtw.website_id_to" +
        "  AND content_id IN (SELECT MAX(content_id) FROM website_to_website WHERE content_id != 0 GROUP BY website_id_from) " +
        "GROUP BY CONCAT(wtw.website_id_from, '-', wtw.website_id_to)" +
        "UNION " +
        "SELECT " +
        "  MAX(wtw.link_id) AS \"link_id\", " +
        "  COALESCE(MAX(soFrom.website_id_parent), MAX(wtw.website_id_from)) AS \"website_id_from\", " +
        "  COALESCE(MAX(soTo.website_id_parent), MAX(wtw.website_id_to)) AS \"website_id_to\", " +
        "  MAX(wtw.content_id) AS \"content_id\", " +
        "  MAX(wtw.title) AS \"title\" " +
        "FROM website_to_website wtw " +
        "  JOIN website w ON wtw.website_id_to = w.website_id " +
        "  LEFT JOIN subdomain_of soFrom ON soFrom.website_id_child = wtw.website_id_from " +
        "  LEFT JOIN subdomain_of soTo ON soTo.website_id_child = wtw.website_id_to " +
        "WHERE wtw.website_id_from != wtw.website_id_to" +
        "  AND content_id IN (SELECT MAX(content_id) FROM website_to_website WHERE content_id != 0 GROUP BY website_id_to) " +
        "GROUP BY CONCAT(wtw.website_id_from, '-', wtw.website_id_to)", nativeQuery = true)
    List<WebsiteToWebsite> findAllForLatestContentIdCoalesced();

    @Query(value = "SELECT COUNT(*) FROM website_to_website", nativeQuery = true)
    Integer getNumberOfWebsiteToWebsiteLinks();
}
