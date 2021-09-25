package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class CrawlAction extends RecursiveAction {

    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;


    CrawlAction(String url,
                Instant deadline,
                int maxDepth,
                Map<String, Integer> counts,
                Set<String> visitedUrls){

        this.counts = counts;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.url = url;
        this.visitedUrls = visitedUrls;
    }

    public CrawlAction.Build() {

    }

    @Override
    protected void compute(String url,
                           Instant deadline,
                           //int maxDepth,
                           PageParserFactory parserFactory,
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

    We should lock this Set here so that it is not modified
    by another thread
     */
        if (visitedUrls.contains(url)) {
            return;
        }

        //Add this url to the list of visited
        visitedUrls.add(url);
        //We will unlock visitedUrls here to keep execution running

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

}
