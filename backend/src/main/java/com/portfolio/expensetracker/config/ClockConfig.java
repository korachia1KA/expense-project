package com.portfolio.expensetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the {@link Clock} used everywhere the code needs "today" or "this month".
 *
 * <p><b>Why inject a Clock instead of calling {@code LocalDate.now()}?</b> Code that reads the
 * system time directly gives different results every day, which makes tests flaky. Services ask for a
 * Clock in their constructor and call {@code LocalDate.now(clock)}. Production gets the real system
 * clock (below); unit tests pass {@code Clock.fixed(...)} so "today" is always the same date.
 */
@Configuration
public class ClockConfig {

    /** {@code @Bean}: the returned object is registered in Spring and injected into any constructor asking for a Clock. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
