package com.cloudbees.hd3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.apache.commons.io.IOUtils;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.DocumentType;
import dev.langchain4j.data.document.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiModelName;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class LangChain4J {

    private static final String API_KEY = System.getenv("OPENAI_TOKEN");
    private static final Path BASEDIR = Paths.get("/home/jerome/projects/cloudbees-aborted-builds-plugin");

    public static void main(final String[] args) throws URISyntaxException, FileNotFoundException, IOException {
        final EmbeddingModel embeddingModel = OpenAiEmbeddingModel.withApiKey(API_KEY);
        final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        final EmbeddingStoreRetriever embeddingStoreRetriever = EmbeddingStoreRetriever.from(embeddingStore,
                embeddingModel);

        final ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .modelName(OpenAiModelName.GPT_4)
                .modelName(OpenAiModelName.GPT_3_5_TURBO_0613)
                .apiKey(API_KEY)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(5)
                .build();

        final ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
                .chatLanguageModel(chatLanguageModel)
                .retriever(embeddingStoreRetriever)
                .build();

        final EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        final String testClass = "com.cloudbees.jenkins.plugins.abortedbuilds.BasicTest";

        ingestor.ingest(FileSystemDocumentLoader.loadDocument(
                BASEDIR.resolve(String.format("target/surefire-reports/%s.txt", testClass)),
                DocumentType.TXT));

        ingestor.ingest(FileSystemDocumentLoader.loadDocument(
                BASEDIR.resolve("test.patch"),
                DocumentType.TXT));

        final String prompt = IOUtils.toString(LangChain4J.class.getClassLoader().getResource("prompt.txt"),
                Charset.defaultCharset());
        System.out.println(chain.execute(prompt));
    }
}
