
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/yamamuraisao/Documents/cloudberry/neo/conf/routes
// @DATE:Wed Jul 12 14:46:06 PDT 2017

package router

import play.core.routing._
import play.core.routing.HandlerInvokerFactory._
import play.core.j._

import play.api.mvc._

import _root_.controllers.Assets.Asset

class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler, 
  // @LINE:6
  Cloudberry_1: controllers.Cloudberry,
  // @LINE:15
  Assets_0: controllers.Assets,
  val prefix: String
) extends GeneratedRouter {

   @javax.inject.Inject()
   def this(errorHandler: play.api.http.HttpErrorHandler,
    // @LINE:6
    Cloudberry_1: controllers.Cloudberry,
    // @LINE:15
    Assets_0: controllers.Assets
  ) = this(errorHandler, Cloudberry_1, Assets_0, "/")

  import ReverseRouteContext.empty

  def withPrefix(prefix: String): Routes = {
    router.RoutesPrefix.setPrefix(prefix)
    new Routes(errorHandler, Cloudberry_1, Assets_0, prefix)
  }

  private[this] val defaultPrefix: String = {
    if (this.prefix.endsWith("/")) "" else "/"
  }

  def documentation = List(
    ("""GET""", this.prefix, """controllers.Cloudberry.index"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """ws""", """controllers.Cloudberry.ws"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """berry""", """controllers.Cloudberry.berryQuery"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """webregister""", """controllers.Cloudberry.webRegister"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """admin/register""", """controllers.Cloudberry.register"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """admin/deregister""", """controllers.Cloudberry.deregister"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """assets/""" + "$" + """file<.+>""", """controllers.Assets.versioned(path:String = "/public", file:Asset)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """favicon.ico""", """controllers.Assets.at(path:String = "/public/images", file:String = "favicon.ico")"""),
    Nil
  ).foldLeft(List.empty[(String,String,String)]) { (s,e) => e.asInstanceOf[Any] match {
    case r @ (_,_,_) => s :+ r.asInstanceOf[(String,String,String)]
    case l => s ++ l.asInstanceOf[List[(String,String,String)]]
  }}


  // @LINE:6
  private[this] lazy val controllers_Cloudberry_index0_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix)))
  )
  private[this] lazy val controllers_Cloudberry_index0_invoker = createInvoker(
    Cloudberry_1.index,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Cloudberry",
      "index",
      Nil,
      "GET",
      """ cloudberry routes""",
      this.prefix + """"""
    )
  )

  // @LINE:7
  private[this] lazy val controllers_Cloudberry_ws1_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("ws")))
  )
  private[this] lazy val controllers_Cloudberry_ws1_invoker = createInvoker(
    Cloudberry_1.ws,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Cloudberry",
      "ws",
      Nil,
      "GET",
      """""",
      this.prefix + """ws"""
    )
  )

  // @LINE:8
  private[this] lazy val controllers_Cloudberry_berryQuery2_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("berry")))
  )
  private[this] lazy val controllers_Cloudberry_berryQuery2_invoker = createInvoker(
    Cloudberry_1.berryQuery,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Cloudberry",
      "berryQuery",
      Nil,
      "POST",
      """""",
      this.prefix + """berry"""
    )
  )

  // @LINE:9
  private[this] lazy val controllers_Cloudberry_webRegister3_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("webregister")))
  )
  private[this] lazy val controllers_Cloudberry_webRegister3_invoker = createInvoker(
    Cloudberry_1.webRegister,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Cloudberry",
      "webRegister",
      Nil,
      "POST",
      """""",
      this.prefix + """webregister"""
    )
  )

  // @LINE:10
  private[this] lazy val controllers_Cloudberry_register4_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("admin/register")))
  )
  private[this] lazy val controllers_Cloudberry_register4_invoker = createInvoker(
    Cloudberry_1.register,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Cloudberry",
      "register",
      Nil,
      "POST",
      """""",
      this.prefix + """admin/register"""
    )
  )

  // @LINE:11
  private[this] lazy val controllers_Cloudberry_deregister5_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("admin/deregister")))
  )
  private[this] lazy val controllers_Cloudberry_deregister5_invoker = createInvoker(
    Cloudberry_1.deregister,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Cloudberry",
      "deregister",
      Nil,
      "POST",
      """""",
      this.prefix + """admin/deregister"""
    )
  )

  // @LINE:15
  private[this] lazy val controllers_Assets_versioned6_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("assets/"), DynamicPart("file", """.+""",false)))
  )
  private[this] lazy val controllers_Assets_versioned6_invoker = createInvoker(
    Assets_0.versioned(fakeValue[String], fakeValue[Asset]),
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Assets",
      "versioned",
      Seq(classOf[String], classOf[Asset]),
      "GET",
      """ Map static resources from the /public folder to the /assets URL path""",
      this.prefix + """assets/""" + "$" + """file<.+>"""
    )
  )

  // @LINE:16
  private[this] lazy val controllers_Assets_at7_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("favicon.ico")))
  )
  private[this] lazy val controllers_Assets_at7_invoker = createInvoker(
    Assets_0.at(fakeValue[String], fakeValue[String]),
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Assets",
      "at",
      Seq(classOf[String], classOf[String]),
      "GET",
      """""",
      this.prefix + """favicon.ico"""
    )
  )


  def routes: PartialFunction[RequestHeader, Handler] = {
  
    // @LINE:6
    case controllers_Cloudberry_index0_route(params) =>
      call { 
        controllers_Cloudberry_index0_invoker.call(Cloudberry_1.index)
      }
  
    // @LINE:7
    case controllers_Cloudberry_ws1_route(params) =>
      call { 
        controllers_Cloudberry_ws1_invoker.call(Cloudberry_1.ws)
      }
  
    // @LINE:8
    case controllers_Cloudberry_berryQuery2_route(params) =>
      call { 
        controllers_Cloudberry_berryQuery2_invoker.call(Cloudberry_1.berryQuery)
      }
  
    // @LINE:9
    case controllers_Cloudberry_webRegister3_route(params) =>
      call { 
        controllers_Cloudberry_webRegister3_invoker.call(Cloudberry_1.webRegister)
      }
  
    // @LINE:10
    case controllers_Cloudberry_register4_route(params) =>
      call { 
        controllers_Cloudberry_register4_invoker.call(Cloudberry_1.register)
      }
  
    // @LINE:11
    case controllers_Cloudberry_deregister5_route(params) =>
      call { 
        controllers_Cloudberry_deregister5_invoker.call(Cloudberry_1.deregister)
      }
  
    // @LINE:15
    case controllers_Assets_versioned6_route(params) =>
      call(Param[String]("path", Right("/public")), params.fromPath[Asset]("file", None)) { (path, file) =>
        controllers_Assets_versioned6_invoker.call(Assets_0.versioned(path, file))
      }
  
    // @LINE:16
    case controllers_Assets_at7_route(params) =>
      call(Param[String]("path", Right("/public/images")), Param[String]("file", Right("favicon.ico"))) { (path, file) =>
        controllers_Assets_at7_invoker.call(Assets_0.at(path, file))
      }
  }
}
