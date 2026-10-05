package com.library.ledger.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BorrowingWorkflowE2ETest {

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private void loginAsAdmin() {
        driver.get(getBaseUrl() + "/login");
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));

        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("admin123");
        submitButton.click();
        wait.until(ExpectedConditions.urlContains("/books"));
    }

    @Test
    @DisplayName("E2E: Lập phiếu mượn mới và kiểm tra điều hướng đến trang chi tiết")
    void testCreateBorrowingWorkflow() {
        loginAsAdmin();

        // 1. Đi tới trang tạo phiếu mượn
        driver.get(getBaseUrl() + "/borrowings/create");
        wait.until(ExpectedConditions.urlContains("/borrowings/create"));
        assertThat(driver.getTitle().toLowerCase()).contains("phiếu mượn");

        // 2. Chọn độc giả
        WebElement memberSelectElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("memberId")));
        Select memberSelect = new Select(memberSelectElement);
        // Chọn thành viên đầu tiên có sẵn trong danh sách
        memberSelect.selectByIndex(1);

        // 3. Chọn sách dòng đầu tiên
        WebElement bookSelectElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("select[name='items[0].bookId']")));
        Select bookSelect = new Select(bookSelectElement);
        bookSelect.selectByIndex(1);

        // 4. Nhấn nút tăng số lượng stepper [+]
        WebElement btnPlus = driver.findElement(By.cssSelector(".btn-step-up"));
        btnPlus.click();

        // 5. Kiểm tra giá trị ô input số lượng đã tăng thành 2
        WebElement qtyInput = driver.findElement(By.cssSelector(".qty-input"));
        assertThat(qtyInput.getAttribute("value")).isEqualTo("2");

        // 6. Nhấn nút Tạo phiếu mượn
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();

        // 7. Chờ chuyển hướng tới trang chi tiết phiếu mượn mới
        wait.until(ExpectedConditions.urlMatches(".*/borrowings/\\d+.*"));
        assertThat(driver.getCurrentUrl()).matches(".*/borrowings/\\d+.*");

        // 8. Xác nhận trang chi tiết hiển thị đúng tiêu đề
        WebElement detailTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        assertThat(detailTitle.getText()).contains("Phiếu mượn");
    }
}
