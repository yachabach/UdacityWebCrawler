package com.udacity.webcrawler;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import com.udacity.webcrawler.parser.ParserModule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {

  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.ignoredUrls = ignoredUrls;
    this.maxDepth = maxDepth;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {

    System.out.println("Started Parallel crawler");
    //Set a timeout
    Instant deadline = clock.instant().plus(timeout);
    System.out.println("Crawler set the deadline");

    /*Repositories for word counts and visited urls.  In a parallel
    implementation these will have to be thread safe.

    counts - concurrent for read-write performance
    visitedUrls - synchronized for data integrity
     */
    Map<String, Integer> counts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());

    //Get the PageParser object from Guice

    //Injector injector = Guice.createInjector(new ParserModule());
    //System.out.println("Created injector");
    //PageParserFactory parserFactory = injector.getInstance(PageParserFactory.class);
    //System.out.println("Created parserFactory");

    System.out.println("Starting Crawl Action");
    CrawlActionFrame cAF = new CrawlActionFrame.Builder()
            .setClock(clock)
            .setCounts(counts)
            .setDeadline(deadline)
            .setPool(pool)
            .setIgnoredUrls(ignoredUrls)
            .setParserFactory(parserFactory)
            .setVisitedUrls(visitedUrls)
            .build();

    //Initiate the crawl at each root url
    for (String url: startingUrls) {
      CrawlActionImpl crawlAction = new CrawlActionImpl(url, maxDepth, cAF);
      pool.invoke(crawlAction);
    }

    return new CrawlResult.Builder().build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
