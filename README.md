# WebTest

A simple, fluent Kotlin API for Selenium-based web testing.

## Installation

### Maven
```xml
<dependency>
    <groupId>com.panayotis</groupId>
    <artifactId>webtest</artifactId>
    <version>0.3.0</version>
</dependency>
```

### Gradle
```kotlin
implementation("com.panayotis:webtest:0.3.0")
```

## Quick Start

```kotlin
WebTest(driver = WebDriver.CHROME, headless = true).use {
    open("https://example.com")

    tag("input").attribute("name", "q").element.type("search query")
    tag("button").text("Search").element.click()

    val result = waitForTag("h2").element
    log("Found: ${result.text}")
}
```

## WebTest Configuration

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `driver` | WebDriver | FIREFOX | Browser to use |
| `headless` | Boolean | true | Run without GUI |
| `binaryPath` | String? | null | Custom browser path (for Brave, Chromium) |
| `errorScreenshot` | File? | null | Save screenshot on error |
| `keepOpen` | Boolean | false | Keep browser open after test |
| `cleanup` | Boolean | true | Clean up temp files |
| `ascii` | Boolean | false | Use ASCII instead of emoji in logs |

## Available Drivers

| Driver | Description |
|--------|-------------|
| `FIREFOX` | Firefox (supports downloads, `binaryPath` supported) |
| `CHROME` | Chrome (also for Brave, Chromium via `binaryPath`) |
| `EDGE` | Microsoft Edge (`binaryPath` supported) |
| `SAFARI` | Safari (macOS only, no headless support) |

## WebTest Methods

| Method | Description |
|--------|-------------|
| `open(url)` | Navigate to URL |
| `back()` | Browser back |
| `forward()` | Browser forward |
| `refresh()` | Reload page |
| `currentUrl` | Get current URL |
| `title` | Get page title |
| `tag(name)` | Find elements by tag |
| `waitForTag(name, timeout)` | Wait for tag to appear (default timeout: 60s) |
| `delay(seconds)` | Pause execution (default: 0.3s) |
| `waitFor(seconds, reason, predicate)` | Wait for condition (default timeout: 1s, reason appears in logs) |
| `screenshot(file)` | Take screenshot |
| `log(message)` | Log message |
| `downloadDir` | Download folder path (Firefox only, `null` for others) |
| `webDriver` | Access underlying Selenium WebDriver |

## Special Keys

WebTest re-exports Selenium's `Keys` for easy access to special keys:

```kotlin
import com.panayotis.webtest.Keys

// Usage in type()
tag("input").element.type("search query", Keys.ENTER)
tag("input").element.type(Keys.TAB)
tag("input").element.type(Keys.ESCAPE)
```

Common keys: `ENTER`, `TAB`, `ESCAPE`, `BACKSPACE`, `DELETE`, `ARROW_UP`, `ARROW_DOWN`, `ARROW_LEFT`, `ARROW_RIGHT`, `HOME`, `END`, `PAGE_UP`, `PAGE_DOWN`, `CONTROL`, `SHIFT`, `ALT`

## Finding Elements

```kotlin
// By tag
tag("input").element

// With filters
tag("a").text("Click me").element
tag("input").attribute("type", "text").element

// Multiple elements
tag("li").elements

// Optional element (returns null if not found)
tag("div").attribute("class", "modal").optionalElement

// Wait for element
waitForTag("div", timeout = 10.0).element

// Nested search
tag("form").element.tag("input").element

// Wait for nested element
tag("table").element.waitForTag("tr", timeout = 5.0).elements
```

## SearchResults Filters

| Filter | Description |
|--------|-------------|
| `.text("...")` | Text contains |
| `.attribute("name", "value")` | Attribute contains value |
| `.filter("name") { ... }` | Custom filter |
| `.element` | Get first match (throws if not found) |
| `.optionalElement` | Get first match or `null` |
| `.elements` | Get all matches |

## Element Methods

| Method | Description |
|--------|-------------|
| `click()` | Click element |
| `jsClick()` | Click using JavaScript (for non-interactable elements) |
| `type(text)` | Type text (supports vararg, including `Keys`) |
| `clear()` | Clear input |
| `hover()` | Hover over element (triggers dropdowns/tooltips) |
| `scrollIntoView()` | Scroll element into view |
| `select(text)` | Select dropdown option by visible text (partial match) |
| `selectByValue(value)` | Select dropdown option by value attribute (partial match) |
| `text` | Get element text |
| `value` | Get input value |
| `parent` | Get parent element |
| `children` | Get child elements |
| `tag(name)` | Find nested elements |
| `waitForTag(name, timeout)` | Wait for nested tag to appear (default timeout: 60s) |
| `webElement` | Access underlying Selenium element |

> **Note:** Methods like `click()`, `type()`, `clear()`, `hover()`, `scrollIntoView()`, `jsClick()`, `select()`, and `selectByValue()` return `this` for method chaining (e.g., `element.scrollIntoView().click()`).

## Examples

### Form Submission
```kotlin
WebTest().use {
    open("https://example.com/login")
    tag("input").attribute("name", "username").element.type("user")
    tag("input").attribute("name", "password").element.type("pass")
    tag("button").text("Login").element.click()
}
```

### Dropdown Selection
```kotlin
// By visible text (partial match)
tag("select").element.select("Option 2")

// By value attribute (partial match)
tag("select").element.selectByValue("opt2")
```

### Using Brave Browser
```kotlin
WebTest(
    driver = WebDriver.CHROME,
    binaryPath = "/usr/bin/brave"
).use {
    // ...
}
```

### Debug Mode (Keep Browser Open)
```kotlin
WebTest(
    headless = false,
    keepOpen = true,
    errorScreenshot = File("error.png")
).use {
    // Browser stays open after test
}
```

### Optional Elements
```kotlin
// Returns null instead of throwing if not found
val popup = tag("div").attribute("class", "popup").optionalElement
popup?.let {
    it.tag("button").text("Close").element.click()
}
```

### Hover and Dropdown Menus
```kotlin
// Trigger dropdown menu by hovering
tag("nav").element.tag("a").text("Menu").element.hover()
waitForTag("ul").attribute("class", "dropdown").element
    .tag("a").text("Settings").element.click()
```

### Handling Hidden Elements
```kotlin
// Scroll element into view before interacting
tag("button").text("Load More").element
    .scrollIntoView()
    .click()

// Use JavaScript click for stubborn elements
tag("button").attribute("class", "hidden-btn").element.jsClick()
```

### Form with Special Keys
```kotlin
tag("input").attribute("name", "search").element
    .type("kotlin webtest", Keys.ENTER)
```

## License

Apache License 2.0
