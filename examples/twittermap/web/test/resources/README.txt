Must download selenium-webdriver on Node.js Command Prompt by entering

npm install selenium-webdriver 

For each browser you are testing, you will be required to download a driver and 
add it to Enviornmental Variables PATH. The current browsers currently being tested
are Chrome and Firefox. Please make sure to download the latest version of the driver
and also have the browser itself installed on your computer.

Chrome:
http://chromedriver.storage.googleapis.com/index.html

Firefox:
https://github.com/mozilla/geckodriver/releases/

For more browser drivers or fully detailed instructions on setting up, visit 
https://www.npmjs.com/package/selenium-webdriver

API Doc: http://seleniumhq.github.io/selenium/docs/api/javascript/index.html

***********************************************************************************************
To run test:

On Node.js Command Prompt change directory to neo\test\resources\
then run

node front-end.js

Browsers will open and test will begin. Please wait for test to be completed.

If you encounter any errors, it is most likely that you are missing a module file.
Simply return to Node.js Command Prompt and install by running

npm install (missing module)

***********************************************************************************************
