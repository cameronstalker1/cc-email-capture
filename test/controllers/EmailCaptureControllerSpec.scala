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

package test.controllers

import controllers.{FakeCCEmailApplication, EmailCaptureController}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.Play
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import reactivemongo.core.errors.ReactiveMongoException
import services.{EmailService, AuditEvents, MessageService}
import uk.gov.hmrc.play.http.{InternalServerException, HttpResponse}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import org.mockito.Matchers.{eq => mockEq, _}
import scala.concurrent._
import models.Message
import scala.concurrent.ExecutionContext.Implicits.global
import Play.current

class EmailCaptureControllerSpec extends UnitSpec with MockitoSugar with FakeCCEmailApplication with ScalaFutures {

  private trait Setup  {
    val mockMessageService = mock[MessageService]
    val mockAuditService = mock[AuditEvents]
    val mockEmailService = mock[EmailService]

    val mockController = new EmailCaptureController {
      override val messageService = mockMessageService
      override val auditService = mockAuditService
      override val emailService = mockEmailService
    }
  }
  val fakeResponseInternalError = new HttpResponse {  override def status: scala.Int = Status.INTERNAL_SERVER_ERROR}
  val fakeResponseAccepted = new HttpResponse {  override def status: scala.Int = Status.ACCEPTED}
  val fakeResponseOk = new HttpResponse {  override def status: scala.Int = Status.OK}
  val fakeResponseNotFound = new HttpResponse {  override def status: scala.Int = Status.NOT_FOUND}
  val fakeResponseBadGateway = new HttpResponse {  override def status: scala.Int = Status.BAD_GATEWAY}
  val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  "Call captureEmail method" should {

    "use the correct email service" in {
      EmailCaptureController.emailService shouldBe EmailService
    }

    "use the correct message service" in {
      EmailCaptureController.messageService shouldBe MessageService
    }

    "use the correct audit service" in {
      EmailCaptureController.auditService shouldBe AuditEvents
    }

    "return 400 status when supplied with invalid json" in new Setup {
      val fakeBody = Json.obj(
        "invalidJson" -> "Invalid Json"
      )
      val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
      status(result) shouldBe 400
    }

    "return 404 status when supplied with a value in the email field and the email is invalid" in new Setup {
      val email = "test@gmail.com"
      val dateOfBirth1 = LocalDate.parse("2009-05-04", formatter)
      val dateOfBirth2 = LocalDate.parse("2009-09-06", formatter)
      val dobs =  List(dateOfBirth1, dateOfBirth2)
      val fakeBody = Json.obj(
        "emailAddress" -> email,
        "dateOfBirth" -> dobs,
        "england" -> true
      )
      when(mockController.emailService.validEmail(mockEq(email))(hc = any())).thenReturn(Future.successful(fakeResponseNotFound))
      when(mockController.messageService.storeMessage(Message(email, Some(dobs), england = true))).thenReturn(Future.successful(Message(email, england = true)))
      when(mockController.emailService.sendEmail(userData = mockEq(Message(email,Some(dobs), england = true)))(hc = any())).thenReturn(Future.successful(fakeResponseAccepted))
      val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
      status(result) shouldBe 404
    }

    "return 400 status when empty json data is passed" in new Setup {
      val fakeBody = Json.obj()
      val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
      status(result) shouldBe 400
    }

    "return 502 status when the email is valid but there is an RuntimeException with storing the message" in new Setup {
        val email = "test@gmail.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> false
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseOk))
        when(mockController.messageService.storeMessage(Message(email, england = false))).thenReturn(Future.failed(new Exception("There was an exception")))
        when(mockController.emailService.sendEmail(userData = mockEq(Message(email, england = false)))(hc = any())).thenReturn(Future.successful(fakeResponseAccepted))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 502
      }

      "return 502 status when there is an IllegalStateException with storing the message" in new Setup {
        val email = "test@gmail.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> false
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseOk))
        when(mockController.messageService.storeMessage(Message(email, england = false))).thenReturn(Future.failed(new IllegalStateException("There was an exception")))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 502
      }

      "return 503 status when the email is valid and there is an ReactiveMongoException with storing the message" in new Setup {
        val email = "test@gmail.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> false
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseOk))
        when(mockController.messageService.storeMessage(Message(email, england = false))).thenReturn(Future.failed(new ReactiveMongoException {
          override val message: String = "There was an exception"
        }))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 503
      }

      //EMAIL SERVICE
      "return 404 status when supplied with a value in the email field and the email is NOT valid" in new Setup {
        val email = "test@test.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> true
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseNotFound))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 404
      }

    "return 502 status when supplied with a value in the email field and is BadGateway exception" in new Setup {
      val email = "test@test.com"
      val fakeBody = Json.obj(
        "emailAddress" -> email,
        "england" -> true
      )
      when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseBadGateway))
      val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
      status(result) shouldBe 502
    }

    "return 503 status when supplied with a value in the email field and is other InternalServer exception" in new Setup {
      val email = "test@test.com"
      val fakeBody = Json.obj(
        "emailAddress" -> email,
        "england" -> true
      )
      when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseInternalError))
      val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
      status(result) shouldBe 503
    }

    "return 503 status when there is an RuntimeException when checking the email" in new Setup {
        val email = "test@gmail.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> false
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.failed(new Exception("There was an exception")))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 503
      }

      "return 503 status when there is an IllegalStateException when checking the email" in new Setup {
        val email = "test@gmail.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> false
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.failed(new IllegalStateException("There was an exception")))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 503
      }

      "return 502 status when there is an RuntimeException when sending the email" in new Setup {
        val email = "test@gmail.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> true
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseOk))
        when(mockController.messageService.storeMessage(Message(email, england = true))).thenReturn(Future.successful(Message(email, england = true)))
        when(mockController.emailService.sendEmail(userData = mockEq(Message(email, england = true)))(hc = any())).thenReturn(Future.failed(new IllegalStateException("There was an exception")))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 502
      }

    "return 502 status when there is an other InternalServerException while storing" in new Setup {
      val email = "test@gmail.com"
      val fakeBody = Json.obj(
        "emailAddress" -> email,
        "england" -> false
      )
      when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseOk))
      when(mockController.messageService.storeMessage(Message(email, england = false))).thenReturn(Future.failed(new IllegalStateException))
      when(mockController.emailService.sendEmail(userData = mockEq(Message(email, england = false)))(hc = any())).thenReturn(Future.failed(new InternalServerException("There was an exception")))
      val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
      status(result) shouldBe 502
    }

      "return 502 status when there is an IllegalStateException when sending the email" in new Setup {
        val email = "test@gmail.com"
        val fakeBody = Json.obj(
          "emailAddress" -> email,
          "england" -> true
        )
        when(mockController.emailService.validEmail(mockEq(email))(any())).thenReturn(Future.successful(fakeResponseOk))
        when(mockController.messageService.storeMessage(Message(email, england = true))).thenReturn(Future.successful(Message(email,  england = true)))
        when(mockController.emailService.sendEmail(userData = mockEq(Message(email,  england = true)))(hc = any())).thenReturn(Future.failed(new IllegalStateException("There was an exception")))
        val result = await(mockController.captureEmail()(FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(fakeBody)))
        status(result) shouldBe 502
      }
    }

  "Call captureEmail method" should {

    "return 200 status when supplied with a value in the email field" in new Setup {
      val email = "test@gmail.com"
      val dateOfBirth1 = LocalDate.parse("2009-05-04", formatter)
      val dateOfBirth2 = LocalDate.parse("2009-09-06", formatter)
      val dobs =  List(dateOfBirth1, dateOfBirth2)
      val fakeBody = Json.obj(
        "emailAddress" -> email,
        "england" -> true
      )
      when(mockController.messageService.storeMessage(Message(email, england = true))).thenReturn(Future(Future.successful(Message(email,  england = true))))
      when(mockController.emailService.sendEmail(userData = mockEq(Message(email, england = true)))(hc = any())).thenReturn(Future.successful(fakeResponseAccepted))
      val result = await(mockController.storeAndSend(Message(email, england = true))(hc = any()))
      status(result) shouldBe 200
    }

    "return 502 status when storing email fails" in new Setup {
      val email = "test@gmail.com"
      when(mockController.messageService.storeMessage(Message(email, england = true))).thenReturn(Future.failed(new Exception("There was an exception")))
      val result = await(mockController.storeAndSend(Message(email, england = true))(hc = any()))
      status(result) shouldBe 502
    }

    "return 502 status when supplied with a value in the email field but throws bad gateway error while sending email" in new Setup {
      val email = "test@gmail.com"
      when(mockController.messageService.storeMessage(Message(email, None, true))).thenReturn(Future.successful(Message(email,  england = true)))
      when(mockController.emailService.sendEmail(userData = mockEq(Message(email, england = true)))(hc = any())).thenReturn(Future.successful(fakeResponseBadGateway))
      val result = await(mockController.storeAndSend(Message(email, england = true))(hc = any()))
      status(result) shouldBe 502
    }

    "return 503 status when sending email fails" in new Setup {
      val email = "test@gmail.com"
      when(mockController.messageService.storeMessage(Message(email, england = false))).thenReturn(Future.successful(Message(email,  england = false)))
      when(mockController.emailService.sendEmail(userData = mockEq(Message(email, england = false)))(hc = any())).thenReturn(Future.successful(fakeResponseInternalError))
      val result = await(mockController.storeAndSend(Message(email, england = false))(hc = any()))
      status(result) shouldBe 503
    }

  }
  "Call receive event method" should {

    "receive event - return 200 status when a valid json is received with eventType as Sent" in new Setup {
      val callBackResponseJson = """{"events": [ {"event": "Sent", "detected": "2015-07-02T08:26:39.035Z" }]}"""
      val result = mockController.receiveEvent("test@test.com", "cc-frontend").apply(FakeRequest().withJsonBody(Json.parse(callBackResponseJson)))

      status(result) shouldBe 200
    }

    "receive event - return 200 status when a valid json is received with eventType as Not present in call back list" in new Setup {
      val callBackResponseJson = """{"events": [ {"event": "Bounce", "detected": "2015-07-02T08:26:39.035Z" }]}"""
      val result = mockController.receiveEvent("test@test.com", "cc-frontend").apply(FakeRequest().withJsonBody(Json.parse(callBackResponseJson)))

      status(result) shouldBe 200
    }

    "receive event - return 502 status when a invalid json is received" in new Setup {
      val callBackResponseJson = """{"eventInvalid": [ {"event": "Sent", "detected": "2015-07-02T08:26:39.035Z" }]}"""
      val result = mockController.receiveEvent("test@test.com", "cc-frontend").apply(FakeRequest().withJsonBody(Json.parse(callBackResponseJson)))

      status(result) shouldBe 502
    }

    "receive event - return 400 status when invalid content Type is received" in new Setup {
      val result = mockController.receiveEvent("test@test.com", "cc-frontend").apply(FakeRequest().withTextBody("You naughty!"))

      whenReady(result) {
        result =>
          status(result) shouldBe 400
          jsonBodyOf(result).toString shouldBe "{\"reason\":\"Invalid request content type\"}"
      }
    }

  }
}
