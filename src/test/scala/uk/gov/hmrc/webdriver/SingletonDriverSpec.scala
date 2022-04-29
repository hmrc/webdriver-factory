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
import org.openqa.selenium.remote.{LocalFileDetector, RemoteWebDriver}
import org.openqa.selenium.{Capabilities, WebDriver}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

class SingletonDriverSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  trait TestSetup extends Driver {}

  override def beforeEach(): Unit = {
    System.setProperty("browser", System.getProperty("browser", "chrome"))
    System.clearProperty("javascript")
  }

  override def afterEach(): Unit = {
    System.clearProperty("javascript")
    SingletonDriver.closeInstance()
  }

  "getInstance" should {

    "return a browser instance" in new TestSetup {
      val driver: WebDriver          = SingletonDriver.getInstance()
      val capabilities: Capabilities = driver.asInstanceOf[RemoteWebDriver].getCapabilities
      capabilities.getBrowserName                     shouldBe "chrome"
      capabilities.asMap().get("acceptInsecureCerts") shouldBe false
    }

    "return a browser instance only if webdriver sessionId is not null" in new TestSetup {
      var driver: WebDriver = SingletonDriver.getInstance()
      driver.asInstanceOf[RemoteWebDriver].getSessionId shouldNot be(null)

      driver.quit()
      driver.asInstanceOf[RemoteWebDriver].getSessionId shouldBe null

      driver = SingletonDriver.getInstance()
      driver.asInstanceOf[RemoteWebDriver].getSessionId shouldNot be(null)
    }

    "return a browser instance when userBrowserOptions is passed" in new TestSetup {
      val options: ChromeOptions     = new ChromeOptions()
      options.setAcceptInsecureCerts(true)
      val driver: WebDriver          = SingletonDriver.getInstance(Some(options))
      val capabilities: Capabilities = driver.asInstanceOf[RemoteWebDriver].getCapabilities
      capabilities.getBrowserName                     shouldBe "chrome"
      capabilities.asMap().get("acceptInsecureCerts") shouldBe true

    }

    "return an exception when browser property is not set" in new TestSetup {
      System.clearProperty("browser")
      val caught: BrowserCreationException = intercept[BrowserCreationException] {
        SingletonDriver.getInstance()
      }
      caught.getMessage shouldBe "'browser' property is not set, this is required to instantiate a Browser"
    }

    "return an exception when browser property has a value not defined" in new TestSetup {
      System.setProperty("browser", "undefined")
      val caught: BrowserCreationException = intercept[BrowserCreationException] {
        SingletonDriver.getInstance()
      }
      caught.getMessage shouldBe "'browser' property 'undefined' not supported by the webdriver-factory library."
    }

    "return a driver with local file detection enabled when a RemoteWebDriver instance is created" in {
      System.setProperty("browser", "remote-chrome")
      val driver: WebDriver = SingletonDriver.getInstance()

      driver.isInstanceOf[RemoteWebDriver]                                                 shouldBe true
      driver.asInstanceOf[RemoteWebDriver].getFileDetector.isInstanceOf[LocalFileDetector] shouldBe true
    }
  }
}
