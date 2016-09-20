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

package models

import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

/**
 * Created by user on 07/06/16.
 */
class SendEmailRequestSpec extends UnitSpec with MockitoSugar {

  val toList: List[EmailAddress] = List(EmailAddress("test@example.com"))
  val params: Map[String, String] = Map("emailAddress" -> "test@example.com", "dob" -> "2016-06-15")
  val sendEmailRequest = SendEmailRequest(to = toList, templateId = "childcare_registration_email",  parameters = params, force = false)

  "send email data should not be null" in {
    sendEmailRequest should not be null
  }

  "send email data to list should not be null" in {
    sendEmailRequest.to should not be null
  }

  "send email data templateId should not be null" in {
    sendEmailRequest.templateId should not be null
  }

  "send email data parameters should be null" in {
    sendEmailRequest.parameters should not be null
  }

  "send email data force should not be null" in {
    sendEmailRequest.force shouldBe false
  }

  "verify the email data unapply" in {
    val outputJson = Json.parse(
      s"""
      {"to":["test@example.com"],"templateId":"childcare_registration_email","parameters":{"emailAddress":"test@example.com","dob":"2016-06-15"},"force":false}
       """.stripMargin
    )
    val output = Json.toJson[models.SendEmailRequest](sendEmailRequest) shouldBe outputJson
  }

  "verify the email data apply" in {
    val emailData = Json.parse(
      s"""
      {"to":["test@example.com"],"templateId":"childcare_registration_email","parameters":{"emailAddress":"test@example.com","dob":"2016-06-15"},"force":false}
       """.stripMargin
    )

    val outputEmailData = emailData.validate[SendEmailRequest]
    outputEmailData.get shouldBe sendEmailRequest

  }
}
