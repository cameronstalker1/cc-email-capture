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

package services

import controllers.FakeCCEmailApplication
import models.Message
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import repositories.MessageRepository
import org.mockito.Mockito._
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent._

class MessageServiceSpec extends UnitSpec with MockitoSugar with FakeCCEmailApplication {

  val messageService = new MessageService {
    override val mongoConnectionUri = ""
    override val messageRepository = mock[MessageRepository]
  }


  "Message Service" should {
    "store a message containing email if the email doesn't exist in the database" in {
      when(
        messageService.messageRepository.storeMessage(Message("test1@example.com", None, true))
      ).thenReturn(
        Future.successful(Message("test1@example.com", england = true))
      )

      await(messageService.storeMessage(Message("test1@example.com", None, true)))
    }

    "update the record by erasing the dob if the dob is not selected" in {
      when(
        messageService.messageRepository.storeMessage(Message("test1@example.com", england = false))
      ).thenReturn(
        Future.successful(Message("test1@example.com", england = false))
      )

      await(messageService.storeMessage(Message("test1@example.com", england = false)))
    }

    "countEmails" should {

      val results: List[(Int, String, Future[Int], Boolean)] = List(
        (1, "1", Future.successful(1), true),
        (2, "2", Future.successful(2), true),
        (-1, "exception", Future.failed(new RuntimeException), true),
        (1, "1", Future.successful(1), false),
        (2, "2", Future.successful(2), false),
        (-1, "exception", Future.failed(new RuntimeException), false)
      )

      results.foreach { case (res, message, repositoryResult, withDOB) =>
        s"return ${res} if MessageRepository.countEmails returns ${message} when with dob = ${withDOB}" in {
          when(
            messageService.messageRepository.countEmails(anyBoolean())
          ).thenReturn(
            repositoryResult
          )

          val result = await(messageService.countEmails(withDOB))
          result shouldBe res
        }
      }

    }
    
  }
}
