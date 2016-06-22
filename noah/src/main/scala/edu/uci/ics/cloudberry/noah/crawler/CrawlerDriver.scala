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
  val configPath = "C:\\Users\\ashka\\IdeaProjects\\cloudberry\\noah\\src\\main\\resources\\crawler\\promedmail.conf"
  val conf = ConfigFactory.parseFile(new File(configPath))
  val URL = conf.getString("crawljax.url")
  val WAR = conf.getInt("crawljax.waitafterreload")
  val WAE = conf.getInt("crawljax.waitafterevent")
  val OutputDir = conf.getString("crawljax.outputDir")
  val InsertRandomDataForms = conf.getBoolean("crawljax.insertrandomdataforms")
  val DivIds = conf.getStringList("builder.crawlrules.element.div.id")
  val DivClasses = conf.getStringList("builder.crawlrules.element.div.class")
  val TableClasses = conf.getStringList("builder.crawlrules.element.table.class")
  val LiIds = conf.getStringList("builder.crawlrules.element.li.id")
  val AHref = conf.getStringList("builder.crawlrules.element.a.href")


    /* Configuration */
  val builder = CrawljaxConfiguration.builderFor(URL)
  builder.crawlRules.addCrawlCondition("stay within promedmail domain", new UrlCondition(URL+"/"))
  builder.setOutputDirectory(new File(OutputDir))
  val browser = BrowserType.FIREFOX
  builder.crawlRules().waitAfterEvent(WAE, TimeUnit.MILLISECONDS)
  builder.crawlRules().waitAfterReloadUrl(WAR,TimeUnit.MILLISECONDS)
  builder.crawlRules.insertRandomDataInInputForms(InsertRandomDataForms)
  var it = DivIds.iterator()
  while(it.hasNext){
    builder.crawlRules().dontClick("div").withAttribute("id",it.next())
  }

  it = DivClasses.iterator()
  while(it.hasNext) {
    builder.crawlRules().dontClick("div").withAttribute("class",it.next())
  }

  it = TableClasses.iterator()
  while(it.hasNext) {
    builder.crawlRules().dontClick("table").withAttribute("class",it.next())
  }

  it = LiIds.iterator()
  while(it.hasNext) {
    builder.crawlRules().dontClick("li").withAttribute("id",it.next())
  }

  it = AHref.iterator()
  while(it.hasNext) {
    builder.crawlRules().dontClick("a").withAttribute("href",it.next())
  }

  builder.addPlugin(new CrawlOverview())

}
