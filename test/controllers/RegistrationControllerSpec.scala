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

import fixtures.RegistrationData
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.test.FakeRequest
import services.EmailService
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._
import scala.concurrent.Future

class RegistrationControllerSpec extends UnitSpec with MockitoSugar with RegistrationData {

  val fakeRequest: FakeRequest[_] = FakeRequest()
  val registrationController: RegistrationController = new RegistrationController {
    override val emailService = mock[EmailService]
  }

  "subscribe" should {

    invalidPayloads.foreach { payload =>
      s"return BAD_REQUEST for invalid payload: '${payload.toString()}'" in {
        val result = await(registrationController.register()(fakeRequest.withBody(payload)))
        status(result) shouldBe BAD_REQUEST
        bodyOf(result) shouldBe "Empty/Invalid JSON received"
      }
    }

    "return INTERNAL_SERVER_ERROR if email validation throws exception" in {
      when(
        registrationController.emailService.validEmail(anyString())(any())
      ).thenReturn(
        Future.failed(new RuntimeException)
      )
      val result = await(registrationController.register()(fakeRequest.withBody(validPayload)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      bodyOf(result) shouldBe "Exception while checking email"
    }

    "return NOT_FOUND if email validation returns any status different than OK" in {
      when(
        registrationController.emailService.validEmail(anyString())(any())
      ).thenReturn(
        Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))
      )
      val result = await(registrationController.register()(fakeRequest.withBody(validPayload)))
      status(result) shouldBe NOT_FOUND
      bodyOf(result) shouldBe "Checking email returned status: 500"
    }

    s"return OK for valid payload: '${validPayload}'" in {
      when(
        registrationController.emailService.validEmail(anyString())(any())
      ).thenReturn(
        Future.successful(HttpResponse(OK))
      )
      val result = await(registrationController.register()(fakeRequest.withBody(validPayload)))
      status(result) shouldBe OK
    }

  }

}
