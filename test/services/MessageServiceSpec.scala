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

import models.Message
import org.specs2.mock.Mockito
import repositories.MessageRepository
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent._

class MessageServiceSpec extends UnitSpec with Mockito {

  "Message Service" should {
    "store a message containing email if the email doesn't exist in the database" in {
      val mockMessageRepository = mock[MessageRepository]

      val individualService = new MessageService {
        override val mongoConnectionUri = ""
        override val messageRepository = mockMessageRepository
      }

      mockMessageRepository.storeMessage(Message("test1@example.com", None, true)) returns Future.successful(Message("test1@example.com", england = true))

      await(individualService.storeMessage(Message("test1@example.com", None, true)))
    }

    "update the record by erasing the dob if the dob is not selected" in {
      val mockMessageRepository = mock[MessageRepository]

      val individualService = new MessageService {
        override val mongoConnectionUri = ""
        override val messageRepository = mockMessageRepository
      }

      mockMessageRepository.storeMessage(Message("test1@example.com", england = false)) returns Future.successful(Message("test1@example.com", england = false))

      await(individualService.storeMessage(Message("test1@example.com", england = false)))
    }
  }
}
