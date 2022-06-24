package com.panayotis.webtest

import org.openqa.selenium.NotFoundException

/**
 * The result of a webpage search
 */
interface SearchResults {
    /**
     * Get all elements in page that fulfil provided filters
     */
    val elements: Collection<Element>

    /**
     * Get first element in page that fulfils provided filters or fail if it doesn't exist
     */
    val element: Element

    /**
     * Get the first element in page that fulfils provided filters or null if it doesn't exist
     */
    val optionalElement: Element?
        get() = try {
            this.element
        } catch (e: NotFoundException) {
            null
        }

    /**
     * Filter elements based on some custom predicates
     * @param filterName The name of this filter; can be any free text
     * @param function the predicate to use
     * @return the chain of SearchResults
     */
    fun filter(filterName: String, function: Element.() -> Boolean): SearchResults

    /**
     * Filter elements whether the value of a given attribute contains some specific text
     * @param attribute the name of the attribute
     * @param containing The text that should be contained inside the value of the given attribute
     * @return the chain of SearchResults
     */
    fun attribute(attribute: String, containing: String) =
        filter("attribute \"$attribute\" with value \"$containing\"") {
            webElement.getAttribute(attribute)?.contains(containing) ?: false
        }

    /**
     * Filter elements whether the value of a given attribute contains some specific text
     * @param containing The text that should be enclosed inside current tag
     * @return the chain of SearchResults
     */
    fun text(containing: String) =
        filter("text contains \"$containing\"") { webElement.text?.contains(containing) ?: false }
}