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

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

/**
 * Created by user on 07/06/16.
 */
class MessageSpec extends UnitSpec with MockitoSugar {

  val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  val childDOB = LocalDate.parse("2012-01-28", formatter)
  val emailData = Message(emailAddress = "test@example.com", dob = Some(List(childDOB)),  england = false)

  "Message data should not be null" in {
    emailData should not be null
  }

  "Message data email should not be null if email is present" in {
    emailData.emailAddress should not be null
  }

  "Message data dob should not be null if dob is present" in {
    emailData.dob should not be null
  }

  "Message dob should be null if dob is null" in {
    val emailData = Message(emailAddress = "test@example.com", dob = None,  england = true)
    emailData.dob shouldBe None
  }

  "Message data should not be null if dob is null" in {
    val emailData = Message(emailAddress = "test@example.com", dob = None, england = false)
    emailData should not be null
  }

  "Message freen entitlement should be true if we receive it has true" in {
    val emailData = Message(emailAddress = "test@example.com", dob = None, england = true)
    emailData.england shouldBe true
  }
}
