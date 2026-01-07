package com.panayotis.webtest

import com.panayotis.webtest.WebTestDriver.FIREFOX
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Sample test demonstrating WebTest usage.
 * Uses https://the-internet.herokuapp.com/ which is designed for Selenium testing.
 */
class SampleTest {

    @Test
    fun testClickLinks() {
        val test = WebTest(FIREFOX, headless = false, quit = false)

        // Open the test site
        test.open("https://the-internet.herokuapp.com/")
        test.log("Opened the-internet.herokuapp.com")

        // Click on "Checkboxes" link
        test.tag("a").text("Checkboxes").element.click()
        test.delay(1.0)

        // Verify we're on the checkboxes page
        val header = test.tag("h3").element
        test.log("Page header: ${header.webElement.text}")
        assertTrue(header.webElement.text.contains("Checkboxes"))

        // Find checkboxes and click the first one
        val checkboxes = test.tag("input").attribute("type", "checkbox").elements
        test.log("Found ${checkboxes.size} checkboxes")
        checkboxes.first().click()
        test.delay(0.5)

        // Go back to home
        test.open("https://the-internet.herokuapp.com/")
        test.delay(1.0)

        // Click on "Dropdown" link
        test.tag("a").text("Dropdown").element.click()
        test.delay(1.0)

        // Verify we're on the dropdown page
        val dropdownHeader = test.tag("h3").element
        test.log("Page header: ${dropdownHeader.webElement.text}")
        assertTrue(dropdownHeader.webElement.text.contains("Dropdown"))

        test.log("Test completed successfully!")
    }
}
