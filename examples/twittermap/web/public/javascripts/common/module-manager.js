angular.module('cloudberry.common')
  .service('moduleManager', function() {

    var moduleManager = {

      // Built-in events
      EVENT: {
        SEARCH_BUTTON: "eventSearchButtonClick",
        TIME_RANGE: "eventTimeRangeChange",
        ZOOM: "eventZoom",
        DRAG: "eventDrag",
        MAP_TYPE: "eventMapTypeChange"
      },

      eventsListensers: {},

      subscribeEvent(eventName, eventHandler) {
        if (eventHandler instanceof Function) {
          if (eventName in this.eventsListensers) {
            this.eventsListensers[eventName].push(eventHandler);
          }
          else {
            this.eventsListensers[eventName] = [eventHandler];
          }
        }
        else {
          return false;
        }
      },

      publishEvent(eventName, event) {

        if (! eventName in this.EVENT) {
          return false;
        }

        if (eventName in this.eventsListensers) {
          for (var i = 0; i < this.eventsListensers[eventName].length; i ++) {
            this.eventsListensers[eventName][i](event);
          }
        }

        return true;
      }
    };

    return moduleManager;
  });