package com.panayotis.webtest

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxDriverLogLevel
import org.openqa.selenium.firefox.FirefoxOptions
import java.util.logging.Level

/**
 * Enumeration of available web drivers for Selenium-based testing.
 * Each driver type automatically downloads the required browser driver via Selenium Manager.
 */
enum class WebTestDriver {
    /**
     * Use Firefox (Gecko) as a web driver.
     * Supports file downloads to a temporary directory.
     */
    FIREFOX {
        override fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): FirefoxDriver {
            val firefoxOptions = FirefoxOptions()
                .setLogLevel(FirefoxDriverLogLevel.WARN)
                .addPreference("browser.download.dir", downloadDir)
                .addPreference("browser.download.folderList", 2)
            if (headless) {
                firefoxOptions.addArguments("-headless")
            }
            if (binaryPath != null) {
                firefoxOptions.setBinary(binaryPath)
            }
            return FirefoxDriver(firefoxOptions)
        }

        override val supportsDownload = true
    },

    /**
     * Use Chrome as a web driver.
     * Note: File downloads are not supported with this driver.
     */
    CHROME {
        override fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): ChromeDriver {
            val chromeOptions = ChromeOptions()
            if (headless) {
                chromeOptions.addArguments("--headless")
            }
            if (binaryPath != null) {
                chromeOptions.setBinary(binaryPath)
            }
            // Set log level via system property
            System.setProperty("webdriver.chrome.silentOutput", "true")
            java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.WARNING
            return ChromeDriver(chromeOptions)
        }

        override val supportsDownload = false
    };

    internal abstract fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): WebDriver
    internal abstract val supportsDownload: Boolean
}
