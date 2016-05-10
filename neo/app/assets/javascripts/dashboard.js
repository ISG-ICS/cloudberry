/**
 * Created by Pro on 5/9/16.
 */
angular.module('dashboard', ['adf','adf.structures.base', 'adf.widget.iframe'])
  .config(function(dashboardProvider){

    dashboardProvider
      .structure('6-6', {
        rows: [{
          columns: [{
            styleClass: 'col-md-6'
          }, {
            styleClass: 'col-md-6'
          }]
        }]
      });
  });