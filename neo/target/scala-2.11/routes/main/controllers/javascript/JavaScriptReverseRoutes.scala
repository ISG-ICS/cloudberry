
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/yamamuraisao/Documents/cloudberry/neo/conf/routes
// @DATE:Wed Jul 12 14:46:06 PDT 2017

import play.api.routing.JavaScriptReverseRoute
import play.api.mvc.{ QueryStringBindable, PathBindable, Call, JavascriptLiteral }
import play.core.routing.{ HandlerDef, ReverseRouteContext, queryString, dynamicString }


import _root_.controllers.Assets.Asset

// @LINE:6
package controllers.javascript {
  import ReverseRouteContext.empty

  // @LINE:15
  class ReverseAssets(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:16
    def at: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Assets.at",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "favicon.ico"})
        }
      """
    )
  
    // @LINE:15
    def versioned: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Assets.versioned",
      """
        function(file1) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "assets/" + (""" + implicitly[PathBindable[Asset]].javascriptUnbind + """)("file", file1)})
        }
      """
    )
  
  }

  // @LINE:6
  class ReverseCloudberry(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:8
    def berryQuery: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Cloudberry.berryQuery",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "berry"})
        }
      """
    )
  
    // @LINE:9
    def webRegister: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Cloudberry.webRegister",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "webregister"})
        }
      """
    )
  
    // @LINE:7
    def ws: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Cloudberry.ws",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "ws"})
        }
      """
    )
  
    // @LINE:11
    def deregister: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Cloudberry.deregister",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "admin/deregister"})
        }
      """
    )
  
    // @LINE:10
    def register: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Cloudberry.register",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "admin/register"})
        }
      """
    )
  
    // @LINE:6
    def index: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.Cloudberry.index",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + """"})
        }
      """
    )
  
  }


}
