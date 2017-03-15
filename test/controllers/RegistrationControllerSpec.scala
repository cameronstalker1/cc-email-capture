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

package controllers

import fixtures.RegistrationData
import models.Registration
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.{AuditEvents, RegistartionService, EmailService}
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._
import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._

class RegistrationControllerSpec extends UnitSpec with MockitoSugar with RegistrationData with FakeCCEmailApplication {

  val fakeRequest: FakeRequest[_] = FakeRequest()

  "verify that controller is set up correctly" should {

    "use rhe right EmailService" in {
      new RegistrationController(applicationMessagesApi).emailService shouldBe EmailService
    }

    "use rhe right RegistrationService" in {
      new RegistrationController(applicationMessagesApi).registrationService shouldBe RegistartionService
    }

  }

  "subscribe" should {

    invalidPayloads.foreach { payload =>
      s"return BAD_REQUEST for invalid payload: '${payload.toString()}'" in {

        val registrationController: RegistrationController = new RegistrationController(applicationMessagesApi) {
          override val emailService: EmailService = mock[EmailService]
          override val registrationService: RegistartionService = mock[RegistartionService]
          override val auditService: AuditEvents = mock[AuditEvents]

          override def processRegistration(registration: Registration)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
        }

        val result = await(registrationController.register()(fakeRequest.withBody(payload)))
        status(result) shouldBe BAD_REQUEST
        bodyOf(result) shouldBe "Empty/Invalid JSON received"
      }
    }

    "return the result of processRegistration if valid payload is given" in {

      val registrationController: RegistrationController = new RegistrationController(applicationMessagesApi) {
        override val emailService: EmailService = mock[EmailService]
        override val registrationService: RegistartionService = mock[RegistartionService]
        override val auditService: AuditEvents = mock[AuditEvents]

        override def processRegistration(registration: Registration)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
      }

      val result = await(registrationController.register()(fakeRequest.withBody(validPayload)))
      status(result) shouldBe OK
    }

  }

  "processRegistration" should {

    val testCases: List[(String, Future[HttpResponse], Int)] = List(
      ("return the result of saveAndSendEmail if validation is successful", Future.successful(HttpResponse(OK)), ACCEPTED),
      ("return the result of NOT_FOUND if validation failed", Future.successful(HttpResponse(BAD_GATEWAY)), NOT_FOUND),
      ("return the result of INTERNAL_SERVER_ERROR if validation throws exception", Future.failed(new RuntimeException), BAD_GATEWAY)
    )

    testCases.foreach { case (testMessage, mockResponse, functionStatus) =>
      testMessage in {
        val registrationController: RegistrationController = new RegistrationController(applicationMessagesApi) {
          override val emailService: EmailService = mock[EmailService]
          override val registrationService: RegistartionService = mock[RegistartionService]
          override val auditService: AuditEvents = mock[AuditEvents]

          override def saveAndSendEmail(registration: Registration)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Accepted)
        }

        when(
          registrationController.emailService.validEmail(anyString())(any())
        ).thenReturn(
          mockResponse
        )
        val result = await(registrationController.processRegistration(registration))
        status(result) shouldBe functionStatus
      }
    }
  }

  "saveAndSendEmail" should {

    val testCases: List[(String, Future[Boolean], Registration, Int)] = List(
      ("return the result of sendEmail if saving data is successful", Future.successful(true), registration, ACCEPTED),
      ("return the result of sendEmail if saving data is successful and audit correctly", Future.successful(true), registration, ACCEPTED),
      ("return the result of INTERNAL_SERVER_ERROR if saving data failed", Future.successful(false), registration, SERVICE_UNAVAILABLE)
    )

    testCases.foreach { case (testMessage, mockResponse, registrationData, functionStatus) =>
      testMessage in {

        val registrationController: RegistrationController = new RegistrationController(applicationMessagesApi) {
          override val emailService: EmailService = mock[EmailService]
          override val registrationService: RegistartionService = mock[RegistartionService]
          override val auditService: AuditEvents = mock[AuditEvents]

          override def sendEmail(registration: Registration)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Accepted)
        }

        when(
          registrationController.registrationService.insertOrUpdate(any[Registration])
        ).thenReturn(
          mockResponse
        )

        when(
          registrationController.registrationService.getEmailCount()
        ).thenReturn(
          Future.successful(1)
        )

        when(
          registrationController.registrationService.getLocationCount()
        ).thenReturn(
          Future.successful(Map("test"->2))
        )

        val result = await(registrationController.saveAndSendEmail(registrationData))
        status(result) shouldBe functionStatus
      }
    }
  }

  "sendEmail" should {

    val testCases: List[(String, Future[HttpResponse], Int)] = List(
      ("return the result of OK if sending email returns OK", Future.successful(HttpResponse(ACCEPTED)), OK),
      ("return the result of BAD_GATEWAY if sending email returns BAD_GATEWAY", Future.successful(HttpResponse(BAD_GATEWAY)), BAD_GATEWAY),
      ("return the result of INTERNAL_SERVER_ERROR if sending email returns different result", Future.successful(HttpResponse(BAD_REQUEST)), SERVICE_UNAVAILABLE)
    )

    testCases.foreach { case (testMessage, mockResponse, functionStatus) =>
      testMessage in {
        val registrationController: RegistrationController = new RegistrationController(applicationMessagesApi) {
          override val emailService: EmailService = mock[EmailService]
          override val registrationService: RegistartionService = mock[RegistartionService]
          override val auditService: AuditEvents = mock[AuditEvents]
        }

        when(
          registrationController.emailService.sendRegistrationEmail(any[Registration])(any[HeaderCarrier])
        ).thenReturn(
          mockResponse
        )

        val result = await(registrationController.sendEmail(registration))
        status(result) shouldBe functionStatus
      }
    }

  }


}
