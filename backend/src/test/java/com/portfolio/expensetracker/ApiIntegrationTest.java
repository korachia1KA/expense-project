package com.portfolio.expensetracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION test: boots the WHOLE application (security filter chain, controllers, services,
 * JPA/Hibernate, Flyway migrations, in-memory H2) and sends real HTTP requests through MockMvc.
 *
 * <p>Unit tests prove the rules in isolation; this proves the layers are wired together correctly:
 * JSON parsing, {@code @Valid}, JWT authentication, SQL aggregate queries actually running on a
 * database, error-to-status mapping and ownership scoping.
 *
 * <p>{@code app.seed-demo-data=false} → no demo account; each test registers fresh users with random
 * emails, so tests don't interfere with each other even though they share one database.
 */
@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    // ------------------------------------------------------------------ helpers

    /** Registers a brand-new user and returns their JWT. */
    private String registerAndGetToken() throws Exception {
        String body = """
                {"name": "Jane Doe", "email": "user-%s@example.com", "password": "supersecret"}
                """.formatted(UUID.randomUUID());
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    /** Adds the Authorization header (and JSON content type) to a request. */
    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    /** Looks up the id of one of the user's categories by name (e.g. a default category). */
    private long categoryId(String token, String name) throws Exception {
        String body = mvc.perform(auth(get("/api/categories"), token)).andReturn().getResponse().getContentAsString();
        for (JsonNode c : json.readTree(body)) {
            if (c.get("name").asText().equals(name)) {
                return c.get("id").asLong();
            }
        }
        throw new AssertionError("No category " + name);
    }

    /** Creates a transaction and returns its id. */
    private long createTransaction(String token, String type, String amount, String description, String date,
                                   long categoryId) throws Exception {
        String body = """
                {"type": "%s", "amount": %s, "description": "%s", "date": "%s", "categoryId": %d}
                """.formatted(type, amount, description, date, categoryId);
        String response = mvc.perform(auth(post("/api/transactions"), token).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    // ------------------------------------------------------------------ auth

    @Test
    void protectedEndpoints_requireAValidToken() throws Exception {
        mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/summary").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_createsTheDefaultCategories() throws Exception {
        String token = registerAndGetToken();
        mvc.perform(auth(get("/api/categories"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[?(@.name == 'Salary')].type").value(contains("INCOME")))
                .andExpect(jsonPath("$[?(@.name == 'Food')].color").value(contains("#f97316")));
    }

    @Test
    void login_withWrongPassword_returns401_andDuplicateEmail_returns409() throws Exception {
        String body = "{\"name\":\"A\",\"email\":\"dupe@example.com\",\"password\":\"password1\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dupe@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ------------------------------------------------------------------ transactions

    @Test
    void transactions_fullCrud_withFiltersSortingAndPagination() throws Exception {
        String token = registerAndGetToken();
        long food = categoryId(token, "Food");
        long salary = categoryId(token, "Salary");

        long coffee = createTransaction(token, "EXPENSE", "4.50", "Coffee", "2026-09-03", food);
        createTransaction(token, "EXPENSE", "60.25", "Groceries", "2026-09-10", food);
        createTransaction(token, "INCOME", "3000", "Salary September", "2026-09-01", salary);
        createTransaction(token, "EXPENSE", "12.00", "Coffee beans", "2026-08-20", food);

        // Month filter + default sort (date desc)
        mvc.perform(auth(get("/api/transactions").param("month", "2026-09"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].description").value("Groceries"))
                .andExpect(jsonPath("$.content[0].category.name").value("Food"));

        // Type + text search (case-insensitive)
        mvc.perform(auth(get("/api/transactions").param("type", "EXPENSE").param("q", "COFFEE"), token))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Category filter, sorting by amount ascending, page size 1
        mvc.perform(auth(get("/api/transactions").param("categoryId", String.valueOf(food))
                        .param("sort", "amount,asc").param("size", "1"), token))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].amount").value(4.5));

        // UPDATE
        mvc.perform(auth(put("/api/transactions/{id}", coffee), token).content("""
                        {"type":"EXPENSE","amount":5.75,"description":"Flat white","date":"2026-09-03",
                         "categoryId":%d,"notes":"with oat milk"}""".formatted(food)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(5.75))
                .andExpect(jsonPath("$.notes").value("with oat milk"));

        // DELETE, then it's gone
        mvc.perform(auth(delete("/api/transactions/{id}", coffee), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/transactions/{id}", coffee), token)).andExpect(status().isNotFound());

        // Unknown sort property → 400 instead of a 500
        mvc.perform(auth(get("/api/transactions").param("sort", "hacker,asc"), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTransaction_returnsFieldErrors() throws Exception {
        String token = registerAndGetToken();
        mvc.perform(auth(post("/api/transactions"), token).content("""
                        {"type":"EXPENSE","amount":0,"description":"","date":"2999-01-01","categoryId":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.amount").value("must be greater than 0"))
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.fieldErrors.date").value("cannot be in the future"));
    }

    @Test
    void transactionTypeMustMatchCategoryType() throws Exception {
        String token = registerAndGetToken();
        long salary = categoryId(token, "Salary");
        mvc.perform(auth(post("/api/transactions"), token).content("""
                        {"type":"EXPENSE","amount":10,"description":"Oops","date":"2026-09-01","categoryId":%d}"""
                        .formatted(salary)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.categoryId", containsString("income category")));
    }

    // ------------------------------------------------------------------ ownership

    @Test
    void usersCannotSeeOrTouchEachOthersData() throws Exception {
        String alice = registerAndGetToken();
        String bob = registerAndGetToken();
        long aliceFood = categoryId(alice, "Food");
        long txId = createTransaction(alice, "EXPENSE", "99.99", "Secret purchase", "2026-09-01", aliceFood);

        // 404 (not 403) so Bob can't even tell the records exist.
        mvc.perform(auth(get("/api/transactions/{id}", txId), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/transactions/{id}", txId), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/categories/{id}", aliceFood), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/categories/{id}", aliceFood), bob)).andExpect(status().isNotFound());
        // Bob can't file a transaction under Alice's category or budget it either.
        mvc.perform(auth(post("/api/transactions"), bob).content("""
                        {"type":"EXPENSE","amount":1,"description":"x","date":"2026-09-01","categoryId":%d}"""
                        .formatted(aliceFood)))
                .andExpect(status().isNotFound());
        mvc.perform(auth(put("/api/budgets"), bob).content("""
                        {"categoryId":%d,"month":"2026-09","limit":10}""".formatted(aliceFood)))
                .andExpect(status().isNotFound());
        // Lists and summaries only contain Bob's (empty) data.
        mvc.perform(auth(get("/api/transactions"), bob)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(auth(get("/api/summary").param("month", "2026-09"), bob))
                .andExpect(jsonPath("$.totalExpense").value(0));
        // Alice still has her transaction.
        mvc.perform(auth(get("/api/transactions/{id}", txId), alice)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ categories

    @Test
    void deletingACategoryInUse_returns409_butUnusedOneCanBeDeleted() throws Exception {
        String token = registerAndGetToken();
        long food = categoryId(token, "Food");
        createTransaction(token, "EXPENSE", "20", "Pizza", "2026-09-05", food);

        mvc.perform(auth(delete("/api/categories/{id}", food), token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("used by 1 transaction")));

        long other = categoryId(token, "Other");
        mvc.perform(auth(delete("/api/categories/{id}", other), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/categories"), token)).andExpect(jsonPath("$", hasSize(8)));
    }

    @Test
    void categoryNamesAreUniquePerUser_andColorIsValidated() throws Exception {
        String token = registerAndGetToken();
        mvc.perform(auth(post("/api/categories"), token).content("""
                        {"name":"food","color":"#000000","type":"EXPENSE"}"""))
                .andExpect(status().isConflict());
        mvc.perform(auth(post("/api/categories"), token).content("""
                        {"name":"Pets","color":"red","type":"EXPENSE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.color").exists());
        mvc.perform(auth(post("/api/categories"), token).content("""
                        {"name":"Pets","color":"#A16207","type":"EXPENSE"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.color").value("#a16207"))
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    // ------------------------------------------------------------------ budgets + summary

    @Test
    void budgetsAndSummary_computeTheRightNumbers() throws Exception {
        String token = registerAndGetToken();
        long food = categoryId(token, "Food");
        long fun = categoryId(token, "Entertainment");
        long salary = categoryId(token, "Salary");

        createTransaction(token, "INCOME", "2000.00", "Salary", "2026-09-01", salary);
        createTransaction(token, "EXPENSE", "150.00", "Groceries", "2026-09-02", food);
        createTransaction(token, "EXPENSE", "50.00", "Restaurant", "2026-09-02", food);
        createTransaction(token, "EXPENSE", "90.00", "Concert", "2026-09-20", fun);
        createTransaction(token, "EXPENSE", "10.00", "August snack", "2026-08-31", food);  // other month

        // Upsert twice: the second call UPDATES the same budget (same id), it doesn't create another.
        String first = mvc.perform(auth(put("/api/budgets"), token).content("""
                        {"categoryId":%d,"month":"2026-09","limit":300}""".formatted(food)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long budgetId = json.readTree(first).get("id").asLong();
        mvc.perform(auth(put("/api/budgets"), token).content("""
                        {"categoryId":%d,"month":"2026-09","limit":250}""".formatted(food)))
                .andExpect(jsonPath("$.id").value(budgetId))
                .andExpect(jsonPath("$.spent").value(200.0))
                .andExpect(jsonPath("$.percentUsed").value(80.0));
        mvc.perform(auth(put("/api/budgets"), token).content("""
                        {"categoryId":%d,"month":"2026-09","limit":80}""".formatted(fun)))
                .andExpect(jsonPath("$.percentUsed").value(112.5))
                .andExpect(jsonPath("$.remaining").value(-10.0));
        // Budgets on income categories are rejected.
        mvc.perform(auth(put("/api/budgets"), token).content("""
                        {"categoryId":%d,"month":"2026-09","limit":80}""".formatted(salary)))
                .andExpect(status().isBadRequest());

        mvc.perform(auth(get("/api/budgets").param("month", "2026-09"), token))
                .andExpect(jsonPath("$", hasSize(2)));

        // Summary: 2000 income, 290 expense, balance 1710, savings 85.5 %
        mvc.perform(auth(get("/api/summary").param("month", "2026-09"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-09"))
                .andExpect(jsonPath("$.totalIncome").value(2000.0))
                .andExpect(jsonPath("$.totalExpense").value(290.0))
                .andExpect(jsonPath("$.balance").value(1710.0))
                .andExpect(jsonPath("$.savingsRate").value(85.5))
                .andExpect(jsonPath("$.spendingByCategory", hasSize(2)))
                .andExpect(jsonPath("$.spendingByCategory[0].name").value("Food"))
                .andExpect(jsonPath("$.spendingByCategory[0].budget").value(250.0))
                .andExpect(jsonPath("$.dailySpending", hasSize(30)))
                .andExpect(jsonPath("$.dailySpending[1].amount").value(200.0))
                .andExpect(jsonPath("$.dailySpending[0].amount").value(0))
                .andExpect(jsonPath("$.trend", hasSize(6)))
                .andExpect(jsonPath("$.trend[4].month").value("2026-08"))
                .andExpect(jsonPath("$.trend[4].expense").value(10.0))
                .andExpect(jsonPath("$.trend[5].income").value(2000.0));

        // Delete a budget
        mvc.perform(auth(delete("/api/budgets/{id}", budgetId), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/budgets").param("month", "2026-09"), token))
                .andExpect(jsonPath("$", hasSize(1)));

        // Malformed month → 400
        mvc.perform(auth(get("/api/summary").param("month", "September"), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.month").exists());
    }
}
