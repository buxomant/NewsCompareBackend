package com.cbp.app.service;

import com.cbp.app.helper.LoggingHelper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class IndexService {
    public static final String WEBSITE_STORAGE_PATH = "./website-storage";
    public static final String DATE_AND_HOUR_PATTERN = "yyyy-MM-dd_HH";
    public static final String FIELD_URL = "url";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TEXT = "text";

    private final ComparisonService comparisonService;

    public IndexService(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    public void indexAndCompareWebsites() throws IOException {
        LocalTime startTime = LoggingHelper.logStartOfMethod("Index and compare websites");

        String dateAndHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_AND_HOUR_PATTERN));
        String workingDirectory = WEBSITE_STORAGE_PATH + "/" + dateAndHour;
        Path documentsPath = Paths.get(workingDirectory);

        Directory directory = FSDirectory.open(Paths.get(workingDirectory));

        Analyzer analyzer = new EnglishAnalyzer();
        ShingleAnalyzerWrapper shingleAnalyzerWrapper = new ShingleAnalyzerWrapper(analyzer, 3, 3, " ", false, false, "_");

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(shingleAnalyzerWrapper);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
        indexWriter.deleteAll();
        indexDocs(indexWriter, documentsPath);

        indexWriter.forceMerge(1);
        indexWriter.close();

        comparisonService.compareDocuments(dateAndHour);

        LoggingHelper.logEndOfMethod("Index and compare websites", startTime);
    }

    public static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    indexDoc(writer, file);
                } catch (IOException ignore) {
                    // don't index files that can't be read.
                }
                return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path);
        }
    }

    private static void indexDoc(IndexWriter indexWriter, Path file) throws IOException {
        if (!file.toString().endsWith(".txt")) {
            return;
        }

        String[] splitDirectoryPath = file.toString().replace(".txt", "").split("\\\\");
        String fileName = splitDirectoryPath[splitDirectoryPath.length - 1];
        String[] splitFileName = fileName.split("_");
        String websiteUrl = splitFileName[0];
        String websiteOrPage = splitFileName[1];
        String websiteId = splitFileName[2];

        try (InputStream stream = Files.newInputStream(file)) {
            Document document = new Document();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String websiteText = bufferedReader.lines().collect(Collectors.joining("\n"));

            Field urlField = new Field(FIELD_URL, websiteUrl, getFieldType());
            Field idField = new Field(FIELD_ID, websiteId, getFieldType());
            Field typeField = new Field(FIELD_TYPE, websiteOrPage, getFieldType());
            Field textField = new Field(FIELD_TEXT, websiteText, getFieldType());
            document.add(urlField);
            document.add(idField);
            document.add(typeField);
            document.add(textField);
            indexWriter.addDocument(document);
        }
    }

    private static FieldType getFieldType() {
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setTokenized(true);
        fieldType.setStoreTermVectors(true);
        fieldType.setStoreTermVectorOffsets(true);
        fieldType.setStoreTermVectorPayloads(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setOmitNorms(true);

        return fieldType;
    }
}
