
package views.html

import play.twirl.api._
import play.twirl.api.TemplateMagic._


     object textarea_Scope0 {
import models._
import controllers._
import play.api.i18n._
import views.html._
import play.api.templates.PlayMagic._
import play.api.mvc._
import play.api.data._

class textarea extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template5[Field,String,String,String,String,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(field: Field, rows: String = "3", label: String = "CHANGEME", placeholder: String = "", help: String = ""):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {


Seq[Any](format.raw/*1.109*/("""

  """),format.raw/*3.3*/("""<div class="form-group """),_display_(/*3.27*/if(field.hasErrors)/*3.46*/ {_display_(Seq[Any](format.raw/*3.48*/("""has-error""")))}),format.raw/*3.58*/("""">
    <label class="col-sm-2 control-label">"""),_display_(/*4.44*/label),format.raw/*4.49*/("""</label>
    <div class="col-sm-10">
      <textarea class="form-control"
                rows=""""),_display_(/*7.24*/rows),format.raw/*7.28*/("""" 
                id=""""),_display_(/*8.22*/field/*8.27*/.id),format.raw/*8.30*/("""" 
                name=""""),_display_(/*9.24*/field/*9.29*/.name),format.raw/*9.34*/("""" 
                placeholder=""""),_display_(/*10.31*/placeholder),format.raw/*10.42*/("""" 
                >"""),_display_(/*11.19*/field/*11.24*/.value.getOrElse("")),format.raw/*11.44*/("""</textarea>
      <span class="help-block">"""),_display_(/*12.33*/help),format.raw/*12.37*/("""</span>
      <span class="help-block">"""),_display_(/*13.33*/{field.error.map { error => error.message }}),format.raw/*13.77*/("""</span>
    </div>
  </div>
"""))
      }
    }
  }

  def render(field:Field,rows:String,label:String,placeholder:String,help:String): play.twirl.api.HtmlFormat.Appendable = apply(field,rows,label,placeholder,help)

  def f:((Field,String,String,String,String) => play.twirl.api.HtmlFormat.Appendable) = (field,rows,label,placeholder,help) => apply(field,rows,label,placeholder,help)

  def ref: this.type = this

}


}

/**/
object textarea extends textarea_Scope0.textarea
              /*
                  -- GENERATED --
                  DATE: Wed Jul 12 14:46:07 PDT 2017
                  SOURCE: /Users/yamamuraisao/Documents/cloudberry/neo/app/views/textarea.scala.html
                  HASH: d29eb6272a4c2b4580ec7e35dadc6f89b709939c
                  MATRIX: 560->1|763->108|793->112|843->136|870->155|909->157|949->167|1021->213|1046->218|1169->315|1193->319|1243->343|1256->348|1279->351|1331->377|1344->382|1369->387|1429->420|1461->431|1509->452|1523->457|1564->477|1635->521|1660->525|1727->565|1792->609
                  LINES: 20->1|25->1|27->3|27->3|27->3|27->3|27->3|28->4|28->4|31->7|31->7|32->8|32->8|32->8|33->9|33->9|33->9|34->10|34->10|35->11|35->11|35->11|36->12|36->12|37->13|37->13
                  -- GENERATED --
              */
          