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

import fixtures.RegistrationData
import models.{Registration, SendEmailRequest}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Writes
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.test.Helpers._
import scala.concurrent.Future

class EmailServiceSpec extends UnitSpec with MockitoSugar with RegistrationData {

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "Calling Send" should {
    val mockPOST = mock[HttpPost]
    val emailService = new EmailService {

      override val httpGetRequest: HttpGet = mock[HttpGet]

      override val httpPostRequest: HttpPost = mockPOST

      override val serviceUrl: String = "service-url"
    }

    val testCases: List[(String, Future[HttpResponse], Int)] = List(
      ("send a succesful email", Future.successful(HttpResponse(OK)), OK),
      ("return BAD_GATEWAY if posts throws badGateway exception", Future.failed(new BadGatewayException("Bad gateway")), BAD_GATEWAY),
      ("return INTERNAL_SERVER_ERROR if posts throws any exception", Future.failed(new Exception("Exception")), INTERNAL_SERVER_ERROR)
    )

    testCases.foreach { case (testMessage, mockResult, expectedStatus) =>
        testMessage in {
          when(
            mockPOST.POST[SendEmailRequest, HttpResponse](anyString, any[SendEmailRequest], any[Seq[(String, String)]])(any[Writes[SendEmailRequest]], any[HttpReads[HttpResponse]], any[HeaderCarrier])
          ).thenReturn(
            mockResult
          )

          val result = await(emailService.send("templateID", registration.emailAddress, "host", "childcare-interest"))
          result.status shouldBe expectedStatus
        }
    }
  }

  "Calling send registration" should{
    val emailService = new EmailService {

      override val httpGetRequest: HttpGet = mock[HttpGet]

      override val httpPostRequest: HttpPost = mock[HttpPost]

      override val serviceUrl: String = "service-url"

      override def send(templateId: String, email: String, host: String, source: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = Future.successful(HttpResponse(OK))
    }

    "return the result of send" in{
      val result = emailService.sendRegistrationEmail(registration, "host")
      result.status shouldBe OK
    }

  }

}
