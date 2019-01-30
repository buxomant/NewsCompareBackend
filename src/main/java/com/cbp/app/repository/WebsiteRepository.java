package com.cbp.app.repository;

import com.cbp.app.model.db.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteRepository extends JpaRepository<Website, Integer> {
    Optional<Website> findByUrl(String url);

    List<Website> findAllByUrlOrderByWebsiteId(String url);

    List<Website> findAllByUrlIn(List<String> urls);

    @Query(value = "SELECT * FROM website" +
        " WHERE type = 'DOMESTIC'" +
        "   AND last_checked_on + INTERVAL '1' HOUR * fetch_every_number_of_hours < now()" +
        " ORDER BY last_checked_on ASC LIMIT 1", nativeQuery = true)
    Optional<Website> getNextDomesticWebsiteThatNeedsFetching();

    @Query(value = "SELECT * FROM website JOIN website_content USING (website_id)" +
        " WHERE time_processed IS NULL" +
        "   AND type = 'DOMESTIC'" +
        " ORDER BY time_fetched ASC LIMIT 1", nativeQuery = true)
    Optional<Website> getNextDomesticWebsiteThatNeedsProcessing();

    @Query(value = "SELECT url FROM website" +
        " GROUP BY url" +
        " HAVING COUNT(url) > 1" +
        " ORDER BY COUNT(url) DESC LIMIT 1", nativeQuery = true)
    Optional<String> getNextDuplicateWebsiteUrl();

    @Query(value = "SELECT * FROM website w " +
        "WHERE error IS NULL " +
        "  AND url NOT LIKE '%.%.%' " +
        "  AND EXISTS(" +
        "    SELECT * FROM website" +
        "    WHERE url LIKE CONCAT('%.', w.url, '%')" +
        "  ) AND NOT EXISTS(" +
        "    SELECT * FROM subdomain_of" +
        "    WHERE website_id_parent = w.website_id OR website_id_child = w.website_id" +
        "  ) LIMIT 1", nativeQuery = true)
    Optional<Website> getNextWebsiteNotMarkedAsDomainOrSubdomain();

    @Query(value = "SELECT * FROM website " +
            "WHERE url LIKE CONCAT('%.', :url, '%')", nativeQuery = true)
    List<Website> getSubdomainsForUrl(@Param("url") String url);

    @Query(value = "SELECT COUNT(website_id) FROM website w" +
        " WHERE w.website_id IN" +
        "   (SELECT MAX(website_id) FROM website GROUP BY url HAVING COUNT(website_id) > 1)", nativeQuery = true)
    Integer getNumberOfDuplicateWebsites();

    @Query(value = "SELECT COUNT(*) FROM website", nativeQuery = true)
    Integer getNumberOfWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE last_checked_on IS NOT NULL", nativeQuery = true)
    Integer getNumberOfCheckedWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE last_processed_on IS NOT NULL", nativeQuery = true)
    Integer getNumberOfProcessedWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE error IS NOT NULL", nativeQuery = true)
    Integer getNumberOfWebsitesWithErrors();

    @Query(value = "SELECT COUNT(*) FROM website WHERE type = 'DOMESTIC'", nativeQuery = true)
    Integer getNumberOfDomesticWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE type = 'FOREIGN'", nativeQuery = true)
    Integer getNumberOfForeignWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE type = 'REDIRECT_TO_FOREIGN'", nativeQuery = true)
    Integer getNumberOfRedirectToForeignWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE type = 'INDEXING_SERVICE'", nativeQuery = true)
    Integer getNumberOfIndexingServiceWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE content_type = 'NEWS'", nativeQuery = true)
    Integer getNumberOfNewsWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE content_type = 'SOCIAL_MEDIA'", nativeQuery = true)
    Integer getNumberOfSocialMediaWebsites();

    @Query(value = "SELECT COUNT(*) FROM website WHERE content_type = 'UNCATEGORIZED'", nativeQuery = true)
    Integer getNumberOfUncategorizedWebsites();

    @Query(value = "SELECT COUNT(*) FROM website " +
        "WHERE url NOT LIKE '%.%.%' " +
        "AND website_id NOT IN (SELECT DISTINCT website_id_child FROM subdomain_of)", nativeQuery = true)
    Integer getNumberOfTopDomains();

    @Query(value = "SELECT COUNT(*) FROM subdomain_of", nativeQuery = true)
    Integer getNumberOfSubDomains();
}
