This guide walks you through the process of using Spring Integration to create a simple application that retrieves data from Twitter, manipulates the data, and then writes it to a file.

What you'll build
-----------------

You'll create a flow using Spring Integration.

What you'll need
----------------

 - About 15 minutes
 - A favorite text editor or IDE
 - [JDK 6][jdk] or later
 - [Gradle 1.7+][gradle] or [Maven 3.0+][mvn]
 - You can also import the code from this guide as well as view the web page directly into [Spring Tool Suite (STS)][gs-sts] and work your way through it from there.

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[gradle]: http://www.gradle.org/
[mvn]: http://maven.apache.org/download.cgi
[gs-sts]: /guides/gs/sts

How to complete this guide
--------------------------

Like all Spring's [Getting Started guides](/guides/gs), you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To **start from scratch**, move on to [Set up the project](#scratch).

To **skip the basics**, do the following:

 - [Download][zip] and unzip the source repository for this guide, or clone it using [Git][u-git]:
`git clone https://github.com/spring-guides/gs-integration.git`
 - cd into `gs-integration/initial`.
 - Jump ahead to [Define an integration plan](#initial).

**When you're finished**, you can check your results against the code in `gs-integration/complete`.
[zip]: https://github.com/spring-guides/gs-integration/archive/master.zip
[u-git]: /understanding/Git

<a name="scratch"></a>
Set up the project
------------------
First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Gradle](http://gradle.org) and [Maven](https://maven.apache.org) is included here. If you're not familiar with either, refer to [Building Java Projects with Gradle](/guides/gs/gradle/) or [Building Java Projects with Maven](/guides/gs/maven).

### Create the directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

    └── src
        └── main
            └── java
                └── hello


### Create a Gradle build file
Below is the [initial Gradle build file](https://github.com/spring-guides/gs-integration/blob/master/initial/build.gradle). But you can also use Maven. The pom.xml file is included [right here](https://github.com/spring-guides/gs-integration/blob/master/initial/pom.xml). If you are using [Spring Tool Suite (STS)][gs-sts], you can import the guide directly.

`build.gradle`
```gradle
buildscript {
    repositories {
        maven { url "http://repo.springsource.org/libs-snapshot" }
        mavenLocal()
    }
}

apply plugin: 'java'
apply plugin: 'eclipse'
apply plugin: 'idea'

jar {
    baseName = 'gs-integration'
    version =  '0.1.0'
}

repositories {
    mavenCentral()
    maven { url "http://repo.springsource.org/libs-snapshot" }
}

dependencies {
    compile("org.springframework.boot:spring-boot-starter-integration:0.5.0.M2")
    compile("org.springframework.integration:spring-integration-twitter:2.2.4.RELEASE")
    testCompile("junit:junit:4.11")
}

task wrapper(type: Wrapper) {
    gradleVersion = '1.7'
}
```
    
[gs-sts]: /guides/gs/sts    

> **Note:** This guide is using [Spring Boot](/guides/gs/spring-boot/).

<a name="initial"></a>
Define an integration plan
--------------------------

For this guide's sample application, you will define a Spring Integration plan that reads tweets from Twitter, transforms them into an easily readable `String`, and appends that `String` to the end of a file.

To define an integration plan, you simply create a Spring XML configuration with a handful of elements from Spring Integration's XML namespaces. Specifically, for the desired integration plan, you work with elements from these Spring Integration namespaces: core, twitter, and file.

The following XML configuration file defines the integration plan:

`src/main/resources/hello/integration.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:int="http://www.springframework.org/schema/integration"
    xmlns:twitter="http://www.springframework.org/schema/integration/twitter"
    xmlns:file="http://www.springframework.org/schema/integration/file"
    xsi:schemaLocation="http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration.xsd
        http://www.springframework.org/schema/integration/file http://www.springframework.org/schema/integration/file/spring-integration-file-2.2.xsd
        http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/integration/twitter http://www.springframework.org/schema/integration/twitter/spring-integration-twitter-2.2.xsd">

    <twitter:search-inbound-channel-adapter id="tweets" 
            query="#HelloWorld" 
            twitter-template="twitterTemplate">
        <int:poller fixed-rate="5000"/>
    </twitter:search-inbound-channel-adapter>

    <int:transformer 
            input-channel="tweets" 
            expression="payload.fromUser + '  :  ' + payload.text + @newline" 
            output-channel="files"/>

    <file:outbound-channel-adapter id="files"
            mode="APPEND"
            charset="UTF-8"
            directory="/tmp/si"
            filename-generator-expression="'HelloWorld'"/>

</beans>
```

As you can see, three integration elements are in play here:

 * `<twitter:search-inbound-channel-adapter>`. An inbound adapter that searches Twitter for tweets with "#HelloWorld" in the text. It is injected with a `TwitterTemplate` from [Spring Social][SpringSocial] to perform the actual search. As configured here, it polls every 5 seconds. Any matching tweets are placed into a channel named "tweets" (corresponding with the adapter's ID).
 * `<int:transformer>`. Transformed tweets in the "tweets" channel, extracting the tweet's author (`payload.fromUser`) and text (`payload.text`) and concatenating them into a readable `String`. The `String` is then written through the output channel named "files".
 * `<file:outbound-channel-adapter>`. An outbound adapter that writes content from its channel (here named "files") to a file. Specifically, as configured here, it will append anything in the "files" channel to a file at `/tmp/si/HelloWorld`.

This simple flow is illustrated like this:

![A flow plan that reads tweets from Twitter, transforms them to a String, and appends them to a file.](images/tweetToFile.png)

The integration plan references two beans that aren't defined in `integration.xml`: the "twitterTemplate" bean that is injected into the search inbound adapter and the "newline" bean referenced in the transformer. Those beans will be declared separately in JavaConfig as part of the main class of the application.

Make the application executable
-------------------------------

Although it is common to configure a Spring Integration plan within a larger application, perhaps even a web application, there's no reason that it can't be defined in a simpler standalone application. That's what you do next, creating a main class that kicks off the integration plan and also declares a handful of beans to support the integration plan. You also build the application into a standalone executable JAR file.

### Create a main class

`src/main/java/hello/Application.java`
```java
package hello;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

@Configuration
@ImportResource("/hello/integration.xml")
public class Application {

    public static void main(String[] args) {
        new AnnotationConfigApplicationContext(Application.class);
    }
    
    @Bean
    public String newline() {
        return System.getProperty("line.separator");
    }
    
    @Bean
    public Twitter twitterTemplate(OAuth2Operations oauth2) {
        return new TwitterTemplate(oauth2.authenticateClient().getAccessToken());
    }
    
    @Bean
    public OAuth2Operations oauth2Template(Environment env) {
        return new OAuth2Template(env.getProperty("clientId"), env.getProperty("clientSecret"), "", "https://api.twitter.com/oauth2/token");
    }
    
}
```

As you can see, this class provides a `main()` method that loads the Spring application context. It's also annotated as a `@Configuration` class, indicating that it will contain bean definitions.

Specifically, three beans are created in this class:

 * The `newline()` method creates a simple `String` bean containing the underlying system's newline character(s). This is used in the integration plan to place a newline at the end of the transformed tweet `String`.
 * The `twitterTemplate()` method defines a `TwitterTemplate` bean that is injected into the `<twitter:search-inbound-channel-adapter>`.
 * The `oauth2Template()` method defines a Spring Social `OAuth2Template` bean used to obtain a client access token when creating the `TwitterTemplate` bean.

The `oauth2Template()` method references the `Environment` to get "clientId" and "clientSecret" properties. Those properties are ultimately client credentials you are given when you [register your application with Twitter][gs-register-twitter-app]. Fetching them from the `Environment` means you don't have to hardcode them in this configuration class. You'll need them when you [run the application](#run), though.

Finally, notice that `Application` is configured with `@ImportResource` to import the integration plan defined in `/hello/integration.xml`. 

### Build an executable JAR
Now that your `Application` class is ready, you simply instruct the build system to create a single, executable jar containing everything. This makes it easy to ship, version, and deploy the service as an application throughout the development lifecycle, across different environments, and so forth.

Below are the Gradle steps, but if you are using Maven, you can find the updated pom.xml [right here](https://github.com/spring-guides/gs-integration/blob/master/complete/pom.xml) and build it by typing `mvn clean package`.

Update your Gradle `build.gradle` file's `buildscript` section, so that it looks like this:

```groovy
buildscript {
    repositories {
        maven { url "http://repo.springsource.org/libs-snapshot" }
        mavenLocal()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:0.5.0.M2")
    }
}
```

Further down inside `build.gradle`, add the following to the list of applied plugins:

```groovy
apply plugin: 'spring-boot'
```
You can see the final version of `build.gradle` [right here]((https://github.com/spring-guides/gs-integration/blob/master/complete/build.gradle).

The [Spring Boot gradle plugin][spring-boot-gradle-plugin] collects all the jars on the classpath and builds a single "über-jar", which makes it more convenient to execute and transport your service.
It also searches for the `public static void main()` method to flag as a runnable class.

Now run the following command to produce a single executable JAR file containing all necessary dependency classes and resources:

```sh
$ ./gradlew build
```

If you are using Gradle, you can run the JAR by typing:

```sh
$ java -jar build/libs/gs-integration-0.1.0.jar
```

If you are using Maven, you can run the JAR by typing:

```sh
$ java -jar target/gs-integration-0.1.0.jar
```

[spring-boot-gradle-plugin]: https://github.com/SpringSource/spring-boot/tree/master/spring-boot-tools/spring-boot-gradle-plugin

> **Note:** The procedure above will create a runnable JAR. You can also opt to [build a classic WAR file](/guides/gs/convert-jar-to-war/) instead.

Run the application
-----------------------

Now you can run the application from the jar:
```
$ java -DclientId={YOUR CLIENT ID} -DclientSecret={YOUR CLIENT SECRET} -jar build/libs/gs-integration-0.1.0.jar

... app starts up ...
```

Make sure you specify your application's client ID and secret in place of the placeholders shown here.

Once the application starts up, it connects to Twitter and starts fetching tweets that match the search criteria of "#HelloWorld". The application processes those tweets through the integration plan you defined, ultimately appending the tweet's author and text to a file at `/tmp/si/HelloWorld`.

After the application has been running for awhile, you should be able to view the file at `/tmp/si/HelloWorld` to see the data from a handful of tweets. On a UNIX-based operating system, you can also choose to tail the file to see the results as they are written:

    $ tail -f /tmp/si/HelloWorld

You should see something like this (the actual tweets may differ):

```sh
BrittLieTjauw  :  Now that I'm all caught up on the bachelorette I can leave my room #helloworld
mishra_ravish  :  Finally, integrated #eclim. #Android #HelloWorld
NordstrmPetite  :  Pink and fluffy #chihuahua #hahalol #boo #helloworld http://t.co/lelHhFN3gq
GRhoderick  :  Ok Saint Louis, show me what you got. #HelloWorld
```

Summary
-------
Congratulations! You have developed a simple application that uses Spring Integration to fetch tweets from Twitter, process them, and write them to a file. 

[u-war]: /understanding/WAR
[u-tomcat]: /understanding/Tomcat
[u-application-context]: /understanding/application-context
[`SpringApplication`]: http://docs.spring.io/spring-boot/docs/0.5.0.M3/api/org/springframework/boot/SpringApplication.html
[`@Component`]: http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/stereotype/Component.html
[`@EnableAutoConfiguration`]: http://docs.spring.io/spring-boot/docs/0.5.0.M3/api/org/springframework/boot/autoconfigure/EnableAutoConfiguration.html
[`DispatcherServlet`]: http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/DispatcherServlet.html
[SpringSocial]: http://www.springsource.org/spring-social
[gs-register-twitter-app]: /guides/gs/register-twitter-app
