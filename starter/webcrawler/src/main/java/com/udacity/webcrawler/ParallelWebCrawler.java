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

    //Set a timeout
    Instant deadline = clock.instant().plus(timeout);

    /*Repositories for word counts and visited urls.  In a parallel
    implementation these will have to be thread safe.

    counts - concurrent for read-write performance
    visitedUrls - synchronized for data integrity
     */
    Map<String, Integer> counts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());

    /*
    CrawlActionFrame is a class used to pass parameters that remain unchanged
    to the RecursiveAction class CrawlAction.  These variables should live in
    CrawlAction but I was unable to debug that design.  This was a solution to
    complete the assignment.
     */
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

    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(counts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(counts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
