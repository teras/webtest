@file:Suppress("unused")

package com.panayotis.webtest

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.*
import org.openqa.selenium.firefox.FirefoxDriver
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.system.exitProcess

private const val SLEEP_TIME = 0.3
private const val DOWNLOAD_KEY = "DOWNLOAD"

@Suppress("SpellCheckingInspection")
private val timefmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

internal val initializedChrome by lazy { WebDriverManager.chromedriver().setup() }
internal val initializedFirefox by lazy { WebDriverManager.firefoxdriver().setup() }

private val pkg = WebTest::class.java.`package`.name
private fun logMsg(unicode: String, ascii: String, asAscii: Boolean, message: String) = println(
    "${if (asAscii) "[$ascii]" else unicode} ${timefmt.format(Date())} $message${
        Thread.currentThread().stackTrace.drop(1)
            .find { !it.className.startsWith(pkg) }?.let {
                if (!it.fileName.isNullOrBlank() && it.lineNumber > 0) " (${it.fileName}:${it.lineNumber})"
                else ""
            } ?: ""
    }"
)

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
    private val errorScreenshot = options.errorScreenshot?.absoluteFile ?: File.createTempFile("webtest_", ".png")
    private val ascii = options.ascii

    /**
     * The location of the download folder. Could be null, if download is not supported.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val downloadDir = if (options.driver.supportsDownload) options.temp else null

    /**
     * Open a URL to start testing
     *
     * @param url The URL to open; could also be relative URL
     */
    fun open(url: String) {
        logMsg("🌐", "OPEN", ascii, "Request for URL '$url'")
        val target = if (URI(url).isAbsolute) url else URI(driver.currentUrl).resolve(url).toString()
        try {
            driver.get(target)
        } catch (e: Throwable) {
            System.err.println("Unable to open server page '$url'. Is the server running?")
            exitProcess(2)
        }
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
                { driver.findElements(By.tagName(tag)).map { Element(it, driver, ascii) } },
                { true }),
            WaitForStrategy(timeout), ascii
        )

    /**
     * Log a user-provided message
     * @param message The message to send to logger
     */
    fun log(message: String) = logMsg("📝", "MESG", ascii, message)

    /**
     * Search for a tag in current page.
     * @param tag The tag to search for
     */
    fun tag(tag: String): SearchResults =
        SearchResultsC(
            DataSet(
                "tag '$tag'",
                { driver.findElements(By.tagName(tag)).map { Element(it, driver, ascii) } },
                { true }),
            DirectStrategy(), ascii
        )

    /**
     * Delay the procedure for some time
     * @param seconds Delay in seconds, defaults to 0.3"
     */
    fun delay(seconds: Double = SLEEP_TIME) {
        logMsg("⏳", "DELY", ascii, "Waiting for ${sec(seconds)}")
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
            "⏳", "WAIT", ascii,
            "Waiting for ${sec(seconds)} until the predicate ${if (reason.isEmpty()) "" else "'$reason' "}is true"
        )
        return waitUntilNonNull(seconds) { if (predicate()) true else null } ?: false
    }


    fun screenshot(target: File) {
        val output = when (driver) {
            is FirefoxDriver -> driver.getFullPageScreenshotAs(OutputType.FILE)
            is TakesScreenshot -> driver.getScreenshotAs(OutputType.FILE)
            else -> {
                logMsg("😳", "EROR", ascii, "Unable to take screenshot using driver $driver")
                null
            }
        } ?: return
        Files.move(output.toPath(), target.toPath())
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
                    logMsg("🏃", "STRT", ascii, "Starting test '${it.name}' in ${javaClass.name}")
                    try {
                        it.invoke(this)
                    } catch (th: Throwable) {
                        System.err.println("=== ERROR while executing tests, saving screenshot to ${errorScreenshot.absolutePath} ===")
                        errorScreenshot.delete()
                        errorScreenshot.parentFile.mkdirs()
                        screenshot(errorScreenshot)
                        val e = (if (th is InvocationTargetException) th.cause else th) ?: th
                        throw e
                    }
                    logMsg("🏁️", "FINS", ascii, "Successfully terminating test '${it.name}' in '${javaClass.name}'")
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
    private val ascii: Boolean
) : SearchResults {

    override val element: Element
        get() {
            logMsg("🔍", "ELMT", ascii, "Request for first element with ${strategy.descr(dataset)}")
            val current = allElements
            return if (current.isEmpty()) throw NotFoundException("Item with ${dataset.descr} not found")
            else current.first()
        }

    override val elements: Collection<Element>
        get() {
            logMsg("🔍", "ELMS", ascii, "Request for elements with ${strategy.descr(dataset)}")
            return allElements
        }

    private val allElements: Collection<Element> get() = strategy.find(dataset)

    override fun filter(filterName: String, function: Element.() -> Boolean): SearchResults =
        SearchResultsC(dataset.filter(filterName, function), strategy, ascii)
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
        }.filter {
            try {
                filter(it)
            } catch (e: StaleElementReferenceException) {
                false
            }
        }
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
class Element internal constructor(
    val webElement: WebElement,
    private val driver: WebDriver,
    private val ascii: Boolean
) {
    /**
     * Send text to this web element. Should be an element that is possible to accept text, like an 'input'.
     * @param text the text to send
     */
    fun type(vararg text: CharSequence): Element {
        logMsg("⌨", "TYPE", ascii, "Typing '${text.joinToString(separator = "")}' on tag '$this'")
        webElement.sendKeys(*text)
        return this
    }

    /**
     * Click on this web element
     */
    fun click(): Element {
        logMsg("👆", "CLIK", ascii, "Clicking on tag '$this'")
        webElement.click()
        return this
    }

    /**
     * Clear this web element
     */
    fun clear(): Element {
        logMsg("🧹", "CLER", ascii, "Clearing tag '$this'")
        webElement.clear()
        return this
    }

    /**
     * Find the parent of current web element
     */
    val parent: Element
        get() = ((driver as? JavascriptExecutor)
            ?.executeScript("return arguments[0].parentNode;", webElement) as? WebElement)?.run {
            Element(this, driver, ascii).also {
                logMsg("⬆", "PART", ascii, "Request parent of tag '${this@Element}', which is a '$it'")
            }
        } ?: throw NotFoundException("Unable to find parent of tag '$this")

    /**
     * Find all children of current web element
     */
    val children
        get() = webElement.findElements(By.xpath("./child::*")).also {
            val size = it.size
            val plural = if (size == 1) "" else "s"
            logMsg("👶", "CHLD", ascii, "Request children of tag '${this@Element}', found $size item$plural")
        }.map { Element(it, driver, ascii) }

    /**
     * Search for a tag in current element.
     * @param tag The tag to search for
     */
    fun tag(tag: String): SearchResults =
        SearchResultsC(
            DataSet(
                "tag '$tag' inside tag '${webElement.tagName}'",
                { webElement.findElements(By.tagName(tag)).map { Element(it, driver, ascii) } },
                { true }),
            DirectStrategy(), ascii
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
                { webElement.findElements(By.tagName(tag)).map { Element(it, driver, ascii) } },
                { true }),
            WaitForStrategy(timeout), ascii
        )

    override fun toString() = webElement.tagName ?: webElement.toString()
}