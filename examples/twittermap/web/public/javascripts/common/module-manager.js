/**
 * moduleManager - A service of AngularJS
 *  This service manages the interactions among modules in Twittermap by an event pub/sub framework abstraction.
 *  It provides 3 API functions:
 *   - subscribeEvent(eventName, eventHandler)
 *   - unsubscribeEvent(eventName, eventHandler)
 *   - publishEvent(eventName, event)
 *  It provides 5 built-in events:
 *   - "change search keyword": CHANGE_SEARCH_KEYWORD
 *       * when user change the keyword and "enter" or click "submit", this event triggered.
 *   - "change time series range": CHANGE_TIME_SERIES_RANGE
 *       * when time series bar range changed, this event triggered.
 *   - "change zoom level": CHANGE_ZOOM_LEVEL
 *       * when zoomed in or out, this event triggered.
 *   - "change region by drag": CHANGE_REGION_BY_DRAG
 *       * when the view region changed by drag, this event triggered.
 *   - "change map type": CHANGE_MAP_TYPE
 *       * when sidebar map type changed, this event triggered.
 *  Example:
 *  (1) in "countmap.js":
 *      function onZoomCountmap(event) {
 *        // issue a new query
 *      }
 *
 *      function initCountmap() {
 *        ...
 *        moduleManager.subscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, onZoomCountmap);
 *        ...
 *      }
 *
 *      function cleanCountmap() {
 *        ...
 *        moduleManager.unsubscribeEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, onZoomCountmap);
 *        ...
 *      }
 *  (2) in "controllers.js":
 *      $scope.$on("leafletDirectiveMap.zoomend", function() {
 *        // load new polygons
 *        ...
 *        moduleManager.publishEvent(moduleManager.EVENT.CHANGE_ZOOM_LEVEL, {level: currentZoomLevel});
 *        ...
 *      }
 */
angular.module("cloudberry.common")
  .service("moduleManager", function() {

    var moduleManager = {

      // Built-in events
      EVENT: {
        CHANGE_SEARCH_KEYWORD: "CHANGE_SEARCH_KEYWORD",
        CHANGE_TIME_SERIES_RANGE: "CHANGE_TIME_SERIES_RANGE",
        CHANGE_ZOOM_LEVEL: "CHANGE_ZOOM_LEVEL",
        CHANGE_REGION_BY_DRAG: "CHANGE_REGION_BY_DRAG",
        CHANGE_MAP_TYPE: "CHANGE_MAP_TYPE"
      },

      /**
       * check if eventName is valid
       *
       * @param eventName
       * @returns {boolean} true: if valid, false: otherwise
       */
      isEventValid(eventName) {
        var isValid = false;
        Object.keys(moduleManager.EVENT).forEach(function(key) {
          if (moduleManager.EVENT[key] === eventName) {
            isValid = true;
          }
        });
        return isValid;
      },

      eventsListeners: {},

      /**
       * subscribe an event:
       *   once this event happens, the eventHandler function will be called.
       *
       * @param eventName moduleManager.EVENT, event name
       * @param eventHandler function, callback function(event): event is the content object of the event
       * @returns {boolean} true: if subscribed successfully, false: otherwise
       */
      subscribeEvent(eventName, eventHandler) {

        if (!this.isEventValid(eventName)) {
          return false;
        }

        if (eventHandler instanceof Function) {
          if (eventName in this.eventsListeners) {
            this.eventsListeners[eventName].add(eventHandler);
          }
          else {
            this.eventsListeners[eventName] = new Set([eventHandler]);
          }
        }
        else {
          return false;
        }
        return true;
      },

      /**
       * unsubscribe an event:
       *   after unsubscribe successfully, eventHandler function will not be called when event happens later
       *
       * @param eventName moduleManager.EVENT, event name
       * @param eventHandler function, callback function(event)
       * @returns {boolean} true: if unsubscribe successfully, false: otherwise
       */
      unsubscribeEvent(eventName, eventHandler) {

        if (eventName in this.eventsListeners) {
          this.eventsListeners[eventName].delete(eventHandler);
        }
        return true;
      },

      /**
       * publish an event:
       *   publish a single event instance once.
       *
       * @param eventName moduleManager.EVENT, event name
       * @param event object, event content which will be passed to every subscriber's eventHandler
       * @returns {boolean} true: if publish successfully, false: otherwise
       */
      publishEvent(eventName, event) {

        if (eventName in this.eventsListeners) {
          for (let eventHandler of this.eventsListeners[eventName]) {
            eventHandler(event);
          }
        }
        return true;
      }
    };

    return moduleManager;
  });