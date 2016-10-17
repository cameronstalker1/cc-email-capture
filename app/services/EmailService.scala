/*
 * Copyright 2016 HM Revenue & Customs
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
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import scala.concurrent.Future

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
    httpGetRequest.GET[IsValidEmail](serviceUrl + (s"/validate-email-address?email=$email")).map {
      result =>
        result.valid match {
          case true =>
            Logger.info(s"\n ========= EmailService.checkEmail: Email $email is valid ========= \n")
            HttpResponse.apply(OK)
          case false =>
            Logger.warn(s"\n ========= EmailService.checkEmail: Email $email is not valid ========= \n")
            HttpResponse.apply(NOT_FOUND)
      }
    } recover {
      case e : BadGatewayException =>
        Logger.warn(s"\n ========= EmailService.checkEmail: BadGatewayException while accessing" +
          s" mailgun microservice (check email): ${e.getMessage} ========= \n")
        HttpResponse.apply(BAD_GATEWAY)
      case e : Exception =>
        Logger.warn(s"\n ========= EmailService.checkEmail: Exception while accessing mailgun microservice " +
          s"(check email): ${e.getMessage} ========= \n")
        HttpResponse.apply(INTERNAL_SERVER_ERROR)
    }
  }

  def sendEmail(userData: Message, host : String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    send("childcare_registration_email", userData.emailAddress, host, "cc-frontend")
  }

  def sendRegistrationEmail(registrationData: Registration, host: String)(implicit hc: HeaderCarrier):
  Future[HttpResponse] = {

    send("childcare_schemes_interest_email", registrationData.emailAddress, host, "childcare-interest")
  }

  def send(templateId: String, email: String, host: String, source: String)(implicit hc: HeaderCarrier):
  Future[HttpResponse] = {

    val toList: List[EmailAddress] = List(EmailAddress(email))
    val params: Map[String, String] = Map("emailAddress" -> email)
    val eventUrl = Some("http://" + host + controllers.routes.EmailCaptureController.receiveEvent(email, source).url)

    val emailData: SendEmailRequest = SendEmailRequest(
      to = toList,
      templateId = templateId,
      parameters = params,
      force = false,
      eventUrl = eventUrl
    )

    httpPostRequest.POST[SendEmailRequest, HttpResponse](serviceUrl + "/send-templated-email", emailData).map {
      result =>
        Logger.info(s"\n ========= EmailService.sendEmail: ${email} sent successfully ========= \n")
        result
    } recover {
      case e : BadGatewayException =>
        Logger.error(s"\n ========= EmailService.sendEmail: BadGatewayException while accessing mailgun microservice " +
          s"(send email): ${e.getMessage} ========= \n")
        HttpResponse.apply(BAD_GATEWAY)
      case e : Exception =>
        Logger.error(s"\n ========= EmailService.sendEmail: Exception while accessing mailgun microservice" +
          s" (send email): ${e.getMessage} ========= \n")
        HttpResponse.apply(INTERNAL_SERVER_ERROR)
    }
  }


}
