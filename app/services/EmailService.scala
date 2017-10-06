/*
 * Copyright 2017 HM Revenue & Customs
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

package services

import config.WSHttp
import models.{Registration, SendEmailRequest, Message, IsValidEmail}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadGatewayException, HeaderCarrier, HttpGet, HttpPost, HttpResponse }

/**
* Created by Povilas Lape on 4/8/15.
*/

object EmailService extends EmailService {
  override val httpGetRequest = WSHttp
  override val httpPostRequest = WSHttp
  override val serviceUrl = baseUrl("email")
}

trait EmailService extends ServicesConfig {
  val httpGetRequest: HttpGet
  val httpPostRequest: HttpPost
  val serviceUrl: String
  import MdcLoggingExecutionContext._

  def validEmail(email: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    httpGetRequest.GET[IsValidEmail](serviceUrl + s"/hmrc/validate-email-address?email=$email").map {
      result =>
        result.valid match {
          case true => HttpResponse(OK)
          case false =>
            Logger.warn(s"\n ========= EmailService.checkEmail: Email is not valid ========= \n")
            HttpResponse(NOT_FOUND)
      }
    } recover {
      case e : BadGatewayException =>
        Logger.warn(s"\n ========= EmailService.checkEmail: BadGatewayException while validating email" +
          s"mailgun microservice (check email): ${e.getMessage} ========= \n")
        HttpResponse(BAD_GATEWAY)
      case e : Exception =>
        Logger.warn(s"\n ========= EmailService.checkEmail: Exception while validating email mailgun microservice " +
          s"(check email): ${e.getMessage} ========= \n")
        HttpResponse(SERVICE_UNAVAILABLE)
    }
  }

  def sendEmail(userData: Message)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    send("childcare_registration_email", userData.emailAddress, "cc-frontend")
  }

  def sendRegistrationEmail(registrationData: Registration)(implicit hc: HeaderCarrier):
  Future[HttpResponse] = {
    send("childcare_schemes_interest_email", registrationData.emailAddress, "childcare-interest")
  }


  def send(templateId: String, email: String, source: String)(implicit hc: HeaderCarrier):
  Future[HttpResponse] = {

    val toList: List[EmailAddress] = List(EmailAddress(email))
    val params: Map[String, String] = Map("emailAddress" -> email)
    val eventUrl = Some(baseUrl("cc-email-capture") + controllers.routes.EmailCaptureController.receiveEvent(email, source).url)

    val emailData: SendEmailRequest = SendEmailRequest(
      to = toList,
      templateId = templateId,
      parameters = params,
      force = false,
      eventUrl = eventUrl
    )

    httpPostRequest.POST[SendEmailRequest, HttpResponse](serviceUrl + "/hmrc/email", emailData).map {
      result =>
        result
    } recover {
      case e : BadGatewayException =>
        Logger.error(s"\n ========= EmailService.sendEmail: BadGatewayException while sending email mailgun microservice " +
          s"(send email): ${e.getMessage} ========= \n")
        HttpResponse(BAD_GATEWAY)
      case e : Exception =>
        Logger.error(s"\n ========= EmailService.sendEmail: Exception while sending email mailgun microservice" +
          s" (send email): ${e.getMessage} ========= \n")
        HttpResponse(SERVICE_UNAVAILABLE)
    }
  }


}
