
package views.html.twittermap

import play.twirl.api._
import play.twirl.api.TemplateMagic._


     object index_Scope0 {
import models._
import controllers._
import play.api.i18n._
import views.html._
import play.api.templates.PlayMagic._
import play.api.mvc._
import play.api.data._

class index extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template8[String,String,String,Boolean,String,Boolean,Seq[String],Boolean,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*2.2*/(title: String, ws: String, startDate: String, sentimentEnabled: Boolean, sentimentUDF: String, removeSearchBar: Boolean, predefinedKeywords: Seq[String], isDrugMap: Boolean):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {


Seq[Any](format.raw/*2.176*/(""" """),_display_(/*2.178*/main(title, ws, startDate, sentimentEnabled, sentimentUDF, removeSearchBar, predefinedKeywords, isDrugMap)/*2.284*/{_display_(Seq[Any](format.raw/*2.285*/("""
"""),format.raw/*3.1*/("""<div xmlns="http://www.w3.org/1999/html" ng-controller="AppCtrl">

  <div class="map-group">
    <alert-bar></alert-bar>

    <map lat="39.5" lng="-96.35" zoom="4"></map>

    <div id="logo">
    </div>
    
    <div id ='powerby'>
      <div class="btn btn-info btn-xs">
        <a href="http://cloudberry.ics.uci.edu/" title="A Distributed middleware by ICS,UCI" class="white"> Cloudberry</a>
          |
        <a href="https://www.youtube.com/watch?v=XwlRm0jcsU4" class="white">Video</a> 
      </div>
   </div>

    
   </div>

   <search-bar></search-bar>
   <predefined-keywords></predefined-keywords>



  <div class="stats">
    <time-series id="chart"></time-series>
  </div>

  <div id='sidebar' ng-init="click = -1" ng-class=""""),format.raw/*33.52*/("""{"""),format.raw/*33.53*/("""toggled: click === 1"""),format.raw/*33.73*/("""}"""),format.raw/*33.74*/("""" >
    <div class = "col-xs-2">
      <ul class="nav nav-tabs tabs-left">
        <li role="presentation" ng-click="click = 1"><a href="#hashtag" data-toggle="tab"><i class="fa fa-hashtag fa-2x" aria-hidden="true"></i></a></li>
        <li role="presentation" ng-click="click = 1"><a href="#tweet" data-toggle="tab"><i class="fa fa-twitter fa-2x" aria-hidden="true"></i></a></li>
        <li role="presentation" ng-click="click = 1" class="active"><a href="#about" data-toggle="tab"><i class="fa fa-info-circle fa-2x"></i></a></li>
        <li role="presentation" ng-click="click = click*-1"><i ng-class="click ===-1 ? 'fa fa-chevron-left fa-2x': 'fa fa-chevron-right fa-2x'"></i></li>
      </ul>
    </div>
    <div class="col-xs-10">
      <div class="tab-content">
        <hashtag id="hashtag" class="tab-pane"></hashtag>
        <tweet id="tweet" class="tab-pane"></tweet>
        <div id="about" class="tab-pane active">
          <h1> About </h1>
          <p><b>Cloudberry</b> is a research prototype to support interactive analytics and visualization of large amounts of spatial-temporal data. </p>
          <p> Basic Information: </p>
          <ul>
            <li>Data set: Tweets</li>
            <li>Number of records: > 500,000,000 </li>
            <li>Collection period: From 2015-11-23 </li>
            <li>Total data size: > 500G bytes</li>
            <li>The live tweets is appending to db at the speed of ~30 tweets/sec </li>
            <li><a href="https://github.com/ISG-ICS/cloudberry">Source code</a></li>
          </ul>
          <p>The backend is running the big data management system <b><a href="http://asterixdb.apache.org/">Apache AsterixDB</a></b>
          to support large compute clusters. Here is the small NUC cluster where the server runs! </p>
          <img src='"""),_display_(/*60.22*/routes/*60.28*/.Assets.versioned("images/nuc.cluster.jpg")),format.raw/*60.71*/("""' width="256" height="192"><br>
          <p>For questions and comments, please contact <b>
              <a href="mailto:&#105;&#099;&#115;&#045;&#099;&#108;&#111;&#117;&#100;&#098;&#101;&#114;&#114;&#121;&#064;&#117;&#099;&#105;&#046;&#101;&#100;&#117;">
                &#105;&#099;&#115;&#045;&#099;&#108;&#111;&#117;&#100;&#098;&#101;&#114;&#114;&#121;&#064;&#117;&#099;&#105;&#046;&#101;&#100;&#117;</a></b></p>
        </div>
      </div>
    </div>
  </div>

  <div class = "exception-bar">
    <exception-bar></exception-bar>
  </div>



</div>


""")))}),format.raw/*78.2*/("""
"""))
      }
    }
  }

  def render(title:String,ws:String,startDate:String,sentimentEnabled:Boolean,sentimentUDF:String,removeSearchBar:Boolean,predefinedKeywords:Seq[String],isDrugMap:Boolean): play.twirl.api.HtmlFormat.Appendable = apply(title,ws,startDate,sentimentEnabled,sentimentUDF,removeSearchBar,predefinedKeywords,isDrugMap)

  def f:((String,String,String,Boolean,String,Boolean,Seq[String],Boolean) => play.twirl.api.HtmlFormat.Appendable) = (title,ws,startDate,sentimentEnabled,sentimentUDF,removeSearchBar,predefinedKeywords,isDrugMap) => apply(title,ws,startDate,sentimentEnabled,sentimentUDF,removeSearchBar,predefinedKeywords,isDrugMap)

  def ref: this.type = this

}


}

/**/
object index extends index_Scope0.index
              /*
                  -- GENERATED --
                  DATE: Fri Jul 14 16:30:57 PDT 2017
                  SOURCE: /Users/yamamuraisao/Documents/cloudberry/twittermap/app/views/twittermap/index.scala.html
                  HASH: 32053b1a83d99ee575f35bc8c5c936317bc1c430
                  MATRIX: 595->2|865->176|894->178|1009->284|1048->285|1075->286|1842->1025|1871->1026|1919->1046|1948->1047|3786->2858|3801->2864|3865->2907|4452->3464
                  LINES: 20->2|25->2|25->2|25->2|25->2|26->3|56->33|56->33|56->33|56->33|83->60|83->60|83->60|101->78
                  -- GENERATED --
              */
          