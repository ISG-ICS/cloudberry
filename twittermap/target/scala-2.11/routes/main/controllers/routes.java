
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/yamamuraisao/Documents/cloudberry/twittermap/conf/routes
// @DATE:Wed Jul 12 14:16:57 PDT 2017

package controllers;

import router.RoutesPrefix;

public class routes {
  
  public static final controllers.ReverseTwitterMapApplication TwitterMapApplication = new controllers.ReverseTwitterMapApplication(RoutesPrefix.byNamePrefix());
  public static final controllers.ReverseAssets Assets = new controllers.ReverseAssets(RoutesPrefix.byNamePrefix());

  public static class javascript {
    
    public static final controllers.javascript.ReverseTwitterMapApplication TwitterMapApplication = new controllers.javascript.ReverseTwitterMapApplication(RoutesPrefix.byNamePrefix());
    public static final controllers.javascript.ReverseAssets Assets = new controllers.javascript.ReverseAssets(RoutesPrefix.byNamePrefix());
  }

}
