package com.panayotis.webtest

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxDriverLogLevel
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.safari.SafariOptions
import java.util.logging.Level

/**
 * Enumeration of available web drivers for Selenium-based testing.
 * Each driver type automatically downloads the required browser driver via Selenium Manager.
 */
enum class WebDriver {
    /**
     * Use Firefox (Gecko) as a web driver.
     * Supports file downloads to a temporary directory.
     */
    FIREFOX {
        override fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): WebDriver {
            val options = FirefoxOptions()
                .setLogLevel(FirefoxDriverLogLevel.WARN)
                .addPreference("browser.download.dir", downloadDir)
                .addPreference("browser.download.folderList", 2)
            if (headless) options.addArguments("-headless")
            if (binaryPath != null) options.setBinary(binaryPath)
            return FirefoxDriver(options)
        }

        override val supportsDownload = true
    },

    /**
     * Use Chrome as a web driver.
     * Also works with Chromium-based browsers (Brave, Chromium, Opera, etc.) by specifying `binaryPath`.
     */
    CHROME {
        override fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): WebDriver {
            val options = ChromeOptions()
            if (headless) options.addArguments("--headless")
            if (binaryPath != null) options.setBinary(binaryPath)
            System.setProperty("webdriver.chrome.silentOutput", "true")
            java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.WARNING
            return ChromeDriver(options)
        }

        override val supportsDownload = false
    },

    /**
     * Use Microsoft Edge as a web driver.
     */
    EDGE {
        override fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): WebDriver {
            val options = EdgeOptions()
            if (headless) options.addArguments("--headless")
            if (binaryPath != null) options.setBinary(binaryPath)
            System.setProperty("webdriver.chrome.silentOutput", "true")
            java.util.logging.Logger.getLogger("org.openqa.selenium").level = Level.WARNING
            return EdgeDriver(options)
        }

        override val supportsDownload = false
    },

    /**
     * Use Safari as a web driver.
     * Only available on macOS. Requires enabling WebDriver via: `safaridriver --enable`
     * Note: Headless mode is not supported by Safari.
     */
    SAFARI {
        override fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): WebDriver {
            val options = SafariOptions()
            // Safari does not support headless mode or custom binary path
            return SafariDriver(options)
        }

        override val supportsDownload = false
    };

    internal abstract fun construct(headless: Boolean, binaryPath: String?, downloadDir: String): WebDriver
    internal abstract val supportsDownload: Boolean
}
