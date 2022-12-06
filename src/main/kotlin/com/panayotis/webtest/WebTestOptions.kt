package com.panayotis.webtest

import java.io.File
import java.nio.file.Files

/**
 * Class to define the options of the web driver to use
 * @param driver the driver to use
 * @param headless whether the browser could be headless, defaults to true
 * @param cleanup whether the system should clean up folders (i.e. download folders) when tests have finished
 * @param quit whether the browser should quit, when tests finish
 * @param binaryPath optional path of the web browser
 */
class WebTestOptions(
    internal val driver: WebTestDriver,
    internal val headless: Boolean = true,
    internal val cleanup: Boolean = true,
    internal val quit: Boolean = true,
    internal val binaryPath: String? = null,
    internal val errorScreenshot: File? = null,
    internal val ascii: Boolean = false
) {
    internal val temp by lazy { Files.createTempDirectory("web-tests-").toAbsolutePath().toString() }
}