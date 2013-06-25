package hello;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Application {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:hello/spring-integration-context.xml");
    }

}
