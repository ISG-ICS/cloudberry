package edu.uci.ics.cloudberry.noah.crawler

import java.io.File
import java.util.concurrent.TimeUnit

import com.crawljax.browser.EmbeddedBrowser.BrowserType
import com.crawljax.condition.UrlCondition
import com.crawljax.core.CrawljaxRunner
import com.crawljax.core.configuration.{CrawljaxConfiguration, InputSpecification}
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

    builder.crawlRules().waitAfterEvent(Wait_After_Event, TimeUnit.MILLISECONDS)

    builder.crawlRules().waitAfterReloadUrl(Wait_After_Reload, TimeUnit.MILLISECONDS)

    builder.crawlRules.insertRandomDataInInputForms(InsertRandomDataForms)

    /* Set crawlFrame to true for every case. This ensures the crawling in order. */
    builder.crawlRules().crawlFrames(true)

    /* Set the maximum depth the crawler can crawler (default is 2, 0 is infinite)*/
    builder.setMaximumDepth(conf.getInt("crawljax.maxdepth"))

    if(conf.hasPath("input")) {
      val in = new InputSpecification();
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
      val AIds = conf.getStringList("builder.crawlrules.clickelements.a.id")
      val AClasses = conf.getStringList("builder.crawlrules.clickelements.a.class")
      val AText = conf.getStringList("builder.crawlrules.clickelements.a.text")
      val AHref = conf.getStringList("builder.crawlrules.clickelements.a.href")

      var it = AIds.iterator()

      while(it.hasNext){
        builder.crawlRules().click("a").withAttribute("id", it.next())
      }

      it = AClasses.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("a").withAttribute("class", it.next())
      }

      it = AHref.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("a").withAttribute("href", it.next())
      }

      it = AText.iterator()
      while(it.hasNext) {
        builder.crawlRules().click("a").withText(it.next())
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