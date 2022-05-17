/*
 * Copyright 2022 HM Revenue & Customs
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

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxOptions
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BrowserFactorySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  trait Setup {
    val browserFactory: BrowserFactory = new BrowserFactory()
  }

  override def beforeEach(): Unit = {
    System.clearProperty("zap.proxy")
    System.clearProperty("javascript")
    System.clearProperty("accessibility.test")
    System.clearProperty("disable.javascript")
  }

  override def afterEach(): Unit = {
    System.clearProperty("zap.proxy")
    System.clearProperty("javascript")
    System.clearProperty("accessibility.test")
    System.clearProperty("disable.javascript")
    SingletonDriver.closeInstance()
  }

  "BrowserFactory" should {

    "return chromeOptions with default options" in new Setup {
      val options: ChromeOptions = browserFactory.chromeOptions(None)
      options.asMap().get("browserName")          shouldBe "chrome"
      options
        .asMap()
        .get("goog:chromeOptions")
        .toString                                 shouldBe "{args=[start-maximized, --use-cmd-decoder=validating, --use-gl=desktop], excludeSwitches=[enable-automation], extensions=[], useAutomationExtension=false}"
      options.asMap().getOrDefault("proxy", None) shouldBe None
    }

    "return chromeOptions with zap proxy configuration when zap.proxy is true" in new Setup {
      System.setProperty("zap.proxy", "true")
      val options: ChromeOptions = browserFactory.chromeOptions(None)
      options.asMap().get("proxy").toString shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "return chromeOptions with zap proxy configuration when ZAP_HOST environment variable is set" in new Setup {
      override val browserFactory: BrowserFactory = new BrowserFactory() {
        override protected val zapHostInEnv: Option[String] = Some("localhost:11000")
      }

      val options: ChromeOptions = browserFactory.chromeOptions(None)
      //This check ensures zap configuration is not setup because of zap.proxy system property.
      sys.props.get("zap.proxy").isDefined  shouldBe false
      options.asMap().get("proxy").toString shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "return userBrowserOptions for Chrome" in new Setup {
      val customOptions = new ChromeOptions()
      customOptions.addArguments("headless")

      val options: ChromeOptions = browserFactory.chromeOptions(Some(customOptions))
      options.asMap().get("browserName")                 shouldBe "chrome"
      options.asMap().get("goog:chromeOptions").toString shouldBe "{args=[headless], extensions=[]}"
      options.asMap().getOrDefault("proxy", None)        shouldBe None
    }

    "return userBrowserOptions with zap proxy for Chrome" in new Setup {
      System.setProperty("zap.proxy", "true")
      val customOptions = new ChromeOptions()
      customOptions.addArguments("headless")

      val options: ChromeOptions = browserFactory.chromeOptions(Some(customOptions))
      options.asMap().get("browserName")                 shouldBe "chrome"
      options.asMap().get("goog:chromeOptions").toString shouldBe "{args=[headless], extensions=[]}"
      options.asMap().get("proxy").toString              shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "return userBrowserOptions with zap proxy for Chrome when ZAP_HOST environment variable is set" in new Setup {
      override val browserFactory: BrowserFactory = new BrowserFactory() {
        override protected val zapHostInEnv: Option[String] = Some("localhost:11000")
      }
      val customOptions                           = new ChromeOptions()
      customOptions.addArguments("headless")

      //This check ensures zap configuration is not setup because of zap.proxy system property.
      sys.props.get("zap.proxy").isDefined shouldBe false
      val options: ChromeOptions = browserFactory.chromeOptions(Some(customOptions))
      options.asMap().get("proxy").toString shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "return chromeOptions with javascript disabled when disable.javascript is true" in new Setup {
      System.setProperty("disable.javascript", "true")
      val options: ChromeOptions = browserFactory.chromeOptions(None)
      options
        .asMap()
        .get("goog:chromeOptions")
        .toString should include("prefs={profile.managed_default_content_settings.javascript=2}")
    }

    "return firefoxOptions" in new Setup {
      val options: FirefoxOptions = browserFactory.firefoxOptions(None)
      options.asMap().get("browserName") shouldBe "firefox"
    }

    "return firefoxOptions with zap proxy configuration when zap.proxy is true " in new Setup {
      System.setProperty("zap.proxy", "true")
      val options: FirefoxOptions = browserFactory.firefoxOptions(None)
      options.asMap().get("proxy").toString shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "return firefoxOptions with zap proxy configuration when ZAP_HOST environment variable is set" in new Setup {
      override val browserFactory: BrowserFactory = new BrowserFactory() {
        override protected val zapHostInEnv: Option[String] = Some("localhost:11000")
      }
      //This check ensures zap configuration is not setup because of zap.proxy system property.
      sys.props.get("zap.proxy").isDefined shouldBe false
      val options: FirefoxOptions                 = browserFactory.firefoxOptions(None)
      options.asMap().get("proxy").toString shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "return userBrowserOptions for firefox" in new Setup {
      val customOptions = new FirefoxOptions()
      customOptions.setHeadless(true)

      val options: FirefoxOptions = browserFactory.firefoxOptions(Some(customOptions))
      options.asMap().get("moz:firefoxOptions").toString shouldBe "{args=[-headless], prefs={}}"
      options.asMap().getOrDefault("proxy", None)        shouldBe None
    }

    "return userBrowserOptions with zap proxy for firefox" in new Setup {
      System.setProperty("zap.proxy", "true")

      val customOptions = new FirefoxOptions()
      customOptions.setHeadless(true)

      val options: FirefoxOptions = browserFactory.firefoxOptions(Some(customOptions))
      options.asMap().get("browserName")                 shouldBe "firefox"
      options.asMap().get("moz:firefoxOptions").toString shouldBe "{args=[-headless], prefs={}}"
      options.asMap().get("proxy").toString              shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "return userBrowserOptions with zap proxy for firefox when ZAP_HOST environment variable is set" in new Setup {
      override val browserFactory: BrowserFactory = new BrowserFactory() {
        override protected val zapHostInEnv: Option[String] = Some("localhost:11000")
      }

      val customOptions = new FirefoxOptions()
      customOptions.setHeadless(true)

      //This check ensures zap configuration is not setup because of zap.proxy system property.
      sys.props.get("zap.proxy").isDefined shouldBe false
      val options: FirefoxOptions = browserFactory.firefoxOptions(Some(customOptions))
      options.asMap().get("browserName")                 shouldBe "firefox"
      options.asMap().get("moz:firefoxOptions").toString shouldBe "{args=[-headless], prefs={}}"
      options.asMap().get("proxy").toString              shouldBe "Proxy(manual, http=localhost:11000)"
    }

    "configure browser with ZAP_HOST when both ZAP_HOST and zap.proxy  is set" in new Setup {
      System.setProperty("zap.proxy", "true")
      override val browserFactory: BrowserFactory = new BrowserFactory() {
        override protected val zapHostInEnv: Option[String] = Some("localhost:1234")
      }

      val options: ChromeOptions = browserFactory.chromeOptions(None)
      sys.props.get("zap.proxy")            shouldBe Some("true")
      options.asMap().get("proxy").toString shouldBe "Proxy(manual, http=localhost:1234)"
    }

    "throw an exception when ZAP_HOST environment is not of the format 'localhost:port'" in new Setup {
      override val browserFactory: BrowserFactory = new BrowserFactory() {
        override protected val zapHostInEnv: Option[String] = Some("localhost:abcd")
      }
      intercept[ZapConfigurationException] {
        browserFactory.chromeOptions(None)
      }
    }

    "return chromeOptions with accessibility extension configuration when system property accessibility.test true" in new Setup {
      System.setProperty("accessibility.test", "true")
      val options: ChromeOptions = browserFactory.chromeOptions(None)

      options
        .asMap()
        .get("goog:chromeOptions")
        .toString shouldBe "{args=[start-maximized, --use-cmd-decoder=validating, --use-gl=desktop], excludeSwitches=[enable-automation], extensions=[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], useAutomationExtension=false}"
    }

    "return chromeOptions without accessibility extension configuration when system property accessibility.test false and system env ACCESSIBILITY_TEST false" in new Setup {
      val options: ChromeOptions = browserFactory.chromeOptions(None)

      options
        .asMap()
        .get("goog:chromeOptions")
        .toString shouldBe "{args=[start-maximized, --use-cmd-decoder=validating, --use-gl=desktop], excludeSwitches=[enable-automation], extensions=[], useAutomationExtension=false}"
    }

    "return error when using firefoxOptions and when configuring system property accessibility.test true" in new Setup {
      val thrown: Exception = intercept[Exception] {
        System.setProperty("accessibility.test", "true")
        browserFactory.firefoxOptions(None)
      }
      assert(
        thrown.getMessage === s"Failed to configure Firefox browser to run accessibility-assessment tests." +
          s" The accessibility-assessment can only be configured to run with Chrome."
      )
    }

    "return firefoxOptions with javascript disabled configuration when disable.javascript is true " in new Setup {
      System.setProperty("disable.javascript", "true")
      val options: FirefoxOptions = browserFactory.firefoxOptions(None)
      println(options.asMap())
      options.asMap().get("moz:firefoxOptions").toString should include("javascript.enabled=false")
    }

    "return error when using chromeOptions headless and when configuring system property accessibility.test true" in new Setup {
      val thrown: Exception = intercept[Exception] {
        System.setProperty("accessibility.test", "true")
        val customOptions = new ChromeOptions()
        customOptions.setHeadless(true)
        browserFactory.chromeOptions(Some(customOptions))
      }
      assert(
        thrown.getMessage === browserFactory.accessibilityInHeadlessChromeNotSupported
      )
    }

    "return error when browser type is headless-chrome and when configuring system property accessibility.test true" in new Setup {
      val thrown: Exception = intercept[Exception] {
        System.setProperty("accessibility.test", "true")
        browserFactory.createBrowser(Some("headless-chrome"), None)
      }
      assert(
        thrown.getMessage === browserFactory.accessibilityInHeadlessChromeNotSupported
      )
    }
  }
}
