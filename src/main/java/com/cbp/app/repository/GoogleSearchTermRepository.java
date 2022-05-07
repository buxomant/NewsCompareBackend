package com.cbp.app.repository;

import com.cbp.app.model.db.GoogleSearchTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleSearchTermRepository extends JpaRepository<GoogleSearchTerm, Integer> {
    @Query(value = "SELECT * FROM google_search_term gst" +
            " WHERE (SELECT COUNT(*) FROM google_search gs WHERE gs.term_id = gst.term_id) = 0" +
            " ORDER BY term_id ASC LIMIT 1", nativeQuery = true)
    public Optional<GoogleSearchTerm> getNextUnusedSearchTerm();
}
