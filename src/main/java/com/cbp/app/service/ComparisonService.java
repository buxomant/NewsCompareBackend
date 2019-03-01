package com.cbp.app.service;

import com.cbp.app.model.db.TextSimilarity;
import com.cbp.app.repository.TextSimilarityRepository;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


import static com.cbp.app.service.IndexService.DATE_AND_HOUR_PATTERN;
import static com.cbp.app.service.IndexService.WEBSITE_STORAGE_PATH;

@Service
public class ComparisonService {
    private final TextSimilarityRepository textSimilarityRepository;

    private static int MAX_HITS = 1000;

    @Autowired
    public ComparisonService(TextSimilarityRepository textSimilarityRepository) {
        this.textSimilarityRepository = textSimilarityRepository;
    }

    public void compareDocuments() throws IOException {
//        String dateAndHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_AND_HOUR_PATTERN));
        String dateAndHour = "2019-02-27_07";
        Path workingDirectoryPath = Paths.get(WEBSITE_STORAGE_PATH + "/" + dateAndHour);

        Directory workingDirectory = FSDirectory.open(workingDirectoryPath);
        IndexReader indexReader = DirectoryReader.open(workingDirectory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        List<TextSimilarity> allSimilarities = new ArrayList<>();

        AtomicInteger docId = new AtomicInteger(0);
        for (int currentDocumentId = 0; currentDocumentId < indexReader.maxDoc(); currentDocumentId++) {
            int tempDocId = docId.getAndIncrement(); // qq refactor
            Document currentDocument = indexReader.document(currentDocumentId);
            int currentId = Integer.parseInt(currentDocument.getField("id").stringValue());
            String currentType = currentDocument.getField("type").stringValue();
            String currentUrl = currentDocument.getField("url").stringValue();
            String currentTopDomain = urlToTopDomain(currentUrl);

            Analyzer analyzer = new RomanianAnalyzer();
            ShingleAnalyzerWrapper shingleAnalyzerWrapper = new ShingleAnalyzerWrapper(analyzer, 3, 3, " ", false, false, "_");
            MoreLikeThis moreLikeThis = new MoreLikeThis(indexReader);
            moreLikeThis.setFieldNames(new String[] { "text" });
            moreLikeThis.setAnalyzer(shingleAnalyzerWrapper);
            moreLikeThis.setMaxQueryTerms(1000);

            Query query = moreLikeThis.like(currentDocumentId);

            TopDocs result = indexSearcher.search(query, MAX_HITS);
            List<TextSimilarity> similarities = Arrays.stream(result.scoreDocs)
                .filter(scoreDoc -> scoreDoc.doc > tempDocId)
                .filter(scoreDoc -> scoreDoc.score / result.getMaxScore() > 0.5F)
                .map(scoreDoc -> {
                    try {
                        Document otherDocument = indexReader.document(scoreDoc.doc);
                        int otherId = Integer.parseInt(otherDocument.getField("id").stringValue());
                        String otherUrl = otherDocument.getField("url").stringValue();
                        String otherType = otherDocument.getField("type").stringValue();
                        String otherTopDomain = urlToTopDomain(otherUrl);

                        if (currentTopDomain.equals(otherTopDomain)) {
                            return null;
                        }

                        Float coefficient = scoreDoc.score / result.getMaxScore();
                        return new TextSimilarity(currentId, otherId, dateAndHour, currentType, otherType, coefficient);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            allSimilarities.addAll(similarities);
        }

        textSimilarityRepository.deleteAll();
        textSimilarityRepository.saveAll(allSimilarities);
    }

    private static String urlToTopDomain(String url) {
        String baseUrl = url.split("\\[]")[0];
        String[] baseUrlSplit = baseUrl.split("\\.");
        return baseUrlSplit[baseUrlSplit.length - 2] + "." + baseUrlSplit[baseUrlSplit.length - 1];
    }
}
