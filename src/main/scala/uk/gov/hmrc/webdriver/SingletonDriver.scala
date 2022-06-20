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

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.{Capabilities, MutableCapabilities, WebDriver}

object SingletonDriver extends Driver

class Driver extends LazyLogging {
  private var instanceOption: Option[WebDriver] = None

  /*
   * Returns a WebDriver instance if already instantiated.  If no WebDriver instance is available, then one is instantiated
   * based on the value of the 'browser' system property.   Consumers of this method may also wish to provide their own 'customOptions'
   * rather than rely on the default browser options in this library.  Custom options will override the default options in this
   * library.
   */
  def getInstance(customOptions: Option[MutableCapabilities] = None): WebDriver = {
    if (instanceOption.isDefined && instanceOption.get.asInstanceOf[RemoteWebDriver].getSessionId == null)
      instanceOption = None

    instanceOption getOrElse initialiseBrowser(customOptions)
  }

  private def initialiseBrowser(customOptions: Option[MutableCapabilities]): WebDriver = {
    val browser: Option[String] = sys.props.get("browser").map(_.toLowerCase)
    val driver                  = new BrowserFactory().createBrowser(browser, customOptions)
    instanceOption = Some(driver)
    logDriverCapabilities(driver)
    driver
  }

  /*
   * Closes the browser and clears instanceOption
   */
  def closeInstance(): Unit =
    instanceOption foreach { instance =>
      instance.quit()
      instanceOption = None
    }

  private def logDriverCapabilities(driver: WebDriver): Unit = {
    val capabilities: Capabilities = driver.asInstanceOf[RemoteWebDriver].getCapabilities
    val browserName                = capabilities.getBrowserName
    val browserImage: String       = sys.env.getOrElse("BROWSER_IMAGE", browserName)

    logger.info(s"Browser info: $browserImage ${capabilities.getVersion}")

    browserName match {
      case "chrome"  => logger.info(s"Driver Version: ${capabilities.getCapability("chrome")}")
      case "firefox" => logger.info(s"Driver Version: ${capabilities.getCapability("moz:geckodriverVersion")}")
      case _         => logger.info(s"Browser Capabilities: $capabilities")
    }
  }
}
