
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/yamamuraisao/Documents/cloudberry/twittermap/conf/routes
// @DATE:Wed Jul 12 14:16:57 PDT 2017

package router

import play.core.routing._
import play.core.routing.HandlerInvokerFactory._
import play.core.j._

import play.api.mvc._

import _root_.controllers.Assets.Asset

class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler, 
  // @LINE:6
  TwitterMapApplication_1: controllers.TwitterMapApplication,
  // @LINE:13
  Assets_0: controllers.Assets,
  val prefix: String
) extends GeneratedRouter {

   @javax.inject.Inject()
   def this(errorHandler: play.api.http.HttpErrorHandler,
    // @LINE:6
    TwitterMapApplication_1: controllers.TwitterMapApplication,
    // @LINE:13
    Assets_0: controllers.Assets
  ) = this(errorHandler, TwitterMapApplication_1, Assets_0, "/")

  import ReverseRouteContext.empty

  def withPrefix(prefix: String): Routes = {
    router.RoutesPrefix.setPrefix(prefix)
    new Routes(errorHandler, TwitterMapApplication_1, Assets_0, prefix)
  }

  private[this] val defaultPrefix: String = {
    if (this.prefix.endsWith("/")) "" else "/"
  }

  def documentation = List(
    ("""GET""", this.prefix, """controllers.TwitterMapApplication.index"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """drugmap""", """controllers.TwitterMapApplication.drugmap"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """city/""" + "$" + """neLat<[^/]+>/""" + "$" + """swLat<[^/]+>/""" + "$" + """neLng<[^/]+>/""" + "$" + """swLng<[^/]+>""", """controllers.TwitterMapApplication.getCity(neLat:Double, swLat:Double, neLng:Double, swLng:Double)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """assets/""" + "$" + """file<.+>""", """controllers.Assets.versioned(path:String = "/public", file:Asset)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """favicon.ico""", """controllers.Assets.at(path:String = "/public/images", file:String = "favicon.ico")"""),
    Nil
  ).foldLeft(List.empty[(String,String,String)]) { (s,e) => e.asInstanceOf[Any] match {
    case r @ (_,_,_) => s :+ r.asInstanceOf[(String,String,String)]
    case l => s ++ l.asInstanceOf[List[(String,String,String)]]
  }}


  // @LINE:6
  private[this] lazy val controllers_TwitterMapApplication_index0_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix)))
  )
  private[this] lazy val controllers_TwitterMapApplication_index0_invoker = createInvoker(
    TwitterMapApplication_1.index,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.TwitterMapApplication",
      "index",
      Nil,
      "GET",
      """ TwitterMap routes""",
      this.prefix + """"""
    )
  )

  // @LINE:7
  private[this] lazy val controllers_TwitterMapApplication_drugmap1_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("drugmap")))
  )
  private[this] lazy val controllers_TwitterMapApplication_drugmap1_invoker = createInvoker(
    TwitterMapApplication_1.drugmap,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.TwitterMapApplication",
      "drugmap",
      Nil,
      "GET",
      """""",
      this.prefix + """drugmap"""
    )
  )

  // @LINE:9
  private[this] lazy val controllers_TwitterMapApplication_getCity2_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("city/"), DynamicPart("neLat", """[^/]+""",true), StaticPart("/"), DynamicPart("swLat", """[^/]+""",true), StaticPart("/"), DynamicPart("neLng", """[^/]+""",true), StaticPart("/"), DynamicPart("swLng", """[^/]+""",true)))
  )
  private[this] lazy val controllers_TwitterMapApplication_getCity2_invoker = createInvoker(
    TwitterMapApplication_1.getCity(fakeValue[Double], fakeValue[Double], fakeValue[Double], fakeValue[Double]),
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.TwitterMapApplication",
      "getCity",
      Seq(classOf[Double], classOf[Double], classOf[Double], classOf[Double]),
      "GET",
      """""",
      this.prefix + """city/""" + "$" + """neLat<[^/]+>/""" + "$" + """swLat<[^/]+>/""" + "$" + """neLng<[^/]+>/""" + "$" + """swLng<[^/]+>"""
    )
  )

  // @LINE:13
  private[this] lazy val controllers_Assets_versioned3_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("assets/"), DynamicPart("file", """.+""",false)))
  )
  private[this] lazy val controllers_Assets_versioned3_invoker = createInvoker(
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

  // @LINE:14
  private[this] lazy val controllers_Assets_at4_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("favicon.ico")))
  )
  private[this] lazy val controllers_Assets_at4_invoker = createInvoker(
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
    case controllers_TwitterMapApplication_index0_route(params) =>
      call { 
        controllers_TwitterMapApplication_index0_invoker.call(TwitterMapApplication_1.index)
      }
  
    // @LINE:7
    case controllers_TwitterMapApplication_drugmap1_route(params) =>
      call { 
        controllers_TwitterMapApplication_drugmap1_invoker.call(TwitterMapApplication_1.drugmap)
      }
  
    // @LINE:9
    case controllers_TwitterMapApplication_getCity2_route(params) =>
      call(params.fromPath[Double]("neLat", None), params.fromPath[Double]("swLat", None), params.fromPath[Double]("neLng", None), params.fromPath[Double]("swLng", None)) { (neLat, swLat, neLng, swLng) =>
        controllers_TwitterMapApplication_getCity2_invoker.call(TwitterMapApplication_1.getCity(neLat, swLat, neLng, swLng))
      }
  
    // @LINE:13
    case controllers_Assets_versioned3_route(params) =>
      call(Param[String]("path", Right("/public")), params.fromPath[Asset]("file", None)) { (path, file) =>
        controllers_Assets_versioned3_invoker.call(Assets_0.versioned(path, file))
      }
  
    // @LINE:14
    case controllers_Assets_at4_route(params) =>
      call(Param[String]("path", Right("/public/images")), Param[String]("file", Right("favicon.ico"))) { (path, file) =>
        controllers_Assets_at4_invoker.call(Assets_0.at(path, file))
      }
  }
}
