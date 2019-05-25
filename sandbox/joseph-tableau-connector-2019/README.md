Tableau Evaluation Issue: https://github.com/ISG-ICS/cloudberry/issues/688

The logic of my code is described in this GitHub Issue


To use run these files:

1. Download Tableau Desktop (Free Trial Version) (https://www.tableau.com/products/desktop) or Tableau Public (Free Version) (https://public.tableau.com/en-us/s/)

2. Create a file to store the data used by Tableau WDC (I used the Python script to get data from the local MySQL database and store them in movies.txt and stars.txt in JSON format). (Note: Tableau WDC does not make a connection to the database. It only retrieves extracted data from a file (JSON, CSV, etc.))

3. Modify the JavaScript files according to your data (My two JavaScript files are made specifically for movies.txt and stars.txt respectively)

3. Deploy the HTML files somewhere on the web (I used GitHub pages)

4. Open Tableau Desktop or Tableau Public, choose the data source as Web Data Connector, enter the URL to the HTML page, and you are all set!


References: https://tableau.github.io/webdataconnector/docs/wdc_tutorial (A detailed tutorial of how to build a Tableau WDC can be found here)


Relevant files:

Group 1: movies.txt, movieWDC.js, movieWDC.html
Group 2: stars.txt, starWDC.js, starWDC.html

Optional Python Generators (getting data from MySQL and outputting them in JSON format): json_generator.py
