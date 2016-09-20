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

package repositories

import models.Message
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class MessageRepositorySpec extends UnitSpec with MockitoSugar with MongoSpecSupport with WithFakeApplication{

  private trait Setup {
    val individualRepo = new MessageRepository {}
    await(individualRepo.drop)
  }

  val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  val childDOB = LocalDate.parse("2012-01-28", formatter)

  "The MessageRepository storeMessage method" should {
    "store the email" in new Setup  {
      val result = individualRepo.storeMessage(Message("test@example.com", None, false))
      await(result)
      await(individualRepo.count) shouldBe 1
      val savedEmail: Message = result
      savedEmail.emailAddress should not be empty
      savedEmail.emailAddress shouldBe ("test@example.com")
      savedEmail.dob shouldBe None
    }

    "store the email with more than one date of birth" in new Setup  {
      val result = individualRepo.storeMessage(Message("test1@example.com", Some(List(childDOB, LocalDate.parse("2014-03-04", formatter))), true))
      await(result)
      await(individualRepo.count) shouldBe 1
      val savedEmail: Message = result
      savedEmail.emailAddress should not be empty
      savedEmail.emailAddress shouldBe ("test1@example.com")
      savedEmail.dob shouldBe (Some(List(childDOB, LocalDate.parse("2014-03-04", formatter))))
    }

    "store the email and date of birth" in new Setup {
      val result = individualRepo.storeMessage(Message("test2@example.com", Some(List(childDOB)), true))
      await(result)
      await(individualRepo.count) shouldBe 1
      val savedEmail: Message = result
      savedEmail.emailAddress should not be empty
      savedEmail.emailAddress shouldBe "test2@example.com"
      savedEmail.dob shouldBe (Some(List(childDOB)))
    }

    "store the email when dob is None" in new Setup {
      val result = individualRepo.storeMessage(Message("test3@example.com", None, england = false))
      await(result)
      await(individualRepo.count) shouldBe 1
      val savedEmail: Message = result
      savedEmail.emailAddress should not be empty
      savedEmail.emailAddress shouldBe "test3@example.com"
    }

  }
}
