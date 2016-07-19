## Scrapy Project: promedmail

Run the setupScrapy.sh **only once** to setup the scrapy-splash environment by:`./setupScrapy.sh`

Go to the scrapy directory: `/cloudberry/src/main/resources/crawler/scripts/scrapy`

Start new scrapy project by using command: `$ scrapy startproject project_name`

After running this command we get the the directory structure provided by scrapy.

Go to the working directory of the project

Run the docker by running:
`$ docker run -p 8050:8050 scrapinghub/splash`

If the port is already in use, change the port number.
Example:
`$ docker run -p 19001:8050 scrapinghub/splash`

Check the docker container Id using the command:
`$docker ps`

Find your docker's ip address by:
`$docker inspect --format '{{ .NetworkSettings.IPAddress }}' docker_container_id`

Append the contents of SplashConfig.txt in the directory: 
`cloudberry/noah/src/main/resources/crawler` 
to the settings.py file in the scrapy project directory.
You can make a shell script named `move.sh` containing following script:
```
DESTN=/path/destination_file.extension
SOURCE=/path/source_file.extension
cat "$SOURCE" >> "$DESTN"
```

Also add the following text to `settings.py`:

`SPLASH_URL = 'http://_docker's Ip address_:8050'`

use the command:
```
cat >> settings.py
`SPLASH_URL = 'http://_docker's Ip address_:8050'`
^D(ctrl+d)
```
Here `8050` is the port number. It can be `19001` if you used that as your port number or it may be something else.

Go to spiders directory inside your scrapy project directory.
Copy `promedmail_spider.py` from the directory `cloudberry/noah/src/main/resources/crawler` to your current directory using the `cp` command.

Copy `promedconf.conf` and `runPromed.sh` files from the directory `cloudberry/noah/src/main/resources/crawler` to the scrapy project directory.

Run runPromed.sh by using command:
`./runPromed.sh`