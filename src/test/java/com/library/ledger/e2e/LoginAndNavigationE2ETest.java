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
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginAndNavigationE2ETest {

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

    @Test
    @DisplayName("E2E: Đăng nhập bằng tài khoản admin và duyệt các trang nghiệp vụ")
    void testLoginAndNavigate() {
        // 1. Mở trang đăng nhập
        driver.get(getBaseUrl() + "/login");
        assertThat(driver.getTitle()).contains("Đăng nhập");

        // 2. Điền form đăng nhập
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));

        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("admin123");
        submitButton.click();

        // 3. Sau khi đăng nhập, hệ thống chuyển hướng vào /books
        wait.until(ExpectedConditions.urlContains("/books"));
        assertThat(driver.getCurrentUrl()).contains("/books");

        // 4. Kiểm tra thành phần Sidebar
        WebElement sidebar = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("aside")));
        assertThat(sidebar.getText().toLowerCase()).contains("thư viện nội bộ");

        // 5. Điều hướng sang trang Danh mục Thể loại
        driver.get(getBaseUrl() + "/categories");
        wait.until(ExpectedConditions.urlContains("/categories"));
        WebElement catTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        assertThat(catTitle.getText().toLowerCase()).contains("thể loại");

        // 6. Điều hướng sang trang Phiếu mượn
        driver.get(getBaseUrl() + "/borrowings");
        wait.until(ExpectedConditions.urlContains("/borrowings"));
        WebElement borrowingTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        assertThat(borrowingTitle.getText().toLowerCase()).contains("phiếu mượn");

        // 7. Điều hướng sang Báo cáo quá hạn
        driver.get(getBaseUrl() + "/reports/overdue");
        wait.until(ExpectedConditions.urlContains("/reports/overdue"));
        WebElement reportTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        assertThat(reportTitle.getText().toLowerCase()).contains("quá hạn");
    }
}
