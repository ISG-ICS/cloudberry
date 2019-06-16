(function () {
  var myConnector = tableau.makeConnector();
  
  myConnector.getSchema = function (schemaCallback) {
    var cols = [{
        id: "id",
        dataType: tableau.dataTypeEnum.string
    }, {
        id: "name",
        alias: "Name",
        dataType: tableau.dataTypeEnum.string
    }, {
        id: "birthYear",
        alias: "Birth Year",
        dataType: tableau.dataTypeEnum.string
    }];
  
    var tableSchema = {
        id: "stars",
        alias: "stars",
        columns: cols
    };
  
    schemaCallback([tableSchema]);
  };
  
  myConnector.getData = function(table, doneCallback) {
    $.getJSON("https://josephzheng1998.github.io/TableauWDC/stars.txt", function(resp) {

        var tableData = [];
        
        // Iterate over the JSON object
        for (var i = 0, len = resp.length; i < len; i++) {
            tableData.push({
                "id": resp[i].id,
                "name": resp[i].name,
                "birthYear": resp[i].birthYear
            });
        }
            
        table.appendRows(tableData);
        doneCallback();
    });
  };
  
  $(document).ready(function () {
    $("#submitButton").click(function () {
        tableau.connectionName = "MovieDB";
        tableau.submit();
    });
  });
  
  tableau.registerConnector(myConnector);
})();
