## Deploy application on the server

1. Add a application secret to the project. You can do it by adding a property in the **application.conf**

   Example:

   ```scala
   play.http.secret.key="changeme"
   ```

2. Open the host filter privilege to allow alternative host access by adding a property in the **application.conf** 

   This command allow access from any host name:

   ```scala
   play.filters.hosts {
       allowed = ["."]
   }
   ```

3. Add the [sbt-native-packager](http://sbt-native-packager.readthedocs.io/) plugin to the project. You can do it by adding a single line of code to the **plugins.sbt** file.

   ```scala
   addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")
   ```

4. Add the following line of code to the **build.sbt** file as well:

   ```scala
   enablePlugins(JavaAppPackaging)
   ```

5. Run the following command in the root directory of the project

   ```sh
   sbt stage
   ```

6. This command generates a new folder in your project directory: *target/universal/stage/*

   There are two folders in the **stage** directory: **bin** and **lib**. The first one contains launchers (Linux/Mac/Windows). The second one contains all dependencies and a JAR with the application classes.

   The content of the **stage** directory can be moved to your server and launched there by executing the following command in the console:

   ```sh
   ./bin/app-name -Dplay.http.secret.key='changeme'
   ```

   Notice that this command must be run from the **stage** folder. By the way, app-name may vary. It depends on the project name (package name) that you specified in the **build.sbt** file.

7. If an alternative http port is required for the server, the command should include the port assignment part.

   ```sh
   ./bin/app-name -Dhttp.port=port_required -Dplay.http.secret.key='changeme'
   ```

8. Application process can be run as daemon.

   ```sh
   nohup ./bin/app-name -Dhttp.port=port_required -Dplay.http.secret.key='changeme' &
   ```



Reference:

[1]. <https://dzone.com/articles/building-and-deploying-scala-apps>

[2]. <https://www.playframework.com/documentation/2.7.x/ApplicationSecret>