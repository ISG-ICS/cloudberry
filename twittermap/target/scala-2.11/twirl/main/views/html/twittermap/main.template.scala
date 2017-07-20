
package views.html.twittermap

import play.twirl.api._
import play.twirl.api.TemplateMagic._


     object main_Scope0 {
import models._
import controllers._
import play.api.i18n._
import views.html._
import play.api.templates.PlayMagic._
import play.api.mvc._
import play.api.data._

class main extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template9[String,String,String,Boolean,String,Boolean,Seq[String],Boolean,Html,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(title: String, ws: String, startDate: String, sentimentEnabled: Boolean, sentimentUDF: String, removeSearchBar: Boolean, predefinedKeywords: Seq[String], isDrugMap: Boolean)(body: Html):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {
import play.api.libs.json.Json

Seq[Any](format.raw/*1.188*/("""

"""),format.raw/*4.1*/("""
"""),format.raw/*5.1*/("""<!DOCTYPE html>
<html ng-app="cloudberry">

<head>
  <title>"""),_display_(/*9.11*/title),format.raw/*9.16*/("""</title>
  <meta charset="UTF-8" name="viewport" content="width=device-width, initial-scale=1.0">

  <link rel='shortcut icon' type='image/png' href='"""),_display_(/*12.53*/routes/*12.59*/.Assets.versioned("images/hyrax.png")),format.raw/*12.96*/("""'>

  <link rel="stylesheet" media="screen" href='"""),_display_(/*14.48*/routes/*14.54*/.Assets.versioned("lib/bootstrap/css/bootstrap.css")),format.raw/*14.106*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*15.48*/routes/*15.54*/.Assets.versioned("lib/leaflet/leaflet.css")),format.raw/*15.98*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*16.48*/routes/*16.54*/.Assets.versioned("lib/dc.js/dc.min.css")),format.raw/*16.95*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*17.48*/routes/*17.54*/.Assets.versioned("lib/font-awesome/css/font-awesome.min.css")),format.raw/*17.116*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*18.48*/routes/*18.54*/.Assets.versioned("lib/bootstrap-vertical-tabs/bootstrap.vertical-tabs.min.css")),format.raw/*18.134*/("""'>
  <link rel="stylesheet" media="screen" href='"""),_display_(/*19.48*/routes/*19.54*/.Assets.versioned("lib/bootstrap-toggle/css/bootstrap-toggle.min.css")),format.raw/*19.124*/("""'>
  <link rel='stylesheet' href='"""),_display_(/*20.33*/routes/*20.39*/.Assets.versioned("stylesheets/main.css")),format.raw/*20.80*/("""'>

  <script>
    var config= """),format.raw/*23.17*/("""{"""),format.raw/*23.18*/("""
      """),format.raw/*24.7*/("""wsURL:""""),_display_(/*24.15*/ws),format.raw/*24.17*/("""",
      sentimentEnabled: """),_display_(/*25.26*/sentimentEnabled),format.raw/*25.42*/(""",
      sentimentUDF: """"),_display_(/*26.23*/sentimentUDF),format.raw/*26.35*/("""",
      startDate: new Date(""""),_display_(/*27.29*/startDate),format.raw/*27.38*/(""""),
      """),_display_(/*28.8*/if(isDrugMap)/*28.21*/ {_display_(Seq[Any](format.raw/*28.23*/("""
        """),format.raw/*29.9*/("""mapLegend: "$",
        sumText : "dollars",
      """)))}/*31.9*/else/*31.14*/{_display_(Seq[Any](format.raw/*31.15*/("""
        """),format.raw/*32.9*/("""mapLegend: "Count:",
        sumText : "tweets",
      """)))}),format.raw/*34.8*/("""
      """),format.raw/*35.7*/("""removeSearchBar: """),_display_(/*35.25*/removeSearchBar),format.raw/*35.40*/(""",
      predefinedKeywords: """),_display_(/*36.28*/Html(Json.stringify(Json.toJson(predefinedKeywords)))),format.raw/*36.81*/("""
    """),format.raw/*37.5*/("""}"""),format.raw/*37.6*/("""
  """),format.raw/*38.3*/("""</script>
  <script src='"""),_display_(/*39.17*/routes/*39.23*/.Assets.versioned("lib/angularjs/angular.min.js")),format.raw/*39.72*/("""'></script>
  <script src='"""),_display_(/*40.17*/routes/*40.23*/.Assets.versioned("lib/json-bigint/dist/json-bigint.js")),format.raw/*40.79*/("""'></script>
  <script src='"""),_display_(/*41.17*/routes/*41.23*/.Assets.versioned("lib/leaflet/leaflet.js")),format.raw/*41.66*/("""'></script>
  <script src='"""),_display_(/*42.17*/routes/*42.23*/.Assets.versioned("lib/angular-leaflet-directive/angular-leaflet-directive.js")),format.raw/*42.102*/("""'></script>
  <script src='"""),_display_(/*43.17*/routes/*43.23*/.Assets.versioned("lib/jquery/jquery.min.js")),format.raw/*43.68*/("""'></script>
  <script src='"""),_display_(/*44.17*/routes/*44.23*/.Assets.versioned("lib/d3/d3.min.js")),format.raw/*44.60*/("""'></script>
  <script src='"""),_display_(/*45.17*/routes/*45.23*/.Assets.versioned("lib/crossfilter/crossfilter.min.js")),format.raw/*45.78*/("""'></script>
  <script src='"""),_display_(/*46.17*/routes/*46.23*/.Assets.versioned("lib/dc.js/dc.min.js")),format.raw/*46.63*/("""'></script>
  <script src='"""),_display_(/*47.17*/routes/*47.23*/.Assets.versioned("lib/bootstrap/js/bootstrap.min.js")),format.raw/*47.77*/("""'></script>
  <script src='"""),_display_(/*48.17*/routes/*48.23*/.Assets.versioned("lib/bootstrap-toggle/js/bootstrap-toggle.min.js")),format.raw/*48.91*/("""'></script>

  <script src='"""),_display_(/*50.17*/routes/*50.23*/.Assets.versioned("javascripts/app.js")),format.raw/*50.62*/("""'></script>
  <script src='"""),_display_(/*51.17*/routes/*51.23*/.Assets.versioned("javascripts/map/controllers.js")),format.raw/*51.74*/("""'></script>

  """),_display_(/*53.4*/if(isDrugMap)/*53.17*/ {_display_(Seq[Any](format.raw/*53.19*/("""
    """),format.raw/*54.5*/("""<script src='"""),_display_(/*54.19*/routes/*54.25*/.Assets.versioned("javascripts/common/services-drug.js")),format.raw/*54.81*/("""'></script>
  """)))}/*55.5*/else/*55.10*/{_display_(Seq[Any](format.raw/*55.11*/("""
    """),format.raw/*56.5*/("""<script src='"""),_display_(/*56.19*/routes/*56.25*/.Assets.versioned("javascripts/common/services.js")),format.raw/*56.76*/("""'></script>
  """)))}),format.raw/*57.4*/("""
  """),format.raw/*58.3*/("""<script src='"""),_display_(/*58.17*/routes/*58.23*/.Assets.versioned("javascripts/timeseries/controllers.js")),format.raw/*58.81*/("""'></script>
  <script src='"""),_display_(/*59.17*/routes/*59.23*/.Assets.versioned("javascripts/sidebar/controllers.js")),format.raw/*59.78*/("""'></script>
  <script src='"""),_display_(/*60.17*/routes/*60.23*/.Assets.versioned("javascripts/searchbar/controllers.js")),format.raw/*60.80*/("""'></script>

</head>

<body>
"""),_display_(/*65.2*/body),format.raw/*65.6*/("""
"""),format.raw/*66.1*/("""</body>
</html>
"""))
      }
    }
  }

  def render(title:String,ws:String,startDate:String,sentimentEnabled:Boolean,sentimentUDF:String,removeSearchBar:Boolean,predefinedKeywords:Seq[String],isDrugMap:Boolean,body:Html): play.twirl.api.HtmlFormat.Appendable = apply(title,ws,startDate,sentimentEnabled,sentimentUDF,removeSearchBar,predefinedKeywords,isDrugMap)(body)

  def f:((String,String,String,Boolean,String,Boolean,Seq[String],Boolean) => (Html) => play.twirl.api.HtmlFormat.Appendable) = (title,ws,startDate,sentimentEnabled,sentimentUDF,removeSearchBar,predefinedKeywords,isDrugMap) => (body) => apply(title,ws,startDate,sentimentEnabled,sentimentUDF,removeSearchBar,predefinedKeywords,isDrugMap)(body)

  def ref: this.type = this

}


}

/**/
object main extends main_Scope0.main
              /*
                  -- GENERATED --
                  DATE: Fri Jul 14 16:30:57 PDT 2017
                  SOURCE: /Users/yamamuraisao/Documents/cloudberry/twittermap/app/views/twittermap/main.scala.html
                  HASH: 53bd3734cc337ebecbd7aee8c22ac9af3e3faba4
                  MATRIX: 598->1|910->187|938->221|965->222|1052->283|1077->288|1255->439|1270->445|1328->482|1406->533|1421->539|1495->591|1572->641|1587->647|1652->691|1729->741|1744->747|1806->788|1883->838|1898->844|1982->906|2059->956|2074->962|2176->1042|2253->1092|2268->1098|2360->1168|2422->1203|2437->1209|2499->1250|2558->1281|2587->1282|2621->1289|2656->1297|2679->1299|2734->1327|2771->1343|2822->1367|2855->1379|2913->1410|2943->1419|2980->1430|3002->1443|3042->1445|3078->1454|3148->1507|3161->1512|3200->1513|3236->1522|3322->1578|3356->1585|3401->1603|3437->1618|3493->1647|3567->1700|3599->1705|3627->1706|3657->1709|3710->1735|3725->1741|3795->1790|3850->1818|3865->1824|3942->1880|3997->1908|4012->1914|4076->1957|4131->1985|4146->1991|4247->2070|4302->2098|4317->2104|4383->2149|4438->2177|4453->2183|4511->2220|4566->2248|4581->2254|4657->2309|4712->2337|4727->2343|4788->2383|4843->2411|4858->2417|4933->2471|4988->2499|5003->2505|5092->2573|5148->2602|5163->2608|5223->2647|5278->2675|5293->2681|5365->2732|5407->2748|5429->2761|5469->2763|5501->2768|5542->2782|5557->2788|5634->2844|5667->2860|5680->2865|5719->2866|5751->2871|5792->2885|5807->2891|5879->2942|5924->2957|5954->2960|5995->2974|6010->2980|6089->3038|6144->3066|6159->3072|6235->3127|6290->3155|6305->3161|6383->3218|6439->3248|6463->3252|6491->3253
                  LINES: 20->1|25->1|27->4|28->5|32->9|32->9|35->12|35->12|35->12|37->14|37->14|37->14|38->15|38->15|38->15|39->16|39->16|39->16|40->17|40->17|40->17|41->18|41->18|41->18|42->19|42->19|42->19|43->20|43->20|43->20|46->23|46->23|47->24|47->24|47->24|48->25|48->25|49->26|49->26|50->27|50->27|51->28|51->28|51->28|52->29|54->31|54->31|54->31|55->32|57->34|58->35|58->35|58->35|59->36|59->36|60->37|60->37|61->38|62->39|62->39|62->39|63->40|63->40|63->40|64->41|64->41|64->41|65->42|65->42|65->42|66->43|66->43|66->43|67->44|67->44|67->44|68->45|68->45|68->45|69->46|69->46|69->46|70->47|70->47|70->47|71->48|71->48|71->48|73->50|73->50|73->50|74->51|74->51|74->51|76->53|76->53|76->53|77->54|77->54|77->54|77->54|78->55|78->55|78->55|79->56|79->56|79->56|79->56|80->57|81->58|81->58|81->58|81->58|82->59|82->59|82->59|83->60|83->60|83->60|88->65|88->65|89->66
                  -- GENERATED --
              */
          