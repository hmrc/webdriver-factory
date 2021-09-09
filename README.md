# webdriver-factory

This scala library provides a factory method to instantiate Selenium WebDriver for use in UI Journey Test Suites running locally, and in the build. 

An example of a test suite consuming the webdriver-factory library can be found in the ["platform-example"](https://github.com/hmrc/platform-example-ui-journey-tests) repository.

## Using the library

### 1. Update your sbt build
In your `build.sbt` file, add the following:

```scala
testDependencies += "uk.gov.hmrc" %% "webdriver-factory % "x.x.x"
```
Replace `x.x.x` with a valid webdriver-factory version

You do not need to add any HMRC resolvers in your repo if you use sbt-auto-build.
If you need to add any other resolvers, ensure you are using `+=` rather than `:=` to not replace them.

```scala
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
```

### 2. Instantiating a browser with default options

To create a WebDriver instance, simply call the getInstance() function on the SingletonDriver:

```scala 
lazy val webDriver: WebDriver = SingletonDriver.getInstance()
```

This returns a WebDriver instance with the library's default browser options for [Chrome](https://github.com/hmrc/webdriver-factory/blob/master/src/main/scala/uk/gov/hmrc/webdriver/BrowserFactory.scala#L47)
or [Firefox](https://github.com/hmrc/webdriver-factory/blob/master/src/main/scala/uk/gov/hmrc/webdriver/BrowserFactory.scala#L66).

The library depends on the System property `browser` to determine the type of Driver to instantiate. For example, to create a Chrome browser with Options using the RemoteWebDriver class, do the following:
```scala
sbt -Dbrowser=remote-chrome test
```

Failing to provide a value to 'browser', or providing an unsupported value, will result in an error.

The following browser types are supported:
* `chrome`: creates a browser using your local ChromeDriver binary.  Ensure that `` is set
* `firefox` : creates a browser using your local gecko-driver binary.  Ensure that the `` system prop is set
* `headless-chrome`: creates a headless browser using your local ChromeDriver binary
* `remote-chrome` : for use in the build when creating a Chrome browser with RemoteWebDriver, using the default webdriver hub URL (`http://localhost:4444/wd/hub`)
* `remote-firefox` : for use in the build when creating a Firefox browser with RemoteWebDriver, using the default webdriver hub URL (`http://localhost:4444/wd/hub`)
* `browserstack` : for use in your local development environment if your tests run against BrowserStack

More detail on each of the supported browser types can be found in the code [here](https://github.com/hmrc/webdriver-factory/blob/master/src/main/scala/uk/gov/hmrc/webdriver/BrowserFactory.scala#L18) 


### 3. Instantiating a browser with custom options

`SingletonDriver`'s `getInstance` method takes an optional browser options/capabilities parameter of type `Option[MutableCapabilities]`.
 
 To instantiate a WebDriver with custom options, build your own Options config and pass it as an argument to the getInstance() function:

```scala 
val options = new ChromeOptions()
options.setHeadless(true)
lazy val webDriver: WebDriver = SingletonDriver.getInstance(Some(options))
```

**NOTE:** *Custom options are not merged with the default options.  By providing custom options you will override the library's default settings.*  

## HMRC Build and Test Environment settings

### Default Browser Options/Capabilities
Irrespective of whether or not you're testing with Chrome or Firefox, this library attempts to start a browser with the following features:
* **A maximised browser window**  
* **Accepting certificate errors**: we don't want all testing to stop if certificates expire
* **No infobars** : only appears to be relevant in Chrome.  
* **File upload from tests** : All RemoteWebDriver instances are returned with the `LocalFileDetector` set, which ensures that the browser uploads files local to the test repository and not the browser itself (as it's running in its own container). 

**A note on ChromeDriver:** 
When [ChromeDriver](http://chromedriver.chromium.org/) is invoked, we've seen the following command line switches set in test repositories quite frequently.  ChromeDriver will add these switches when it launches Chrome.  Therefore, you **do not** need to pass the following arguments to ChromeOptions when creating a WebDriver instance in your test repo:
* `--no-sandbox`
* `--enable-automation`
* `--test-type`
* `--ignore-certificate-errors`

### Proxying traffic via Zap 

Start ZAP locally on port 11000. Set the system property `zap.proxy` to `true`. The library will then configure the WebDriver
 with a default proxy host `localhost:11000`.  
 
 ```scala
 sbt -Dbrowser=remote-chrome -Dzap.proxy=true test
 ```

To override the default ZAP host, set the new value in environment variable `ZAP_HOST`. This should be
 of the format `localhost:portNumber`. Any other format would return `ZapConfigurationException`. 

```
export ZAP_HOST=localhost:1234
``` 

### Running accessibility tests

To run accessibility tests locally, set the system property `accessibility.test` to `true`. 
The library will then configure your tests to capture pages for the accessibility-assessment service.

Further information on how to run your accessibility tests locally can be found [here](https://github.com/hmrc/accessibility-assessment#running-accessibility-assessment-tests-locally).

## Running tests using Browser Stack
The HMRC MDTP build *does not* support running of Browser Stack tests.  Instantiation of a WebDriver instance for use with Browser Stack is included in the webdriver-factory library for local test execution only.

Visit the [Browser Stack website](https://www.browserstack.com/automate/java) to get started. 

### Setting up different capabilities
The default browserstack capabilities can be overridden by wrapping your test execution with a bash script that overrides the following system properties:

```bash
-Dbrowserstack.debug="true"
-Dbrowserstack.seleniumLogs="false"
-Dbrowserstack.browser="Firefox"
```

```bash
# Use the following if testing on a mobile:
-Dbrowserstack.real_mobile="true"
-Dbrowserstack.os="ios"
-Dbrowserstack.device="iPhone 8 Plus"
-Dbrowserstack.os_version="11"
```

For more info about Browser Stack specific capabilities, please visit the [capabilities](https://www.browserstack.com/automate/capabilities) page at Browser Stack

## Development

### Running the tests
As this library's purpose is to integrate HMRC UI test code with ChromeDriver, GeckoDriver and SeleniumHub, the tests in this repo require these components to be in scope.  
Please ensure you have:
* [geckodriver](https://github.com/mozilla/geckodriver) installed and on your PATH (or set `webdriver.firefox.driver` to point at your installation)
* [chromedriver](http://chromedriver.chromium.org/) installed and on your PATH (or set `webdriver.chrome.driver` to point at your installation)
* selenium server running on port 4444 with a single chrome node available.  This can be done by running the [standalone-chrome-debug](https://hub.docker.com/r/selenium/standalone-chrome-debug) docker image ([more details](https://github.com/SeleniumHQ/docker-selenium/blob/master/README.md]))

Then execute:
`sbt test`

### Formatting code
This library uses [Scalafmt](https://scalameta.org/scalafmt/), a code formatter for Scala. The formatting rules configured for this repository are defined within [.scalafmt.conf](.scalafmt.conf). Prior to checking in any changes to this repository, please make sure all files are formatted correctly.

To apply formatting to this repository using the configured rules in [.scalafmt.conf](.scalafmt.conf) execute:

```
sbt scalafmtAll
```

To check files have been formatted as expected execute:

```
sbt scalafmtCheckAll scalafmtSbtCheck
```


### Suggestions
Feel free to reach out to PlatOps or PlatUI on Slack, or submit a PR for review.  

We welcome conversation on how the library could be improved.
