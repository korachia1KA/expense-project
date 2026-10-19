package com.portfolio.expensetracker.config;

import com.portfolio.expensetracker.budget.Budget;
import com.portfolio.expensetracker.budget.BudgetRepository;
import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.category.CategoryRepository;
import com.portfolio.expensetracker.category.CategoryService;
import com.portfolio.expensetracker.transaction.Transaction;
import com.portfolio.expensetracker.transaction.TransactionRepository;
import com.portfolio.expensetracker.transaction.TransactionType;
import com.portfolio.expensetracker.user.User;
import com.portfolio.expensetracker.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates a demo account (demo@example.com / password123) with about three months of realistic
 * transactions and a few budgets, so anyone reviewing the project sees a populated dashboard
 * immediately instead of empty charts.
 *
 * <p>{@link CommandLineRunner}: Spring calls {@link #run} once, right after the application has
 * started. {@code @ConditionalOnProperty} means this bean only EXISTS when
 * {@code app.seed-demo-data=true}; tests set it to false so they start with an empty database.
 *
 * <p>Data is generated relative to "today" (from the injected Clock), so the demo always shows the
 * current month and the two before it. A {@link Random} with a FIXED seed produces the same
 * "random" amounts on every start, which keeps the demo reproducible.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_EMAIL = "demo@example.com";
    public static final String DEMO_PASSWORD = "password123";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final CategoryService categoryService;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final Random random = new Random(42);
    private LocalDate today;
    private int count;

    public DemoDataSeeder(UserRepository users, CategoryService categoryService, CategoryRepository categories,
                          TransactionRepository transactions, BudgetRepository budgets,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.categoryService = categoryService;
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotent: with a persistent database (Docker/Postgres) the app restarts often; seed only once.
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        User demo = users.save(new User("Demo User", DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD)));
        categoryService.createDefaults(demo);
        // name → Category, for readable code below.
        Map<String, Category> cat = categories.findByUserIdOrderByTypeAscNameAsc(demo.getId()).stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));

        today = LocalDate.now(clock);
        YearMonth current = YearMonth.from(today);
        for (int back = 2; back >= 0; back--) {
            seedMonth(demo, cat, current.minusMonths(back));
        }

        // Budgets for this month and last month. Entertainment this month is deliberately exceeded.
        for (YearMonth m : new YearMonth[]{current.minusMonths(1), current}) {
            budget(demo, cat.get("Food"), m, "450.00");
            budget(demo, cat.get("Transport"), m, "150.00");
            budget(demo, cat.get("Entertainment"), m, "80.00");
            budget(demo, cat.get("Shopping"), m, "200.00");
        }

        log.info("Seeded demo account {} / {} with {} transactions", DEMO_EMAIL, DEMO_PASSWORD, count);
    }

    /** A typical month: salary, rent, bills, weekly groceries, commuting and some fun. */
    private void seedMonth(User u, Map<String, Category> c, YearMonth m) {
        add(u, c.get("Salary"), m, 1, "Monthly salary", "3200.00", 0);
        add(u, c.get("Housing"), m, 1, "Rent", "1150.00", 0);
        add(u, c.get("Utilities"), m, 5, "Electricity bill", "68.00", 0.25);
        add(u, c.get("Utilities"), m, 8, "Internet", "45.00", 0);
        add(u, c.get("Utilities"), m, 12, "Phone plan", "25.00", 0);
        add(u, c.get("Health"), m, 3, "Gym membership", "35.00", 0);

        // Groceries every week, coffee/lunches a few times a week.
        for (int day = 2; day <= 30; day += 7) {
            add(u, c.get("Food"), m, day, "Groceries", "78.00", 0.3);
        }
        for (int day = 4; day <= 28; day += 6) {
            add(u, c.get("Food"), m, day, day % 2 == 0 ? "Lunch with colleagues" : "Coffee & pastry", "14.00", 0.5);
        }
        // Commuting.
        add(u, c.get("Transport"), m, 1, "Monthly transit pass", "75.00", 0);
        add(u, c.get("Transport"), m, 9, "Fuel", "48.00", 0.2);
        add(u, c.get("Transport"), m, 23, "Ride share", "18.00", 0.4);

        // Fun and shopping.
        add(u, c.get("Entertainment"), m, 6, "Streaming subscriptions", "22.00", 0);
        add(u, c.get("Entertainment"), m, 17, "Cinema", "28.00", 0.2);
        add(u, c.get("Shopping"), m, 14, "Clothes", "65.00", 0.5);
        add(u, c.get("Shopping"), m, 26, "Household items", "32.00", 0.4);
        add(u, c.get("Other"), m, 20, "Gift for a friend", "40.00", 0.3);

        boolean isCurrent = m.equals(YearMonth.from(today));
        if (isCurrent) {
            // Guarantee the current month is over the Entertainment budget (80), whatever today's date is:
            // the concert is clamped to today so it always exists.
            addOn(u, c.get("Entertainment"), today.withDayOfMonth(Math.min(2, today.getDayOfMonth())),
                    "Concert tickets", new BigDecimal("95.00"));
        } else {
            add(u, c.get("Health"), m, 19, "Pharmacy", "23.00", 0.3);
            add(u, c.get("Salary"), m, 15, "Freelance website project", "450.00", 0.2);
        }
    }

    /**
     * Adds one transaction on day {@code day} of month {@code m}, skipping dates after today
     * (no future transactions!) and days that don't exist in short months.
     *
     * @param variation e.g. 0.3 → amount varies randomly ±30% around {@code base}
     */
    private void add(User u, Category c, YearMonth m, int day, String description, String base, double variation) {
        if (day > m.lengthOfMonth()) {
            return;
        }
        LocalDate date = m.atDay(day);
        if (date.isAfter(today)) {
            return;
        }
        double factor = 1 + (random.nextDouble() * 2 - 1) * variation;
        BigDecimal amount = new BigDecimal(base).multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        addOn(u, c, date, description, amount);
    }

    private void addOn(User u, Category c, LocalDate date, String description, BigDecimal amount) {
        Transaction t = new Transaction(u);
        t.setCategory(c);
        t.setType(c.getType());           // always consistent with the category
        t.setAmount(amount);
        t.setDescription(description);
        t.setDate(date);
        transactions.save(t);
        count++;
    }

    private void budget(User u, Category c, YearMonth m, String limit) {
        Budget b = new Budget(u, c, m.toString());
        b.setLimitAmount(new BigDecimal(limit));
        budgets.save(b);
    }
}
