package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    //crawler and profiler are instantiated here - thank you Guice
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    //Crawl the given pages
    CrawlResult result = crawler.crawl(config.getStartPages());

    //Create an object to write results in a JSON format
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    // TODO: Write the crawl results to a JSON file (or System.out if the file name is empty)

    /*
    Here we simply establish the connection to the output receiver - either the
    console or a path to a file or other storage.
     */
    if (config.getResultPath().isEmpty()) {
      Writer out = new BufferedWriter(new OutputStreamWriter(System.out));
      resultWriter.write(out);
    } else {
      Path path = Path.of(config.getResultPath());
      resultWriter.write(path);
      System.out.println(path);
    }
    // TODO: Write the profile data to a text file (or System.out if the file name is empty)
    if (config.getProfileOutputPath().isEmpty()) {
      Writer out = new BufferedWriter(new OutputStreamWriter(System.out));
      profiler.writeData(out);
    } else {
      Path path = Path.of(config.getProfileOutputPath());
      profiler.writeData(path);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
