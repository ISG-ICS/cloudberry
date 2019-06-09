(function () {
  var myConnector = tableau.makeConnector();
  
  myConnector.getSchema = function (schemaCallback) {
    var cols = [{
        id: "id",
        dataType: tableau.dataTypeEnum.string
    }, {
        id: "title",
        alias: "Title",
        dataType: tableau.dataTypeEnum.string
    }, {
        id: "year",
        alias: "Year",
        dataType: tableau.dataTypeEnum.int
    }, {
        id: "director",
        alias: "Director",
        dataType: tableau.dataTypeEnum.string
    }];
  
    var tableSchema = {
        id: "movies",
        alias: "movies",
        columns: cols
    };
  
    schemaCallback([tableSchema]);
  };
  
  myConnector.getData = function(table, doneCallback) {
    $.getJSON("https://josephzheng1998.github.io/TableauWDC/movies.txt", function(resp) {

        var tableData = [];
        
        // Iterate over the JSON object
        for (var i = 0, len = resp.length; i < len; i++) {
            tableData.push({
                "id": resp[i].id,
                "title": resp[i].title,
                "year": resp[i].year,
                "director": resp[i].director
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
