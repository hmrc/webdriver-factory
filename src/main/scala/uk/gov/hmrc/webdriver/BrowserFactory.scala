/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.webdriver

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities, LocalFileDetector, RemoteWebDriver}
import org.openqa.selenium.{MutableCapabilities, Proxy, WebDriver}

import java.net.URL
import scala.collection.JavaConverters._

class BrowserFactory extends LazyLogging {

  private val defaultSeleniumHubUrl: String                    = "http://localhost:4444/wd/hub"
  private val defaultZapHost: String                           = "localhost:11000"
  protected val zapHostInEnv: Option[String]                   = sys.env.get("ZAP_HOST")
  lazy val zapProxyInEnv: Option[String]                       = sys.props.get("zap.proxy")
  lazy val accessibilityTest: Boolean                          =
    sys.env.getOrElse("ACCESSIBILITY_TEST", sys.props.getOrElse("accessibility.test", "false")).toBoolean
  lazy val disableJavaScript: Boolean                          =
    sys.props.getOrElse("disable.javascript", "false").toBoolean
  private val enableProxyForLocalhostRequestsInChrome: String  = "<-loopback>"
  private val enableProxyForLocalhostRequestsInFirefox: String = "network.proxy.allow_hijacking_localhost"

  /*
   * Returns a specific WebDriver instance based on the value of the browserType String and the customOptions passed to the
   * function.  An exception is thrown if the browserType string value is not set or not recognised.  If customOptions are
   * passed to this function they will override the default settings in this library.
   */
  def createBrowser(browserType: Option[String], customOptions: Option[MutableCapabilities]): WebDriver =
    browserType match {
      case Some("chrome")          => chromeInstance(chromeOptions(customOptions))
      case Some("firefox")         => firefoxInstance(firefoxOptions(customOptions))
      case Some("remote-chrome")   => remoteWebdriverInstance(chromeOptions(customOptions))
      case Some("remote-firefox")  => remoteWebdriverInstance(firefoxOptions(customOptions))
      case Some("browserstack")    => browserStackInstance()
      case Some("headless-chrome") => headlessChromeInstance(chromeOptions(customOptions))
      case Some(browser)           =>
        throw BrowserCreationException(
          s"'browser' property '$browser' not supported by " +
            s"the webdriver-factory library."
        )
      case None                    =>
        throw BrowserCreationException("'browser' property is not set, this is required to instantiate a Browser")
    }

  private def chromeInstance(options: ChromeOptions): WebDriver =
    new ChromeDriver(options)

  private def headlessChromeInstance(options: ChromeOptions): WebDriver = {
    options.addArguments("headless")
    new ChromeDriver(options)
  }

  /*
   * Silences Firefox's logging when running locally with driver binary.  Ensure that the browser starts maximised.
   */
  private def firefoxInstance(options: FirefoxOptions): WebDriver = {
    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null")
    val driver = new FirefoxDriver(options)
    driver.manage().window().maximize()
    driver
  }

  private def remoteWebdriverInstance(options: MutableCapabilities): WebDriver = {
    val driver: RemoteWebDriver = new RemoteWebDriver(new URL(defaultSeleniumHubUrl), options)
    driver.setFileDetector(new LocalFileDetector)
    driver
  }

  private[webdriver] def chromeOptions(customOptions: Option[MutableCapabilities]): ChromeOptions =
    customOptions match {
      case Some(options) =>
        val userOptions = options.asInstanceOf[ChromeOptions]
        if (accessibilityTest && userOptions.asMap().get("goog:chromeOptions").toString.contains("headless"))
          throw AccessibilityAuditConfigurationException(
            s"Failed to configure Chrome browser to run accessibility-assessment tests in headless." +
              s" The accessibility-assessment can only be configured to run with non headless Chrome."
          )
        if (accessibilityTest)
          addPageCaptureChromeExtension(userOptions)
        zapConfiguration(userOptions)
        userOptions
      case None          =>
        val defaultOptions = new ChromeOptions()
        zapConfiguration(defaultOptions)
        if (accessibilityTest)
          addPageCaptureChromeExtension(defaultOptions)
        defaultOptions.addArguments("start-maximized")
        // `--use-cmd-decoder` and `--use-gl` are added as a workaround for slow test duration in chrome 85 and higher (PBD-822)
        // Can be reverted once the issue is fixed in the future versions of Chrome.
        defaultOptions.addArguments("--use-cmd-decoder=validating")
        defaultOptions.addArguments("--use-gl=desktop")

        defaultOptions.setExperimentalOption("excludeSwitches", List("enable-automation").asJava)
        defaultOptions.setExperimentalOption("useAutomationExtension", false)
        if (disableJavaScript) {
          defaultOptions.setExperimentalOption(
            "prefs",
            Map[String, Int]("profile.managed_default_content_settings.javascript" -> 2).asJava
          )
          logger.info(s"'javascript.enabled' system property is set to:$disableJavaScript. Disabling JavaScript.")
        }
        defaultOptions
    }

  private[webdriver] def firefoxOptions(customOptions: Option[MutableCapabilities]): FirefoxOptions = {
    if (accessibilityTest)
      throw AccessibilityAuditConfigurationException(
        s"Failed to configure Firefox browser to run accessibility-assessment tests." +
          s" The accessibility-assessment can only be configured to run with Chrome."
      )
    customOptions match {
      case Some(options) =>
        val userOptions = options.asInstanceOf[FirefoxOptions]
        zapConfiguration(userOptions)
        userOptions
      case None          =>
        val defaultOptions = new FirefoxOptions()
        defaultOptions.setAcceptInsecureCerts(true)
        defaultOptions.addPreference(enableProxyForLocalhostRequestsInFirefox, true)
        zapConfiguration(defaultOptions)
        if (disableJavaScript) {
          defaultOptions.addPreference("javascript.enabled", false)
          logger.info(s"'javascript.enabled' system property is set to:$disableJavaScript. Disabling JavaScript.")
        }
        defaultOptions
    }
  }

  /**
    * Configures ZAP proxy settings in the provided browser options.
    * The configuration is set when:
    *  - the environment property ZAP_HOST is set (or)
    *  - the system property `zap.proxy` is set to true
    * @param options accepts a MutableCapabilities object to configure ZAP proxy.
    * @throws ZapConfigurationException when ZAP_HOST is not of the format localhost:port
    */
  private def zapConfiguration(options: MutableCapabilities): Unit = {
    (zapHostInEnv, zapProxyInEnv) match {
      case (Some(zapHost), _)   =>
        val hostPattern = "(localhost:[0-9]+)".r
        zapHost match {
          case hostPattern(host) => setZapHost(host)
          case _                 =>
            throw ZapConfigurationException(
              s" Failed to configure browser with ZAP proxy." +
                s" Environment variable ZAP_HOST is not of the format localhost:portNumber."
            )
        }
      case (None, Some("true")) => setZapHost(defaultZapHost)
      case _                    => ()
    }

    def setZapHost(host: String): Unit = {
      //Chrome does not allow proxying to localhost by default. This allows chrome to proxy requests to localhost.
      val noProxy: String =
        if (options.getBrowserName.equalsIgnoreCase("chrome"))
          enableProxyForLocalhostRequestsInChrome
        else ""
      options.setCapability(CapabilityType.PROXY, new Proxy().setHttpProxy(host).setNoProxy(noProxy))
      logger.info(s"Zap Configuration Enabled: using $host ")
    }
  }

  /**
    * Configures page-capture-chrome-extension for the accessibility assessment in the provided browser options.
    * The configuration in set when:
    *  - the browser being used is chrome (and)
    *  - the system property `accessibility.test` is set to true (or)
    *  - the environment property `ACCESSIBILITY_TEST` is set to true
    * @param options accepts a ChromeOptions object to add the page-capture-chrome-extension.
    * The encoded string can be generated by using the `encodeExtension.sh` script within the `remote-webdriver-proxy-scripts` repository
    */
  private def addPageCaptureChromeExtension(options: ChromeOptions): Unit = {
    options.addEncodedExtensions(
      "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"
    )
    logger.info(s"Configured ${options.getBrowserName} browser to run accessibility-assessment tests.")
  }

  /*
   * The tests can be ran using browserstack with different capabilities passed from the command line. e.g -Dbrowserstack.version="firefox" and this allows a flexibility to test using different configs to override the default browserstack values.
   * An exception will be thrown if username or key is not passed.
   */
  def browserStackInstance(): WebDriver = {
    val username           = sys.props.getOrElse(
      "browserstack.username",
      throw new Exception("browserstack.username is required. Enter a valid username")
    )
    val automateKey        =
      sys.props.getOrElse("browserstack.key", throw new Exception("browserstack.key is required. Enter a valid key"))
    val browserStackHubUrl = s"http://$username:$automateKey@hub.browserstack.com/wd/hub"

    val desiredCaps = new DesiredCapabilities()
    desiredCaps.setCapability("browserstack.debug", "true")
    desiredCaps.setCapability("browserstack.local", "true")

    val properties: Map[String, String] =
      sys.props.toMap[String, String].filter(key => key._1.startsWith("browserstack") && key._2 != "")

    properties.map(x => (x._1.replace("browserstack.", ""), x._2.replace("_", " ")))
    properties
      .foreach(x => desiredCaps.setCapability(x._1.replace("browserstack.", ""), x._2.replace("_", " ")))

    new RemoteWebDriver(new URL(browserStackHubUrl), desiredCaps)
  }
}
case class BrowserCreationException(message: String) extends RuntimeException(message)

case class ZapConfigurationException(message: String) extends RuntimeException(message)

case class AccessibilityAuditConfigurationException(message: String) extends RuntimeException(message)
