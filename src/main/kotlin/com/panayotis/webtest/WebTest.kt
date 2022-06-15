@file:Suppress("unused")

package com.panayotis.webtest

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.*
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private const val SLEEP_TIME = 0.3
private const val DOWNLOAD_KEY = "DOWNLOAD"
private val timefmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

internal val initializedChrome by lazy { WebDriverManager.chromedriver().setup() }
internal val initializedFirefox by lazy { WebDriverManager.firefoxdriver().setup() }

private fun logMsg(tag: String, message: String) =
    println("$tag ${timefmt.format(Date())} $message")

private fun sec(seconds: Double) = if (abs(seconds - 1.0) <= 0.0001) "1 second" else "$seconds seconds"

private fun <T : Any> waitUntilNonNull(timeout: Double, predicate: () -> T?): T? {
    var elapsed = 0.0
    while (elapsed <= timeout) {
        val item = predicate()
        if (item != null)
            return item
        Thread.sleep((SLEEP_TIME * 1000).toLong())
        elapsed += SLEEP_TIME
    }
    return null
}

/**
 * Base class for all Selenium-based tests
 * @param options The options to use for this web test
 */
open class WebTest(options: WebTestOptions) {
    private val driver = options.driver.construct(options)
    private val shouldQuit = options.quit
    private val shouldCleanup = options.cleanup

    /**
     * The location of the download folder. Could be null, if download is not supported.
     */
    val downloadDir = if (options.driver.supportsDownload) options.temp else null

    /**
     * Open a URL to start testing
     *
     * @param url The URL to open; could also be relative URL
     */
    fun open(url: String) {
        logMsg("🌐", "Request for URL '$url'")
        val target = if (URI(url).isAbsolute) url else URI(driver.currentUrl).resolve(url).toString()
        driver.get(target)
    }

    /**
     * Wait for a tag to appear and then search for it.
     *
     * @param tag The tag to search for
     * @param timeout The timeout time in seconds. This is the maximum time that the system will
     * wait for a tag to appear
     */
    fun waitForTag(tag: String, timeout: Double = 60.0): SearchResults =
        SearchResultsC(
            DataSet(
                "tag '$tag'",
                { driver.findElements(By.tagName(tag)).map { Element(it, driver) } },
                { true }),
            WaitForStrategy(timeout)
        )

    /**
     * Log a user-provided message
     * @param message The message to send to logger
     */
    fun log(message: String) = logMsg("📝", message)

    /**
     * Search for a tag in current page.
     * @param tag The tag to search for
     */
    fun tag(tag: String): SearchResults =
        SearchResultsC(
            DataSet(
                "tag '$tag'",
                { driver.findElements(By.tagName(tag)).map { Element(it, driver) } },
                { true }),
            DirectStrategy()
        )

    /**
     * Delay the procedure for some time
     * @param seconds Delay in seconds, defaults to 0.3"
     */
    fun delay(seconds: Double = SLEEP_TIME) {
        logMsg("⏳", "Waiting for ${sec(seconds)}")
        Thread.sleep((seconds * 1000).toLong())
    }

    /**
     * Wait for a predicate to fulfill, or exit if a timeout period is passed.
     * @param seconds the timeout period
     * @param reason Describe what the predicate is doing. Could be empty
     * @param predicate define a predicate that, when true, the wait is over
     */
    fun waitFor(seconds: Double = 1.0, reason: String = "", predicate: () -> Boolean): Boolean {
        logMsg(
            "⏳",
            "Waiting for ${sec(seconds)} until the predicate ${if (reason.isEmpty()) "" else "'$reason' "}is true"
        )
        return waitUntilNonNull(seconds) { if (predicate()) true else null } ?: false
    }

    /**
     * Begin tests
     */
    fun start() = try {
        javaClass.declaredMethods
            .filter { it.getAnnotation(TestScenario::class.java) != null }
            .forEach {
                if (it.parameters.isNotEmpty()) throw IllegalArgumentException("Test methods could not have arguments")
                else {
                    logMsg("🏃", "Starting test '${it.name}' in ${javaClass.name}")
                    it.invoke(this)
                    logMsg("🏁️", "Successfully terminating test '${it.name}' in '${javaClass.name}'")
                }
            }
    } finally {
        if (shouldQuit)
            driver.quit()
        if (shouldCleanup && downloadDir != null) Files
            .walk(File(downloadDir).toPath())
            .sorted(Comparator.reverseOrder())
            .map { it.toFile() }
            .forEach { it.delete() }
    }
}

private open class SearchResultsC(
    private val dataset: DataSet,
    protected val strategy: SearchStrategy,
) : SearchResults {

    override val element: Element
        get() {
            logMsg("🔍", "Request for first element with ${strategy.descr(dataset)}")
            val current = allElements
            return if (current.isEmpty()) throw NotFoundException("Item with ${dataset.descr} not found")
            else current.first()
        }

    override val elements: Collection<Element>
        get() {
            logMsg("🔍", "Request for elements with ${strategy.descr(dataset)}")
            return allElements
        }

    private val allElements: Collection<Element> get() = strategy.find(dataset)

    override fun filter(filterName: String, function: Element.() -> Boolean): SearchResults =
        SearchResultsC(dataset.filter(filterName, function), strategy)

}

private class DataSet(
    val descr: String,
    private val allElements: () -> Collection<Element>,
    private val filter: Element.() -> Boolean
) {
    val elements = {
        try {
            allElements()
        } catch (e: Exception) {
            emptyList()
        }.filter(filter)
    }

    fun filter(filterName: String, newFilter: Element.() -> Boolean) =
        DataSet("$descr and filter '$filterName'", allElements) { filter() && newFilter() }
}

private interface SearchStrategy {
    fun find(dataset: DataSet): Collection<Element>
    fun descr(dataset: DataSet): String
}

private class DirectStrategy : SearchStrategy {
    override fun find(dataset: DataSet) = dataset.elements()

    override fun descr(dataset: DataSet) = dataset.descr
}

private class WaitForStrategy(private val timeout: Double) : SearchStrategy {
    override fun find(dataset: DataSet) =
        waitUntilNonNull(timeout) { dataset.elements().ifEmpty { null } } ?: emptyList()

    override fun descr(dataset: DataSet) = "${dataset.descr} within the next ${sec(timeout)}"
}

/**
 * Model an element as found inside the web page. Will wrap a defined Selenium web element
 */
class Element internal constructor(val webElement: WebElement, private val driver: WebDriver) {
    /**
     * Send text to this web element. Should be an element that is possible to accept text, like an 'input'.
     * @param text the text to send
     */
    fun type(text: String): Element {
        logMsg("⌨", "Typing '$text' on tag '$this'")
        webElement.sendKeys(text)
        return this
    }

    /**
     * Click on this web element
     */
    fun click(): Element {
        logMsg("👆", "Clicking on tag '$this'")
        webElement.click()
        return this
    }

    /**
     * Find the parent of current web element
     */
    val parent: Element
        get() = ((driver as? JavascriptExecutor)
            ?.executeScript("return arguments[0].parentNode;", webElement) as? WebElement)?.run {
            Element(this, driver).also {
                logMsg("⬆", "Request parent of tag '${this@Element}', which is a '$it'")
            }
        } ?: throw NotFoundException("Unable to find parent of tag '$this")

    /**
     * Search for a tag in current element.
     * @param tag The tag to search for
     */
    fun tag(tag: String): SearchResults =
        SearchResultsC(
            DataSet(
                "tag '$tag' inside tag '${webElement.tagName}'",
                { webElement.findElements(By.tagName(tag)).map { Element(it, driver) } },
                { true }),
            DirectStrategy()
        )

    /**
     * Wait for a tag to appear and then search for it.
     *
     * @param tag The tag to search for
     * @param timeout The timeout time in seconds. This is the maximum time that the system will
     * wait for a tag to appear
     */
    fun waitForTag(tag: String, timeout: Double = 60.0): SearchResults =
        SearchResultsC(
            DataSet(
                "tag '$tag' inside tag '$this'",
                { webElement.findElements(By.tagName(tag)).map { Element(it, driver) } },
                { true }),
            WaitForStrategy(timeout)
        )

    override fun toString() = webElement.tagName ?: webElement.toString()
}