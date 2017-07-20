
package views.html.dashboard

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

class index extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[String,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(title: String):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {


Seq[Any](format.raw/*1.17*/("""
"""),format.raw/*2.1*/("""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>"""),_display_(/*6.11*/title),format.raw/*6.16*/("""</title>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*7.48*/routes/*7.54*/.Assets.versioned("lib/bootstrap/css/bootstrap.css")),format.raw/*7.106*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*8.48*/routes/*8.54*/.Assets.versioned("lib/angular-dashboard-framework/dist/angular-dashboard-framework.min.css")),format.raw/*8.147*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*9.48*/routes/*9.54*/.Assets.versioned("javascripts/lib/adf-widget-iframe-0.1.2/dist/adf-widget-iframe.min.css")),format.raw/*9.145*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*10.48*/routes/*10.54*/.Assets.versioned("lib/dc.js/dc.min.css")),format.raw/*10.95*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*11.48*/routes/*11.54*/.Assets.versioned("lib/bootstrap/css/bootstrap.css")),format.raw/*11.106*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*12.48*/routes/*12.54*/.Assets.versioned("lib/leaflet/leaflet.css")),format.raw/*12.98*/("""'>


  <script src='"""),_display_(/*15.17*/routes/*15.23*/.Assets.versioned("lib/angularjs/angular.min.js")),format.raw/*15.72*/("""'></script>
  <script type='text/javascript' src='"""),_display_(/*16.40*/routes/*16.46*/.Assets.versioned("lib/leaflet/leaflet.js")),format.raw/*16.89*/("""'></script>
  <script type='text/javascript' src='"""),_display_(/*17.40*/routes/*17.46*/.Assets.versioned("lib/angular-leaflet-directive/angular-leaflet-directive.js")),format.raw/*17.125*/("""'></script>
  <script type='text/javascript' src='"""),_display_(/*18.40*/routes/*18.46*/.Assets.versioned("lib/jquery/jquery.min.js")),format.raw/*18.91*/("""'></script>
  <script type='text/javascript' src='"""),_display_(/*19.40*/routes/*19.46*/.Assets.versioned("lib/d3/d3.min.js")),format.raw/*19.83*/("""'></script>
  <script type='text/javascript' src='"""),_display_(/*20.40*/routes/*20.46*/.Assets.versioned("lib/crossfilter/crossfilter.min.js")),format.raw/*20.101*/("""'></script>
  <script type='text/javascript' src='"""),_display_(/*21.40*/routes/*21.46*/.Assets.versioned("lib/dc.js/dc.min.js")),format.raw/*21.86*/("""'></script>
  <script src='"""),_display_(/*22.17*/routes/*22.23*/.Assets.versioned("javascripts/lib/dc.leaflet.js")),format.raw/*22.73*/("""'></script>
  <script type='text/javascript' src='"""),_display_(/*23.40*/routes/*23.46*/.Assets.versioned("javascripts/dashboard/controllers.js")),format.raw/*23.103*/("""'></script>
  <script src='"""),_display_(/*24.17*/routes/*24.23*/.Assets.versioned("javascripts/dashboard.js")),format.raw/*24.68*/("""'></script>

  <style>
    #map """),format.raw/*27.10*/("""{"""),format.raw/*27.11*/("""
      """),format.raw/*28.7*/("""border: 1px solid black;
      border-radius: 8px;
    """),format.raw/*30.5*/("""}"""),format.raw/*30.6*/("""
  """),format.raw/*31.3*/("""</style>

</head>
<body ng-app="dashboard" ng-controller="dashboardCtrl">
  <div class = "container-fluid">
    <div class="row">
      <h1>Cloudberry Example Dashboard</h1>
    </div>
    <div class="row">
      <map id="map" config="mapConf" class="col-md-6" style="height: 350px"></map>
      <row-chart id="rowchart" config="rowchartConf" class="col-md-6"></row-chart>
    </div>
    <div class="row">
      <pie-chart id="piechart" config="piechartConf" class="col-md-2"></pie-chart>
      <pie-chart id="piechart" config="piechartConf" class="col-md-2"></pie-chart>
      <pie-chart id="piechart" config="piechartConf" class="col-md-2"></pie-chart>
      <line-chart id = "linechart" config="linechartConf" class="col-md-6"></line-chart>
    </div>
  </div>
</body>
</html>"""))
      }
    }
  }

  def render(title:String): play.twirl.api.HtmlFormat.Appendable = apply(title)

  def f:((String) => play.twirl.api.HtmlFormat.Appendable) = (title) => apply(title)

  def ref: this.type = this

}


}

/**/
object index extends index_Scope0.index
              /*
                  -- GENERATED --
                  DATE: Wed Jul 12 14:16:58 PDT 2017
                  SOURCE: /Users/yamamuraisao/Documents/cloudberry/twittermap/app/views/dashboard/index.scala.html
                  HASH: 1f7e2c464e458d3c521fc8e5fe11811184cc1643
                  MATRIX: 537->1|647->16|674->17|775->92|800->97|882->153|896->159|969->211|1045->261|1059->267|1173->360|1249->410|1263->416|1375->507|1452->557|1467->563|1529->604|1606->654|1621->660|1695->712|1772->762|1787->768|1852->812|1900->833|1915->839|1985->888|2063->939|2078->945|2142->988|2220->1039|2235->1045|2336->1124|2414->1175|2429->1181|2495->1226|2573->1277|2588->1283|2646->1320|2724->1371|2739->1377|2816->1432|2894->1483|2909->1489|2970->1529|3025->1557|3040->1563|3111->1613|3189->1664|3204->1670|3283->1727|3338->1755|3353->1761|3419->1806|3479->1838|3508->1839|3542->1846|3624->1901|3652->1902|3682->1905
                  LINES: 20->1|25->1|26->2|30->6|30->6|31->7|31->7|31->7|32->8|32->8|32->8|33->9|33->9|33->9|34->10|34->10|34->10|35->11|35->11|35->11|36->12|36->12|36->12|39->15|39->15|39->15|40->16|40->16|40->16|41->17|41->17|41->17|42->18|42->18|42->18|43->19|43->19|43->19|44->20|44->20|44->20|45->21|45->21|45->21|46->22|46->22|46->22|47->23|47->23|47->23|48->24|48->24|48->24|51->27|51->27|52->28|54->30|54->30|55->31
                  -- GENERATED --
              */
          