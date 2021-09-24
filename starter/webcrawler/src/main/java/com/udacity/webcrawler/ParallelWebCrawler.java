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

  @Inject
  ParallelWebCrawler(
      Clock clock,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {

    /*Repositories for word counts and visited urls.  In a parallel
    implementation these will have to be thread safe.

    counts - concurrent for read-write performance
    visitedUrls - synchronized for data integrity
     */
    Map<String, Integer> counts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());

    //Get the PageParser object from Guice
    Injector injector = Guice.createInjector(new ParserModule.Builder().build());
    PageParserFactory parserFactory = injector.getInstance(PageParserFactory.class);
    for (String url: startingUrls) {
      PageParser.Result result = parserFactory.get(url).parse();
    }

    return new CrawlResult.Builder().build();
  }

  private void crawlInternal(
          String url,
          Instant deadline,
          int maxDepth,
          Map<String, Integer> counts,
          Set<String> visitedUrls) {

    //Check that we haven't timed out
    if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
      return;
    }

    //Skip urls that match the ignoredUrls pattern
    for (Pattern pattern : ignoredUrls) {
      if (pattern.matcher(url).matches()) {
        return;
      }
    }

    /*
    skip urls that have already been visited

    We will lock this Set here so that it is not modified
    by another thread between reading and updating.
     */
    synchronized (visitedUrls) {
      if (visitedUrls.contains(url)) {
        return;
      }
      //Add this url to the list of visited
      visitedUrls.add(url);
      //We will unlock visitedUrls here to keep execution running
    }

    PageParser.Result result = parserFactory.get(url).parse();

    //Update word counts
    for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
      if (counts.containsKey(e.getKey())) {
        counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
      } else {
        counts.put(e.getKey(), e.getValue());
      }
    }

    //Recurse down the tree of links within this url
    for (String link : result.getLinks()) {
      crawlInternal(link, deadline, maxDepth - 1, counts, visitedUrls);
    }
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
