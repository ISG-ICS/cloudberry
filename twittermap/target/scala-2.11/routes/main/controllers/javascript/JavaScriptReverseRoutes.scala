
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/yamamuraisao/Documents/cloudberry/twittermap/conf/routes
// @DATE:Wed Jul 12 14:16:57 PDT 2017

import play.api.routing.JavaScriptReverseRoute
import play.api.mvc.{ QueryStringBindable, PathBindable, Call, JavascriptLiteral }
import play.core.routing.{ HandlerDef, ReverseRouteContext, queryString, dynamicString }


import _root_.controllers.Assets.Asset

// @LINE:6
package controllers.javascript {
  import ReverseRouteContext.empty

  // @LINE:6
  class ReverseTwitterMapApplication(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:9
    def getCity: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.TwitterMapApplication.getCity",
      """
        function(neLat0,swLat1,neLng2,swLng3) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "city/" + (""" + implicitly[PathBindable[Double]].javascriptUnbind + """)("neLat", neLat0) + "/" + (""" + implicitly[PathBindable[Double]].javascriptUnbind + """)("swLat", swLat1) + "/" + (""" + implicitly[PathBindable[Double]].javascriptUnbind + """)("neLng", neLng2) + "/" + (""" + implicitly[PathBindable[Double]].javascriptUnbind + """)("swLng", swLng3)})
        }
      """
    )
  
    // @LINE:7
    def drugmap: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.TwitterMapApplication.drugmap",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "drugmap"})
        }
      """
    )
  
    // @LINE:6
    def index: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.TwitterMapApplication.index",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + """"})
        }
      """
    )
  
  }

  // @LINE:13
  class ReverseAssets(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:14
    def at: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Assets.at",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "favicon.ico"})
        }
      """
    )
  
    // @LINE:13
    def versioned: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Assets.versioned",
      """
        function(file1) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "assets/" + (""" + implicitly[PathBindable[Asset]].javascriptUnbind + """)("file", file1)})
        }
      """
    )
  
  }


}
