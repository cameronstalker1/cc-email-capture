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

import config.ApplicationConfig
import models.Message
import play.api.Logger
import play.api.libs.json.JsObject
import reactivemongo.api.FailoverStrategy
import uk.gov.hmrc.mongo.SimpleMongoConnection
import repositories.MessageRepository
import uk.gov.hmrc.play.config.RunMode
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MessageService extends MessageService with RunMode {
  override val mongoConnectionUri: String = ApplicationConfig.mongoConnectionUri
  override lazy val messageRepository = new MessageRepository
}

trait MessageService extends SimpleMongoConnection {
  val failoverStrategy: Option[FailoverStrategy] = None // use the default by supplying None (see ReactiveMongoHelper)
  val messageRepository: MessageRepository

  def getEmailsByAge(age: Option[Int], sentMails: Boolean): Future[List[String]] = {
    messageRepository.getEmailsByAge(age, sentMails)
  }

  def storeMessage(message: Message) : Future[Message] = {
    messageRepository.storeMessage(message)
  }

  def countSentEmails(): Future[List[String]] = {
    messageRepository.countSentEmails().map(res => res).recover {
      case ex: Exception => {
        Logger.error(s"Exception counting sent emails from cc: ${ex.getMessage}")
        List.empty
      }
    }
  }

  def countEmails(withDOB: Boolean): Future[Int] = {
    messageRepository.countEmails(withDOB).recover {
      case ex: Exception => {
        Logger.error(s"Exception counting emails with DOB = ${withDOB} from calculator: ${ex.getMessage}")
        -1
      }
    }
  }
}
