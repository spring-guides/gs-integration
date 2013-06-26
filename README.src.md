# Getting Started: Data Integration

What you'll build
-----------------

This guide walks you through creating a simple application that retrieves data from Twitter, manipulates the data, then writes it to a file.

What you'll need
----------------

- About 15 minutes
- {!include#prereq-editor-jdk-buildtools}

## {!include#how-to-complete-this-guide}

<a name="scratch"></a>
Set up the project
------------------
{!include#build-system-intro}

{!include#create-directory-structure-hello}

### Create a Maven POM

    {!include:initial/pom.xml}

{!include#bootstrap-starter-pom-disclaimer}

<a name="initial"></a>
Define an Integration Plan
--------------------------

For this guide's sample application, you're going to define a Spring Integration plan that reads tweets from Twitter, transforms them into an easily readable `String`, and appends that `String` to the end of a file.

To define an integration plan, you simply create a Spring XML configuration using a handful of elements from Spring Integration's XML namespaces. Specifically, for the desired integration plan, you'll need to work with elements from 3 of Spring Integration's namespaces: core, twitter, and file.

The following XML configuration file defines the integration plan:

    {!include:complete/src/main/resources/hello/integration.xml}

As you can see, there are three integration elements in play here:

 * `<twitter:search-inbound-channel-adapter>` - An inbound adapter that searches Twitter for tweets with "#HelloWorld" in the text. It is injected with a `TwitterTemplate` from [Spring Social][SpringSocial] to perform the actual search. As configured here, it polls every 5 seconds. Any matching tweets are placed into a channel named "tweets" (corresponding with the adapter's ID).
 * `<int:transformer>` - Transformed tweets in the "tweets" channel, extracting the tweet's author (`payload.fromUser`) and text (`payload.text`) and concatenating them into a readable `String`. The `String` is then written through the output channel named "files".
 * `<file:outbound-channel-adapter>` - An outbound adapter that writes content from its channel (here named "files") to a file. Specifically, as configured here, it will append anything in the "files" channel to a file at `/tmp/si/HelloWorld`.

This simple flow is illustrated like this:

![A flow plan that reads tweets from Twitter, transforms them to a String, and appends them to a file.](images/tweetToFile.png)

Notice that the integration plan references a couple of beans that aren't defined in `integration.xml`. Specifically, the "twitterTemplate" bean that is injected into the search inbound adapter and the "newline" bean referenced in the transformer are not defined here. Those beans will be declared separately in JavaConfig as part of the main class of the application.

Make the application executable
-------------------------------

Although it is common to configure a Spring Integration plan within a larger application, perhaps even a web applicaion, there's no reason that it can't be defined in a simpler standalone application. That's what you'll do next, creating a main class that kicks off the integration plan and also declares a handful of beans to support the integration plan. You'll also build the application into a standalone executable JAR file.

### Create a main class
    {!include:complete/src/main/java/hello/Application.java}

As you can see, this class provies a `main()` method that loads the Spring application context. It's also annotated as a `@Configuration` class, indicating that it will contain some bean definitions.

Specifically, there are three beans created in this class:

 * The `newline()` method creates a simple `String` bean containing the underlying system's newline character(s). This is used in the integration plan to place a newline at the end of the transformed tweet `String`.
 * The `twitterTemplate()` method defines a `TwitterTemplate` bean that is injected into the `<twitter:search-inbound-channel-adapter>`.
 * The `oauth2Template()` method defines a Spring Social `OAuth2Template` bean used to obtain a client access token when creating the `TwitterTemplate` bean.

Note that the `oauth2Template()` method references the `Environment` to get "clientId" and "clientSecret" properties. Those properties are ultimately client credentials you are given when you [register your application with Twitter][register-twitter-app]. By fetching them from the `Environment`, it prevents you from having to hardcode them in this configuration class. You'll need them when you [run the application](#run), though.

Finally, notice that `Application` is configured with `@ImportResource` to import the integration plan defined in `/hello/integration.xml`. 

### {!include#build-an-executable-jar}

<a name="run"></a>
Running the Application
-----------------------

Now you can run the application from the jar:
```
$ java -DclientId={YOUR CLIENT ID} -DclientSecret={YOUR CLIENT SECRET} -jar target/gs-integration-complete-0.1.0.jar

... app starts up ...
```

Note that you'll need to be sure to specify your application's client ID and secret in place of the placeholders shown here.

Once the application starts up, it will connect to Twitter and start fetching tweets that match the search criteria of "#HelloWorld". It will process those tweets through the integration plan you defined, ultimately appending the tweet's author and text to a file at `/tmp/si/HelloWorld`.

After the application has been running for awhile, you should be able to view the file at `/tmp/si/HelloWorld` to see the data from a handful of tweets. On a UNIX-based operating system, you also choose to tail the file to see the results as they are written:

```sh
$ tail -f /tmp/si/HelloWorld
```

You should see something like this (the actual tweets may differ):

```sh
BrittLieTjauw  :  Now that I'm all caught up on the bachelorette I can leave my room #helloworld
mishra_ravish  :  Finally, integrated #eclim. #Android #HelloWorld
NordstrmPetite  :  Pink and fluffy #chihuahua #hahalol #boo #helloworld http://t.co/lelHhFN3gq
GRhoderick  :  Ok Saint Louis, show me what you got. #HelloWorld
```

Summary
-------
Congratulations! You have just developed a simple application that uses Spring Integration to fetch tweets from Twitter, process them, and write them to a file. There's a lot more that Spring Integration can do

[zip]: https://github.com/springframework-meta/gs-accessing-facebook/archive/master.zip
[u-war]: /understanding/war
[u-tomcat]: /understanding/tomcat
[u-application-context]: /understanding/application-context
[`SpringApplication`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.BUILD-SNAPSHOT/javadoc-api/org/springframework/bootstrap/SpringApplication.html
[`@Component`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/stereotype/Component.html
[`@EnableAutoConfiguration`]: http://static.springsource.org/spring-bootstrap/docs/0.5.0.BUILD-SNAPSHOT/javadoc-api/org/springframework/bootstrap/context/annotation/SpringApplication.html
[`DispatcherServlet`]: http://static.springsource.org/spring/docs/current/javadoc-api/org/springframework/web/servlet/DispatcherServlet.html
[SpringSocial]: http://www.springsource.org/spring-social
[register-twitter-app]: /gs-register-twitter-app/README.md
