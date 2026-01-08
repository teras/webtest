package com.panayotis.webtest

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Sample test demonstrating WebTest usage.
 * Uses https://the-internet.herokuapp.com/ which is designed for Selenium testing.
 */
class SampleTest {

    @Test
    fun testClickLinks() {
        WebTest(
            driver = WebDriver.CHROME,
            binaryPath = "/usr/bin/brave",
            headless = false,
            keepOpen = true,
            errorScreenshot = File("error.png")
        ).use {
            // Open the test site
            open("https://the-internet.herokuapp.com/")
            log("Opened the-internet.herokuapp.com")

            // Click on "Checkboxes" link
            tag("a").text("Checkboxes").element.click()
            delay(1.0)

            // Verify we're on the checkboxes page
            val header = tag("h3").element
            log("Page header: ${header.text}")
            assertTrue(header.text.contains("Checkboxes"))

            // Find checkboxes and click the first one
            val checkboxes = tag("input").attribute("type", "checkbox").elements
            log("Found ${checkboxes.size} checkbox(es)")
            checkboxes.first().click()
            delay(0.5)

            // Go back to home
            open("https://the-internet.herokuapp.com/")
            delay(1.0)

            // Click on "Dropdown" link
            tag("a").text("Dropdown").element.click()
            delay(1.0)

            // Verify we're on the dropdown page
            val dropdownHeader = tag("h3").element
            log("Page header: ${dropdownHeader.text}")
            assertTrue(dropdownHeader.text.contains("Dropdown"))

            // Select from dropdown
            tag("select").element.select("Option 2")
            delay(0.5)

            log("Test completed successfully!")

        }
    }
}
