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
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.{MutableCapabilities, WebDriver}

import java.net.URL

class BrowserFactory extends LazyLogging {

  private val remoteUsername  =
    sys.props.getOrElse("remote.username", throw BrowserFactoryException("Remote username was not defined"))
  private val remoteAccessKey =
    sys.props.getOrElse("remote.accesskey", throw BrowserFactoryException("Remote access key was not defined"))

  def createBrowser(browser: Option[String], customOptions: Option[MutableCapabilities]): WebDriver =
    browser match {
      case Some("chrome")  => remoteWebDriver(chromeOptions(customOptions))
      case Some("edge")    => remoteWebDriver(edgeOptions(customOptions))
      case Some("firefox") => remoteWebDriver(firefoxOptions(customOptions))
      case Some(browser)   => throw BrowserFactoryException(s"Browser '$browser' is not supported")
      case None            => throw BrowserFactoryException("Browser was not defined")
    }

  private def remoteWebDriver(caps: MutableCapabilities): WebDriver = {
    val remoteService = sys.props.get("remote.service").map(_.toLowerCase)
    remoteService match {
      case Some("browserstack") => browserStack(caps)
      case Some("lambdatest")   => lambdaTest(caps)
      case Some("saucelabs")    => sauceLabs(caps)
      case Some("seleniumgrid") => seleniumGrid(caps)
      case Some(remoteService)  => throw BrowserFactoryException(s"Remote service '$remoteService' is not supported")
      case None                 => throw BrowserFactoryException("Remote service was not defined")
    }
  }

  private def chromeOptions(customOptions: Option[MutableCapabilities]): ChromeOptions =
    customOptions match {
      case Some(options) =>
        val caps: ChromeOptions = options.asInstanceOf[ChromeOptions]
        caps
      case None          =>
        val caps: ChromeOptions = new ChromeOptions()
        // set additional capabilities, add plugins, etc.
        caps
    }

  private def edgeOptions(customOptions: Option[MutableCapabilities]): EdgeOptions =
    customOptions match {
      case Some(options) =>
        val caps: EdgeOptions = options.asInstanceOf[EdgeOptions]
        caps
      case None          =>
        val caps: EdgeOptions = new EdgeOptions()
        // set additional capabilities, add plugins, etc.
        caps
    }

  private def firefoxOptions(customOptions: Option[MutableCapabilities]): FirefoxOptions =
    customOptions match {
      case Some(options) =>
        val caps: FirefoxOptions = options.asInstanceOf[FirefoxOptions]
        caps
      case None          =>
        val caps: FirefoxOptions = new FirefoxOptions()
        // set additional capabilities, add plugins, etc.
        caps
    }

  private def browserStack(caps: MutableCapabilities): WebDriver = {
    val url = s"https://$remoteUsername:$remoteAccessKey@hub-cloud.browserstack.com/wd/hub"

    caps.setCapability("browserstack.local", true)

    new RemoteWebDriver(new URL(url), caps)
  }

  private def lambdaTest(caps: MutableCapabilities): WebDriver = {
    val url = s"https://$remoteUsername:$remoteAccessKey@hub.lambdatest.com/wd/hub"

    caps.setCapability("tunnel", true)

    new RemoteWebDriver(new URL(url), caps)
  }

  private def sauceLabs(caps: MutableCapabilities): WebDriver = {
    val url = s"https://$remoteUsername:$remoteAccessKey@ondemand.eu-central-1.saucelabs.com:443/wd/hub"

    // When starting the Sauce Labs binary locally the tunnel name MUST match the value provided here
    caps.setCapability("tunnelIdentifier", "platui-poc")

    new RemoteWebDriver(new URL(url), caps)
  }

  private def seleniumGrid(caps: MutableCapabilities): WebDriver = {
    val url = "http://localhost:4444"

    new RemoteWebDriver(new URL(url), caps)
  }

  private case class BrowserFactoryException(exception: String) extends RuntimeException(exception)

}
