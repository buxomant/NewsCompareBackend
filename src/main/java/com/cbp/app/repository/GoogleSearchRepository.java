package com.cbp.app.repository;

import com.cbp.app.model.db.GoogleSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleSearchRepository extends JpaRepository<GoogleSearch, Integer> {
    @Query(value = "SELECT term_id FROM google_search" +
        "  GROUP BY term_id" +
        "  HAVING MAX(next_start_index) < 101 LIMIT 1", nativeQuery = true)
    Optional<Integer> getNextSearchTermId();

    @Query(value = "SELECT COALESCE(MAX(next_start_index), 1) FROM google_search" +
        " WHERE term_id = :termId", nativeQuery = true)
    Optional<Integer> getNextSearchStartIndexByTermId(@Param("termId") Integer termId);
}
