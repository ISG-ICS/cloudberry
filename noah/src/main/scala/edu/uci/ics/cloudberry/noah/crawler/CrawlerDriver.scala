package edu.uci.ics.cloudberry.noah.crawler

import java.io.File
import java.util.concurrent.TimeUnit

import com.crawljax.browser.EmbeddedBrowser.BrowserType
import com.crawljax.condition.UrlCondition
import com.crawljax.core.CrawljaxRunner
import com.crawljax.core.configuration.CrawljaxConfiguration
import com.crawljax.plugins.crawloverview.CrawlOverview
import com.typesafe.config.ConfigFactory
/**
  * Created by ashka on 6/21/2016.
  */
object CrawlerDriver extends App{


  override def main(args: Array[String]) = {

    println("Enter the name of the config file (promedmail/proquest/factiva) : ")

    /* Parse the configuration file */
    val conf = ConfigFactory.parseFile(new File(args(0)))

    /* Compulsory configuration parameters */
    val EntryURL = conf.getString("crawljax.url")
    val Wait_After_Reload = conf.getInt("crawljax.waitafterreload")
    val Wait_After_Event = conf.getInt("crawljax.waitafterevent")
    val OutputDir = conf.getString("crawljax.outputDir")
    val maxCrawlTime = conf.getInt("crawljax.maxtime")
    val InsertRandomDataForms = conf.getBoolean("crawljax.insertrandomdataforms")

    /* Get the elements to be configured */
    /* DIV */
    val DivIds = conf.getStringList("builder.crawlrules.element.div.id")
    val DivClasses = conf.getStringList("builder.crawlrules.element.div.class")

    /* TABLE */
    val TableIds = conf.getStringList("builder.crawlrules.element.table.id")
    val TableClasses = conf.getStringList("builder.crawlrules.element.table.class")

    /* LI */
    val LiIds = conf.getStringList("builder.crawlrules.element.li.id")
    val LiClasses = conf.getStringList("builder.crawlrules.element.li.class")

    /* A */
    val AHref = conf.getStringList("builder.crawlrules.element.a.href")

    /* SPAN */
    val SpanIds = conf.getStringList("builder.crawlrules.element.span.id")
    val SpanClass = conf.getStringList("builder.crawlrules.element.span.class")

    /* Configuration */
    val builder = CrawljaxConfiguration.builderFor(EntryURL)

    builder.crawlRules.addCrawlCondition("stay within " + EntryURL + "domain", new UrlCondition(EntryURL + "/"))

    builder.setOutputDirectory(new File(OutputDir))

    /* Let the browser type for all the cases be firefox */
    val browser = BrowserType.FIREFOX

    builder.crawlRules().waitAfterEvent(Wait_After_Event, TimeUnit.MILLISECONDS)

    builder.crawlRules().waitAfterReloadUrl(Wait_After_Reload, TimeUnit.MILLISECONDS)

    builder.crawlRules.insertRandomDataInInputForms(InsertRandomDataForms)

    /* Set crawlFrame to true for every case. This ensures the crawling in order. */
    builder.crawlRules().crawlFrames(true)

    builder.setMaximumRunTime(maxCrawlTime, TimeUnit HOURS)

    var it = DivIds.iterator()

    while (it.hasNext) {
      builder.crawlRules().dontClick("div").withAttribute("id", it.next())
    }

    it = DivClasses.iterator()

    while (it.hasNext) {
      builder.crawlRules().dontClick("div").withAttribute("class", it.next())
    }

    it = TableClasses.iterator()

    while (it.hasNext) {
      builder.crawlRules().dontClick("table").withAttribute("class", it.next())
    }

    it = SpanClass.iterator()
    while (it.hasNext) {
      builder.crawlRules().dontClick("span").withAttribute("class", it.next())
    }
    it = LiIds.iterator()

    while (it.hasNext) {
      builder.crawlRules().dontClick("li").withAttribute("id", it.next())
    }

    it = AHref.iterator()

    while (it.hasNext) {
      builder.crawlRules().dontClick("a").withAttribute("href", it.next())
    }


    /*Crawloverview plugin generates an HTML report with a snapshot overview of what is crawled by Crawljax.
    Without this we do not get the folder-wise representation of crawled states*/
    builder.addPlugin(new CrawlOverview())

    /*run the crawler*/
    val crunner = new CrawljaxRunner(builder.build())
    crunner.call()
  }
}
