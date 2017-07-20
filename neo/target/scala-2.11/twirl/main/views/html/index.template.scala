
package views.html

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

     object index_Scope1 {
import play.api.libs.json.JsValue
import play.api.libs.json.Json

class index extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Seq[JsValue],Form[Cloudberry.RegisterForm],play.api.i18n.Messages,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*3.2*/(datasets: Seq[JsValue], form: Form[Cloudberry.RegisterForm])(implicit messages: play.api.i18n.Messages):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {
import views.html.textarea

Seq[Any](format.raw/*3.106*/("""

"""),format.raw/*6.1*/("""
"""),_display_(/*7.2*/main("Cloudberry Admin")/*7.26*/ {_display_(Seq[Any](format.raw/*7.28*/("""
    """),format.raw/*8.5*/("""<h3>Registered Datasets</h3>
    <div class="list-group">
    """),_display_(/*10.6*/for(d <- datasets) yield /*10.24*/ {_display_(Seq[Any](format.raw/*10.26*/("""
        """),format.raw/*11.9*/("""<li class="list-group-item">
            <pre class="prettyprint">"""),_display_(/*12.39*/{
                Json.prettyPrint(d)
            }),format.raw/*14.14*/("""</pre>
        </li>
    """)))}),format.raw/*16.6*/("""
    """),format.raw/*17.5*/("""</div>

    <hr/>

    <div class="well">
    """),_display_(/*22.6*/helper/*22.12*/.form(routes.Cloudberry.webRegister(), 'class -> "form-horizontal")/*22.79*/ {_display_(Seq[Any](format.raw/*22.81*/("""

        """),_display_(/*24.10*/textarea(form("RegisterJSONString"), rows = "10", label = "Register Dataset JSON", placeholder = "copy DDL json to here")),format.raw/*24.131*/("""

        """),format.raw/*26.9*/("""<div class="form-group">
            <div class="col-sm-offset-2 col-sm-10">
                <button id="submit" type="submit" value="Submit" class="btn btn-primary">Submit</button>
            </div>
        </div>
    """)))}),format.raw/*31.6*/("""
    """),format.raw/*32.5*/("""</div>

""")))}),format.raw/*34.2*/("""
"""))
      }
    }
  }

  def render(datasets:Seq[JsValue],form:Form[Cloudberry.RegisterForm],messages:play.api.i18n.Messages): play.twirl.api.HtmlFormat.Appendable = apply(datasets,form)(messages)

  def f:((Seq[JsValue],Form[Cloudberry.RegisterForm]) => (play.api.i18n.Messages) => play.twirl.api.HtmlFormat.Appendable) = (datasets,form) => (messages) => apply(datasets,form)(messages)

  def ref: this.type = this

}


}
}

/**/
object index extends index_Scope0.index_Scope1.index
              /*
                  -- GENERATED --
                  DATE: Wed Jul 12 14:46:07 PDT 2017
                  SOURCE: /Users/yamamuraisao/Documents/cloudberry/neo/app/views/index.scala.html
                  HASH: 4be60168f72f7b9feba0bb78a2b73f990bc6f782
                  MATRIX: 679->68|905->172|933->202|960->204|992->228|1031->230|1062->235|1151->298|1185->316|1225->318|1261->327|1355->394|1427->445|1483->471|1515->476|1588->523|1603->529|1679->596|1719->598|1757->609|1900->730|1937->740|2188->961|2220->966|2259->975
                  LINES: 24->3|29->3|31->6|32->7|32->7|32->7|33->8|35->10|35->10|35->10|36->11|37->12|39->14|41->16|42->17|47->22|47->22|47->22|47->22|49->24|49->24|51->26|56->31|57->32|59->34
                  -- GENERATED --
              */
          