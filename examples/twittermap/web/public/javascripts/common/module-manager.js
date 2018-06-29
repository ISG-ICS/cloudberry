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

      eventsListeners: {},

      /**
       * subscribe an event:
       *   once this event happens, the eventHandler function will be called.
       *
       * @param eventName moduleManager.EVENT, event name
       * @param eventHandler function, callback function(event): event is the content object of the event
       * @param priority number, defines the order of calling eventHandlers, 0 is highest
       * @returns {boolean}
       */
      subscribeEvent(eventName, eventHandler, priority) {

        var isPriority = false;
        if (priority instanceof Number) {
          isPriority = true;
        }
        else {
          priority = 0;
        }

        if (eventHandler instanceof Function) {
          if (eventName in this.eventsListeners) {
            this.eventsListeners[eventName].push({p: priority, h: eventHandler});

            if (isPriority) {
              this.eventsListeners[eventName].sort(function (a, b) {
                a.p - b.p
              });
            }
          }
          else {
            this.eventsListeners[eventName] = [{p: priority, h: eventHandler}];
          }
        }
        else {
          return false;
        }
      },

      /**
       * unsubscribe an event
       *
       * @param eventName
       * @param eventHandler
       * @returns {boolean}
       */
      unsubscribeEvent(eventName, eventHandler) {

        if (eventName in this.eventsListeners) {
          var index = this.eventsListeners[eventName].findIndex(function (element) {
            if (element.h === eventHandler)
              return true;
          });

          if (index >= 0) {
            this.eventsListeners[eventName].splice(index, 1);
          }
        }

        return true;
      },

      /**
       * publish an event:
       *   publish a single event instance once.
       *
       * @param eventName moduleManager.EVENT, event name
       * @param event object, event content which will be passed to every subscriber's eventHandler
       * @returns {boolean}
       */
      publishEvent(eventName, event) {

        if (!eventName in this.EVENT) {
          return false;
        }

        if (eventName in this.eventsListeners) {
          for (var i = 0; i < this.eventsListeners[eventName].length; i++) {
            this.eventsListeners[eventName][i].h(event);
          }
        }

        return true;
      }

    };

    return moduleManager;
  });