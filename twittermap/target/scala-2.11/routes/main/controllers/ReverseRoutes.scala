
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/yamamuraisao/Documents/cloudberry/twittermap/conf/routes
// @DATE:Wed Jul 12 14:16:57 PDT 2017

import play.api.mvc.{ QueryStringBindable, PathBindable, Call, JavascriptLiteral }
import play.core.routing.{ HandlerDef, ReverseRouteContext, queryString, dynamicString }


import _root_.controllers.Assets.Asset

// @LINE:6
package controllers {

  // @LINE:6
  class ReverseTwitterMapApplication(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:9
    def getCity(neLat:Double, swLat:Double, neLng:Double, swLng:Double): Call = {
      import ReverseRouteContext.empty
      Call("GET", _prefix + { _defaultPrefix } + "city/" + implicitly[PathBindable[Double]].unbind("neLat", neLat) + "/" + implicitly[PathBindable[Double]].unbind("swLat", swLat) + "/" + implicitly[PathBindable[Double]].unbind("neLng", neLng) + "/" + implicitly[PathBindable[Double]].unbind("swLng", swLng))
    }
  
    // @LINE:7
    def drugmap(): Call = {
      import ReverseRouteContext.empty
      Call("GET", _prefix + { _defaultPrefix } + "drugmap")
    }
  
    // @LINE:6
    def index(): Call = {
      import ReverseRouteContext.empty
      Call("GET", _prefix)
    }
  
  }

  // @LINE:13
  class ReverseAssets(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:14
    def at(): Call = {
      implicit val _rrc = new ReverseRouteContext(Map(("path", "/public/images"), ("file", "favicon.ico")))
      Call("GET", _prefix + { _defaultPrefix } + "favicon.ico")
    }
  
    // @LINE:13
    def versioned(file:Asset): Call = {
      implicit val _rrc = new ReverseRouteContext(Map(("path", "/public")))
      Call("GET", _prefix + { _defaultPrefix } + "assets/" + implicitly[PathBindable[Asset]].unbind("file", file))
    }
  
  }


}
