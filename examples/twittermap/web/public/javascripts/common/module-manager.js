angular.module("cloudberry.common")
  .service("moduleManager", function() {

    var moduleManager = {

      // Built-in events
      EVENT: {
        CLICK_SEARCH_BUTTON: "clickSearchButtonEvent",
        CHANGE_TIME_SERIES_RANGE: "changeTimeSeriesRangeEvent",
        CHANGE_ZOOM_LEVEL: "changeZoomLevelEvent",
        CHANGE_REGION_BY_DRAG: "changeRegionByDragEvent",
        CHANGE_MAP_TYPE: "changeMapTypeEvent"
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

        if (!(eventName in this.EVENT)) {
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
          for (var i = 0; i < this.eventsListeners[eventName].length; i++) {
            this.eventsListeners[eventName][i](event);
          }
        }
        return true;
      }
    };

    return moduleManager;
  });