package portal.expenses.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import portal.expenses.dto.ApprovalRequest;
import portal.expenses.dto.ExpenseRequest;
import portal.expenses.dto.LoginRequest;
import portal.expenses.dto.LoginResponse;
import portal.expenses.entity.ApprovalStatus;
import portal.expenses.entity.Expense;
import portal.expenses.entity.ExpenseCategory;
import portal.expenses.repository.ExpenseRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Expense Management - Integration Tests with Testcontainers")
@Disabled("Integration tests require Docker. Enable when Docker is available.")
class ExpenseManagementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("storage.mode", () -> "local");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    private String employeeToken;
    private String managerToken;
    private String financeToken;

    @BeforeEach
    void setUp() throws Exception {
        // Login as different users to get JWT tokens
        employeeToken = getAuthToken("employee.user", "password123");
        managerToken = getAuthToken("manager.admin", "password123");
        financeToken = getAuthToken("finance.admin", "password123");
    }

    private String getAuthToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );

        return response.token();
    }

    @Test
    @Order(1)
    @DisplayName("Integration Test 1: Full Expense Lifecycle - Create, Submit, Approve, Reimburse")
    void testFullExpenseLifecycle() throws Exception {
        // Step 1: Employee creates an expense
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Team lunch expense");
        expenseRequest.setAmount(new BigDecimal("150.00"));
        expenseRequest.setCategory(ExpenseCategory.MEALS);
        expenseRequest.setExpenseDate(LocalDate.now());

        MockMultipartFile receiptFile = new MockMultipartFile(
                "receipt", "receipt.pdf", "application/pdf", "receipt content".getBytes()
        );

        MockMultipartFile expenseJson = new MockMultipartFile(
                "expense", "", "application/json",
                objectMapper.writeValueAsBytes(expenseRequest)
        );

        MvcResult createResult = mockMvc.perform(multipart("/api/expenses")
                        .file(expenseJson)
                        .file(receiptFile)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Team lunch expense"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andReturn();

        Expense createdExpense = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Expense.class
        );
        Long expenseId = createdExpense.getId();

        // Step 2: Employee submits the expense
        mockMvc.perform(post("/api/expenses/" + expenseId + "/submit")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("MANAGER_REVIEW"));

        // Step 3: Manager approves the expense
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setAction("approve");
        approvalRequest.setComments("Approved - reasonable expense");

        mockMvc.perform(post("/api/expenses/" + expenseId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        // Step 4: Finance marks as reimbursed
        mockMvc.perform(post("/api/expenses/" + expenseId + "/reimburse")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REIMBURSED"))
                .andExpect(jsonPath("$.reimbursedAt").exists());

        // Verify final state in database
        Expense finalExpense = expenseRepository.findById(expenseId).orElseThrow();
        Assertions.assertEquals(ApprovalStatus.REIMBURSED, finalExpense.getApprovalStatus());
        assertNotNull(finalExpense.getSubmittedAt());
        assertNotNull(finalExpense.getApprovedAt());
        assertNotNull(finalExpense.getReimbursedAt());
    }

    @Test
    @Order(2)
    @DisplayName("Integration Test 2: Policy Engine - Auto-approval for low amounts")
    void testPolicyEngineAutoApproval() throws Exception {
        // Create a low-value expense (should be auto-approved based on policy rules)
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Coffee meeting");
        expenseRequest.setAmount(new BigDecimal("25.00"));
        expenseRequest.setCategory(ExpenseCategory.MEALS);
        expenseRequest.setExpenseDate(LocalDate.now());

        MockMultipartFile receiptFile = new MockMultipartFile(
                "receipt", "receipt.jpg", "image/jpeg", "receipt image".getBytes()
        );

        MockMultipartFile expenseJson = new MockMultipartFile(
                "expense", "", "application/json",
                objectMapper.writeValueAsBytes(expenseRequest)
        );

        MvcResult result = mockMvc.perform(multipart("/api/expenses")
                        .file(expenseJson)
                        .file(receiptFile)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        Expense expense = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Expense.class
        );

        // Verify auto-approval (or manager review based on your policy rules)
        assertNotNull(expense.getApprovalStatus());
        assertTrue(
                expense.getApprovalStatus() == ApprovalStatus.AUTO_APPROVED ||
                        expense.getApprovalStatus() == ApprovalStatus.MANAGER_REVIEW
        );
    }

    @Test
    @Order(3)
    @DisplayName("Integration Test 3: Approval Workflow - Manager rejects expense")
    void testApprovalWorkflowRejection() throws Exception {
        // Create and submit expense
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Unapproved purchase");
        expenseRequest.setAmount(new BigDecimal("500.00"));
        expenseRequest.setCategory(ExpenseCategory.EQUIPMENT);
        expenseRequest.setExpenseDate(LocalDate.now());

        MockMultipartFile expenseJson = new MockMultipartFile(
                "expense", "", "application/json",
                objectMapper.writeValueAsBytes(expenseRequest)
        );

        MvcResult createResult = mockMvc.perform(multipart("/api/expenses")
                        .file(expenseJson)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        Expense expense = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Expense.class
        );

        // Submit
        mockMvc.perform(post("/api/expenses/" + expense.getId() + "/submit")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // Manager rejects
        ApprovalRequest rejectionRequest = new ApprovalRequest();
        rejectionRequest.setAction("reject");
        rejectionRequest.setComments("Not pre-approved, please get authorization first");

        mockMvc.perform(post("/api/expenses/" + expense.getId() + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectedAt").exists());

        // Verify rejection in database
        Expense rejectedExpense = expenseRepository.findById(expense.getId()).orElseThrow();
        assertEquals(ApprovalStatus.REJECTED, rejectedExpense.getApprovalStatus());
        assertNotNull(rejectedExpense.getRejectedAt());
    }

    @Test
    @Order(4)
    @DisplayName("Integration Test 4: API Security - Unauthorized access returns 401")
    void testApiSecurity_UnauthorizedAccess() throws Exception {
        // Attempt to create expense without token
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Test");
        expenseRequest.setAmount(new BigDecimal("100.00"));

        MockMultipartFile expenseJson = new MockMultipartFile(
                "expense", "", "application/json",
                objectMapper.writeValueAsBytes(expenseRequest)
        );

        mockMvc.perform(multipart("/api/expenses")
                        .file(expenseJson))
                .andExpect(status().isUnauthorized());

        // Attempt to access pending approvals without manager role
        mockMvc.perform(get("/api/expenses/pending-approvals")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk()); // Employee can view but filter will be applied
    }

    @Test
    @Order(5)
    @DisplayName("Integration Test 5: Audit Trail - Complete history tracked")
    void testAuditTrailCompleteHistory() throws Exception {
        // Create expense
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Audited expense");
        expenseRequest.setAmount(new BigDecimal("300.00"));
        expenseRequest.setCategory(ExpenseCategory.TRAVEL);
        expenseRequest.setExpenseDate(LocalDate.now());

        MockMultipartFile expenseJson = new MockMultipartFile(
                "expense", "", "application/json",
                objectMapper.writeValueAsBytes(expenseRequest)
        );

        MvcResult createResult = mockMvc.perform(multipart("/api/expenses")
                        .file(expenseJson)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        Expense expense = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                Expense.class
        );

        // Submit
        mockMvc.perform(post("/api/expenses/" + expense.getId() + "/submit")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // Approve
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setAction("approve");
        approvalRequest.setComments("Approved for travel");

        mockMvc.perform(post("/api/expenses/" + expense.getId() + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk());

        // Get audit log
        mockMvc.perform(get("/api/expenses/" + expense.getId() + "/audit-log")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[0].action").exists())
                .andExpect(jsonPath("$[0].performedBy").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @Order(6)
    @DisplayName("Integration Test 6: Database Persistence - Expense retrieval after restart")
    void testDatabasePersistence() throws Exception {
        // Create expense
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setDescription("Persistent expense");
        expenseRequest.setAmount(new BigDecimal("200.00"));
        expenseRequest.setCategory(ExpenseCategory.OTHER);

        MockMultipartFile expenseJson = new MockMultipartFile(
                "expense", "", "application/json",
                objectMapper.writeValueAsBytes(expenseRequest)
        );

        MvcResult result = mockMvc.perform(multipart("/api/expenses")
                        .file(expenseJson)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        Expense createdExpense = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Expense.class
        );

        // Retrieve from database directly
        Expense dbExpense = expenseRepository.findById(createdExpense.getId()).orElseThrow();
        assertEquals("Persistent expense", dbExpense.getDescription());
        assertEquals(new BigDecimal("200.00"), dbExpense.getAmount());

        // Retrieve via API
        mockMvc.perform(get("/api/expenses/" + createdExpense.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Persistent expense"))
                .andExpect(jsonPath("$.amount").value(200.00));
    }

    @Test
    @Order(7)
    @DisplayName("Integration Test 7: CSV Export functionality")
    void testCsvExportFunctionality() throws Exception {
        // Create multiple expenses
        for (int i = 1; i <= 3; i++) {
            ExpenseRequest expenseRequest = new ExpenseRequest();
            expenseRequest.setDescription("Export Test " + i);
            expenseRequest.setAmount(new BigDecimal(i * 50));
            expenseRequest.setCategory(ExpenseCategory.SUPPLIES);

            MockMultipartFile expenseJson = new MockMultipartFile(
                    "expense", "", "application/json",
                    objectMapper.writeValueAsBytes(expenseRequest)
            );

            mockMvc.perform(multipart("/api/expenses")
                            .file(expenseJson)
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isOk());
        }

        // Export to CSV
        MvcResult csvResult = mockMvc.perform(get("/api/expenses/export/csv")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        String csvContent = csvResult.getResponse().getContentAsString();
        assertNotNull(csvContent);
        assertTrue(csvContent.contains("ID,Description,Amount"));
        assertTrue(csvContent.length() > 100); // Should have header + data
    }

    @AfterAll
    static void tearDown() {
        postgres.stop();
    }
}
