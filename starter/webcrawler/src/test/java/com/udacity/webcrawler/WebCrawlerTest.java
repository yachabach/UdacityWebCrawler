package com.udacity.webcrawler;

import com.google.inject.Guice;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public final class WebCrawlerTest {
  @Inject
  private WebCrawler crawler;

  private static final String DATA_DIR = System.getProperty("testDataDir");

 /*
 Produces a stream of classes. This stream is used in all the testing to apply
 the same tests to the two crawler implementations - sequential and parallel.
 Results in
      - com.udacity.webcrawler.SequentialWebCrawler
      - com.udacity.webcrawler.ParallelWebCrawler

 The string names of these classes are sent to the Builder for the crawler.
 I don't know why they were made classes in the first place.
  */
  static Stream<Class<?>> provideTestParameters() throws Exception {
    String[] names = System.getProperty("crawlerImplementations").split("\\s+");
    List<Class<?>> classes = new ArrayList<>();
    for (String name : names) {
      classes.add(Class.forName(name.strip())); //Turns strings into classes
      //System.out.println("Test Parameter: " + name);
    }

    return classes.stream();
  }

  @Test //12
  public void testOverrideToSequential() {
    System.out.println("Started testOverrideToSequential");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(SequentialWebCrawler.class.getName())
            .setParallelism(12)
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(SequentialWebCrawler.class);
    System.out.println("Completed testOverrideToSequential");
  }

  @Test //4
  public void testOverrideToParallel() {
    System.out.println("Started testOverrideToParallel");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(ParallelWebCrawler.class.getName())
            .setParallelism(12)
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(ParallelWebCrawler.class);
  }

  @Test //5
  public void testSequentialParallelism() {
    System.out.println("Started testSequentialParallelism");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setParallelism(1)
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(SequentialWebCrawler.class);
  }

  @Test //1
  public void testParallelParallelism() {
    System.out.println("Started testParallelParallelism");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setParallelism(2)
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(ParallelWebCrawler.class);
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void zeroMaxDepth(Class<?> crawlerClass) {
    System.out.println("Started zeroMaxDepth--------------------------------");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(0)
            .setPopularWordCount(3)
            .addStartPages(Paths.get(DATA_DIR, "test-page.html").toUri().toString())
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(0);
    assertThat(result.getWordCounts()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void noStartPages(Class<?> crawlerClass) {
    System.out.println("Started noStartPages");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(10)
            .setPopularWordCount(3)
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(0);
    assertThat(result.getWordCounts()).isEmpty();
  }

  //This is one of the first tests performed.
  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void testBasicCrawl(Class<?> crawlerClass) {
    System.out.println("Started testBasicCrawl -------------------------------------");

    /*
    We first create a configuration definition for the crawler
     */
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())  //From stream of classes
            .setMaxDepth(10)
            .setPopularWordCount(3)
            .addStartPages(Paths.get(DATA_DIR, "test-page.html").toUri().toString())
            .build();

    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);

    System.out.println("Name of crawlerClass: " + crawlerClass.getName() + "\n" +
            "Class of crawler: " + crawler.getClass());

    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);
    System.out.println("Crawler is the right implementation: " + crawler.getClass().isAssignableFrom(crawlerClass));

    CrawlResult result = crawler.crawl(config.getStartPages());
      System.out.println("Got a crawl result");

    assertWithMessage("Returned the wrong number of popular words")
        .that(result.getUrlsVisited())
        .isEqualTo(3);
    System.out.println("result.getUrlsVisited: " + result.getUrlsVisited());

    assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
        .that(result.getWordCounts())
        .containsEntry("the", 4);
    assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
        .that(result.getWordCounts())
        .containsEntry("jumped", 2);
    assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
        .that(result.getWordCounts())
        .containsEntry("brown", 2);
    assertWithMessage("Returned the correct words, but they are in the wrong order")
        .that(result.getWordCounts().entrySet())
        .containsExactly(
            Map.entry("the", 4),
            Map.entry("jumped", 2),
            Map.entry("brown", 2))
        .inOrder();
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void respectsIgnoredUrls(Class<?> crawlerClass) {
    System.out.println("Started respectsIgnoredUrls");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(10)
            .setPopularWordCount(3)
            .addStartPages(Paths.get(DATA_DIR, "test-page.html").toUri().toString())
            .addStartPages(Paths.get(DATA_DIR, "infinite-loop.html").toUri().toString())
            .addIgnoredUrls(".*-loop\\.html$")
            .addIgnoredUrls(".*dead-.*")
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(2);
    assertThat(result.getWordCounts().entrySet())
        .containsExactly(
            Map.entry("the", 4),
            Map.entry("jumped", 2),
            Map.entry("brown", 2))
        .inOrder();
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void respectsIgnoredWords(Class<?> crawlerClass) {
    System.out.println("Started respectsIgnoredWords");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(10)
            .setPopularWordCount(3)
            .addStartPages(Paths.get(DATA_DIR, "test-page.html").toUri().toString())
            .addIgnoredWords("^...$")
            .addIgnoredWords("^......$")
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(3);
    assertThat(result.getWordCounts().entrySet())
        .containsExactly(
            Map.entry("brown", 2),
            Map.entry("quick", 2),
            Map.entry("lazy", 2))
        .inOrder();
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void respectsMaxDepth(Class<?> crawlerClass) {
    System.out.println("Started respectsMaxDepth");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(1)
            .setPopularWordCount(20)
            .addStartPages(Paths.get(DATA_DIR, "test-page.html").toUri().toString())
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(1);
    assertThat(result.getWordCounts().entrySet())
        .containsExactly(
            Map.entry("the", 2),
            Map.entry("jumped", 1),
            Map.entry("brown", 1),
            Map.entry("quick", 1),
            Map.entry("lazy", 1),
            Map.entry("link", 1),
            Map.entry("over", 1),
            Map.entry("dog", 1),
            Map.entry("fox", 1))
        .inOrder();
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void pageNotFoundStillCountsAsVisited(Class<?> crawlerClass) {
    System.out.println("Started pageNotFoundStillCountsAsVisited");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(10)
            .setPopularWordCount(20)
            .addStartPages(Paths.get(DATA_DIR, "link-1.html").toUri().toString())
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(2);
    assertThat(result.getWordCounts().entrySet())
        .containsExactly(
            Map.entry("the", 2),
            Map.entry("jumped", 1),
            Map.entry("brown", 1),
            Map.entry("quick", 1),
            Map.entry("lazy", 1),
            Map.entry("link", 1),
            Map.entry("over", 1),
            Map.entry("dog", 1),
            Map.entry("fox", 1))
        .inOrder();
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void infiniteLoop(Class<?> crawlerClass) {
    System.out.println("Started infiniteLoop");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(10)
            .setPopularWordCount(3)
            .setTimeoutSeconds(1)
            .addStartPages(Paths.get(DATA_DIR, "infinite-loop.html").toUri().toString())
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(2);
    assertThat(result.getWordCounts().entrySet())
        .containsExactly(
            Map.entry("the", 4),
            Map.entry("jumped", 2),
            Map.entry("brown", 2))
        .inOrder();
  }

  @ParameterizedTest
  @MethodSource("provideTestParameters")
  public void multipleStartingUrls(Class<?> crawlerClass) {
    System.out.println("Started multipleStartingUrls");
    CrawlerConfiguration config =
        new CrawlerConfiguration.Builder()
            .setImplementationOverride(crawlerClass.getName())
            .setMaxDepth(10)
            .setPopularWordCount(3)
            .addStartPages(Paths.get(DATA_DIR, "test-page.html").toUri().toString())
            .addStartPages(Paths.get(DATA_DIR, "link-1.html").toUri().toString())
            .addStartPages(Paths.get(DATA_DIR, "infinite-loop.html").toUri().toString())
            .build();
    Guice.createInjector(new WebCrawlerModule(config), new NoOpProfilerModule())
        .injectMembers(this);
    assertThat(crawler.getClass()).isAssignableTo(crawlerClass);

    CrawlResult result = crawler.crawl(config.getStartPages());

    assertThat(result.getUrlsVisited()).isEqualTo(5);
    assertThat(result.getWordCounts().entrySet())
        .containsExactly(
            Map.entry("the", 8),
            Map.entry("jumped", 4),
            Map.entry("brown", 4))
        .inOrder();
  }
}