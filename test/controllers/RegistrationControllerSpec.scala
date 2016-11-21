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

package controllers

import com.kenshoo.play.metrics.PlayModule
import fixtures.RegistrationData
import models.Registration
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.Play
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.{AuditEvents, RegistartionService, EmailService}
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import play.api.test.Helpers._
import scala.concurrent.Future
import Play.current

class RegistrationControllerSpec extends UnitSpec with MockitoSugar with RegistrationData with WithFakeApplication {

  override def bindModules = Seq(new PlayModule)

  val fakeRequest: FakeRequest[_] = FakeRequest()
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "verify that controller is set up correctly" should {

    "use rhe right EmailService" in {
      RegistrationController.emailService shouldBe EmailService
    }

    "use rhe right RegistartionService" in {
      RegistrationController.registartionService shouldBe RegistartionService
    }

  }

  "subscribe" should {

    val registrationController: RegistrationController = new RegistrationController {
      override val emailService = mock[EmailService]
      override val registartionService: RegistartionService = mock[RegistartionService]
      override val auditService: AuditEvents = mock[AuditEvents]

      override def processRegistration(registration: Registration, host: String)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Ok)
    }

    invalidPayloads.foreach { payload =>
      s"return BAD_REQUEST for invalid payload: '${payload.toString()}'" in {
        implicit val materializer = Play.application.materializer
        val result = await(registrationController.register()(fakeRequest.withBody(payload)))
        status(result) shouldBe BAD_REQUEST
        bodyOf(result) shouldBe "Empty/Invalid JSON received"
      }
    }

    "return the result of processRegistration if valid payload is given" in {
      implicit val materializer = Play.application.materializer
      val result = await(registrationController.register()(fakeRequest.withBody(validPayload)))
      status(result) shouldBe OK
    }

  }

  "processRegistration" should {

    val registrationController: RegistrationController = new RegistrationController {
      override val emailService = mock[EmailService]
      override val registartionService: RegistartionService = mock[RegistartionService]
      override val auditService: AuditEvents = mock[AuditEvents]

      override def saveAndSendEmail(registration: Registration, host: String)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Accepted)
    }

    val testCases: List[(String, Future[HttpResponse], Int)] = List(
      ("return the result of saveAndSendEmail if validation is successful", Future.successful(HttpResponse(OK)), ACCEPTED),
      ("return the result of NOT_FOUND if validation failed", Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)), NOT_FOUND),
      ("return the result of INTERNAL_SERVER_ERROR if validation throws exception", Future.failed(new RuntimeException), INTERNAL_SERVER_ERROR)
    )

    testCases.foreach { case (testMessage, mockResponse, functionStatus) =>
      testMessage in {
        implicit val materializer = Play.application.materializer
        when(
          registrationController.emailService.validEmail(anyString())(any())
        ).thenReturn(
          mockResponse
        )
        val result = await(registrationController.processRegistration(registration, "host"))
        status(result) shouldBe functionStatus
      }
    }
  }

  "saveAndSendEmail" should {
    val registrationController: RegistrationController = new RegistrationController {
      override val emailService = mock[EmailService]
      override val registartionService: RegistartionService = mock[RegistartionService]
      override val auditService: AuditEvents = mock[AuditEvents]

      override def sendEmail(registration: Registration, host: String)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Accepted)
    }

    val testCases: List[(String, Future[Boolean], Registration, Int)] = List(
      ("return the result of sendEmail if saving data is successful", Future.successful(true), registration, ACCEPTED),
      ("return the result of sendEmail if saving data is successful and audit correctly", Future.successful(true), registrationWithoutDOB, ACCEPTED),
      ("return the result of INTERNAL_SERVER_ERROR if saving data failed", Future.successful(false), registration, INTERNAL_SERVER_ERROR)
    )

    testCases.foreach { case (testMessage, mockResponse, registrationData, functionStatus) =>
      testMessage in {
        implicit val materializer = Play.application.materializer
        when(
          registrationController.registartionService.insertOrUpdate(any[Registration])
        ).thenReturn(
          mockResponse
        )

        when(
          registrationController.registartionService.getEmailCount()
        ).thenReturn(
          Future.successful(1)
        )

        when(
          registrationController.registartionService.getLocationCount()
        ).thenReturn(
          Future.successful(Map("test"->2))
        )

        val result = await(registrationController.saveAndSendEmail(registrationData, "host"))
        status(result) shouldBe functionStatus
      }
    }
  }

  "sendEmail" should {
    val registrationController: RegistrationController = new RegistrationController {
      override val emailService = mock[EmailService]
      override val registartionService: RegistartionService = mock[RegistartionService]
      override val auditService: AuditEvents = mock[AuditEvents]
    }

    val testCases: List[(String, Future[HttpResponse], Int)] = List(
      ("return the result of OK if sending email returns OK", Future.successful(HttpResponse(ACCEPTED)), OK),
      ("return the result of BAD_GATEWAY if sending email returns BAD_GATEWAY", Future.successful(HttpResponse(BAD_GATEWAY)), BAD_GATEWAY),
      ("return the result of INTERNAL_SERVER_ERROR if sending email returns different result", Future.successful(HttpResponse(BAD_REQUEST)), INTERNAL_SERVER_ERROR)
    )

    testCases.foreach { case (testMessage, mockResponse, functionStatus) =>
      testMessage in {
        implicit val materializer = Play.application.materializer
        when(
          registrationController.emailService.sendRegistrationEmail(any[Registration], anyString)(any[HeaderCarrier])
        ).thenReturn(
          mockResponse
        )

        val result = await(registrationController.sendEmail(registration, "host"))
        status(result) shouldBe functionStatus
      }
    }

  }


}
