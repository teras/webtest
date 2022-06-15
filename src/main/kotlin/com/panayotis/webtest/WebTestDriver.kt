package com.panayotis.webtest

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverLogLevel
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxDriverLogLevel
import org.openqa.selenium.firefox.FirefoxOptions

/**
 * Enumeration of available web drivers
 */
enum class WebTestDriver {
    /**
     * Use Firefox as a web driver
     */
    FIREFOX {
        override fun construct(options: WebTestOptions): FirefoxDriver {
            initializedFirefox
            return FirefoxDriver(
                FirefoxOptions()
                    .setHeadless(options.headless)
                    .setLogLevel(FirefoxDriverLogLevel.WARN)
                    .addPreference("browser.download.dir", options.temp)
                    .addPreference("browser.download.folderList", 2)
                    .also { if (options.binaryPath != null) it.setBinary(options.binaryPath) }
            )
        }

        override val supportsDownload = true
    },

    /**
     * Use Chrome as a web driver
     */
    CHROME {
        override fun construct(options: WebTestOptions): ChromeDriver {
            initializedChrome
            return ChromeDriver(
                ChromeOptions()
                    .setHeadless(options.headless)
                    .setLogLevel(ChromeDriverLogLevel.WARNING)
                    .also { if (options.binaryPath != null) it.setBinary(options.binaryPath) }
            )
        }

        override val supportsDownload = false
    };

    internal abstract fun construct(options: WebTestOptions): WebDriver
    internal abstract val supportsDownload: Boolean
}