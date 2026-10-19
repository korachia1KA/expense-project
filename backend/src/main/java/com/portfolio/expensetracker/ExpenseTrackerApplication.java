package com.portfolio.expensetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Expense Tracker REST API.
 *
 * <p>{@code @SpringBootApplication} combines three annotations:
 * <ul>
 *   <li>{@code @Configuration} - this class may declare beans;</li>
 *   <li>{@code @EnableAutoConfiguration} - Spring Boot configures things based on the jars it finds
 *       (a DataSource because H2/Postgres is on the classpath, Tomcat because of spring-web, ...);</li>
 *   <li>{@code @ComponentScan} - every {@code @Component}, {@code @Service}, {@code @RestController},
 *       {@code @Configuration} in this package and its sub-packages is discovered and created.</li>
 * </ul>
 * That is why all other classes live in sub-packages of {@code com.portfolio.expensetracker}.
 *
 * <p>Code is organised <b>by feature</b> (auth, category, transaction, budget, summary) rather than
 * by layer: everything about budgets lives in the {@code budget} package. Inside each feature the
 * classic layers appear: Controller (HTTP) → Service (business rules) → Repository (database).
 */
@SpringBootApplication
public class ExpenseTrackerApplication {

    /** Starts the embedded Tomcat server and the Spring application context. */
    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }
}
