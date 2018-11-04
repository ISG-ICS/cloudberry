//Load modules
var webdriver = require('selenium-webdriver');

//Internet Browser Drivers
var chrome = new webdriver.Builder().forBrowser('chrome').build();
var firefox = new webdriver.Builder().forBrowser('firefox').build();

var drivers = [ chrome, firefox];
var names = ['Chrome', 'FireFox'];

//Execute Tests
for (i = 0; i < drivers.length; i++){
    console.log('Now Testing for %s \n', names[i]);
	drivers[i].get('http://cloudberry.ics.uci.edu');
	Title_Assert();
	Reset_Button_Assert();
    console.log(' ');

}




//Test Functions
function Title_Assert() {
	var title = drivers[i].getTitle().then( function(title) {
		if(title != "Cloudberry")
		{
			console.log("************************");
			console.log("Title Check......Failed ");
			console.log("************************");
		}
		else
			console.log("Title Check......Cleared");
	});

}

function Reset_Button_Assert() {
    var zoomLevel = drivers[i].findElement(webdriver.By.tagName('map')).getAttribute("zoom").then( function(zoomlevel){
	    if(zoomlevel != 4)
	    {
	    	console.log("****************************************");
	    	console.log('Reset Button ZoomLevel Check......Failed');
	    	console.log("****************************************");
	    }
	    else
	    	console.log('Reset Button Zoomlevel Check......Cleared');
		});

    var latCoordinate = drivers[i].findElement(webdriver.By.tagName('map')).getAttribute("lat").then( function(latCoordinate){
	    if(latCoordinate != 39.5)
	    {
	    	console.log("**************************************************");
	    	console.log('Reset Button Latitude Coordinate Check......Failed');
	    	console.log("**************************************************");
	 	}
	    else
	    	console.log('Reset Button Latitude Coordinate Check......Cleared');
	});

    var lngCoordinate = drivers[i].findElement(webdriver.By.tagName('map')).getAttribute("lng").then( function(lngCoordinate){
    if(lngCoordinate != -96.35)
    {
    	console.log("**************************************************");
    	console.log('Reset Button Longitude Coordinate Check......Failed');
    	console.log("**************************************************");
    }
    else
    	console.log('Reset Button Longitude Coordinate Check......Cleared');
	});
}
