package edu.uci.ics.cloudberry.noah.crawler

import java.io.File
import java.util.concurrent.TimeUnit

import com.crawljax.browser.EmbeddedBrowser.BrowserType
import com.crawljax.condition.UrlCondition
import com.crawljax.core.CrawljaxRunner
import com.crawljax.core.configuration.{BrowserConfiguration, CrawljaxConfiguration, InputSpecification}
import com.crawljax.plugins.crawloverview.CrawlOverview
import com.typesafe.config.ConfigFactory
/**
  * Created by ashka on 6/21/2016.
  */
object CrawlerDriver extends App{

  override def main(args: Array[String]) = {

    println("Enter the config file name with the path : ")

    /* Parse the configuration file */
    val conf = ConfigFactory.parseFile(new File(args(0)))

    /* Compulsory configuration parameters */
    val EntryURL = conf.getString("crawljax.url")
    val Wait_After_Reload = conf.getInt("crawljax.waitafterreload")
    val Wait_After_Event = conf.getInt("crawljax.waitafterevent")
    val OutputDir = conf.getString("crawljax.outputDir")
    val InsertRandomDataForms = conf.getBoolean("crawljax.insertrandomdataforms")

    /* Configuration */
    val builder = CrawljaxConfiguration.builderFor(EntryURL)

    /* To stay with in the domain */
    builder.crawlRules.addCrawlCondition("stay within " + EntryURL + "domain", new UrlCondition(EntryURL + "/"))

    builder.setOutputDirectory(new File(OutputDir))

    /* Let the browser type for all the cases be firefox */
    val browser = BrowserType.FIREFOX

    val browsers = 1

    builder.setBrowserConfig((new BrowserConfiguration(browser, browsers)))

    builder.crawlRules().waitAfterEvent(Wait_After_Event, TimeUnit.MILLISECONDS)

    builder.crawlRules().waitAfterReloadUrl(Wait_After_Reload, TimeUnit.MILLISECONDS)
    builder.crawlRules.insertRandomDataInInputForms(InsertRandomDataForms)
    builder.crawlRules().dontClick("a").withAttribute("href","http://www.promedmail.org")
    builder.crawlRules().dontClick("a").withAttribute("href", "http://www.isid.org")

    /* Set crawlFrame to true for every case. This ensures the crawling in order. */
    builder.crawlRules().crawlFrames(true)

    /* Set unlimited crawl states */
    builder.setMaximumDepth(0)

    /* Set unlimited run time */
    builder.setMaximumRunTime(0, TimeUnit MILLISECONDS)

    builder.crawlRules().clickOnce(true)

    /* Set the run time */
    builder.setMaximumRunTime(0, TimeUnit MILLISECONDS)

    if(conf.hasPath("input")) {
      val in = new InputSpecification()
      in.field(conf.getString("input.field")).setValue(conf.getString("input.value"))
      builder.crawlRules().setInputSpec(in)
    }

    /* Get the dont click elements to be configured */
    if(conf.hasPath("builder.crawlrules.dontclickelements")) {
      /* DIV */
      val DivIds = conf.getStringList("builder.crawlrules.dontclickelements.div.id")
      val DivClasses = conf.getStringList("builder.crawlrules.dontclickelements.div.class")

      /* TABLE */
      val TableClasses = conf.getStringList("builder.crawlrules.dontclickelements.table.class")

      /* LI */
      val LiIds = conf.getStringList("builder.crawlrules.dontclickelements.li.id")

      /* A */
      val AHref = conf.getStringList("builder.crawlrules.dontclickelements.a.href")

      /* SPAN */
      val SpanClass = conf.getStringList("builder.crawlrules.dontclickelements.span.class")

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
    }

    if(conf.hasPath("builder.crawlrules.clickelements")) {
      val AClasses = conf.getStringList("builder.crawlrules.clickelements.a.class")
      val AHref = conf.getStringList("builder.crawlrules.clickelements.a.href")
      val LiIds = conf.getString("builder.crawlrules.clickelements.li.id")
      val InputIds = conf.getStringList("builder.crawlrules.clickelements.input.id")
      val InputNames = conf.getStringList("builder.crawlrules.clickelements.input.name")
      val InputValues = conf.getStringList("builder.crawlrules.clickelements.input.value")

      builder.crawlRules().click("li").withAttribute("id", LiIds)

      var it = AClasses.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("a").withAttribute("class", it.next())
      }

      it = AHref.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("a").withAttribute("href", it.next())
      }

      it = InputIds.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("input").withAttribute("id", it.next())
      }

      it = InputNames.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("input").withAttribute("name", it.next())
      }

      it = InputValues.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("input").withAttribute("value", it.next())
      }

    }

    /*Crawloverview plugin generates an HTML report with a snapshot overview of what is crawled by Crawljax.
    Without this we do not get the folder-wise representation of crawled states*/
    builder.addPlugin(new CrawlOverview())

    /*run the crawler*/
    val crunner = new CrawljaxRunner(builder.build())
    crunner.call()
  }
}