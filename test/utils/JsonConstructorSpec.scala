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

package utils

import controllers.FakeCCEmailApplication
import models.EmailResponse
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import play.api.i18n.Messages.Implicits.applicationMessagesApi

class JsonConstructorSpec extends UnitSpec with MockitoSugar with FakeCCEmailApplication {

  "construct error response when response has some error" in {
    val response = EmailResponse(500, Some("error occured"))
    val error = new JsonConstructor(applicationMessagesApi).constructErrorResponse(response)
    error.toString shouldBe  "{\"reason\":\"error occured\"}"
  }

  "construct error response when response has no error" in {
    val response = EmailResponse(500, None)
    val error = new JsonConstructor(applicationMessagesApi).constructErrorResponse(response)
    error.toString shouldBe  "{\"reason\":\"Unknown reason. Dependent system did not provide failure reason\"}"
  }

  "construct error using a string" in {
    val error = new JsonConstructor(applicationMessagesApi).constructErrorJson("Error")
    error.toString shouldBe  "{\"reason\":\"Error\"}"
  }

}
