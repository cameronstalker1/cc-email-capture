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

import play.Play
import models.Message
import play.api.Logger
import play.api.libs.json.JsObject
import reactivemongo.api.FailoverStrategy
import uk.gov.hmrc.mongo.SimpleMongoConnection
import repositories.MessageRepository
import uk.gov.hmrc.play.config.RunMode
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MessageService extends MessageService with RunMode {
  override val mongoConnectionUri: String = Play.application().configuration().getString(s"$env.mongodb.uri")
  override lazy val messageRepository = new MessageRepository
}

trait MessageService extends SimpleMongoConnection {
  val failoverStrategy: Option[FailoverStrategy] = None // use the default by supplying None (see ReactiveMongoHelper)
  val messageRepository: MessageRepository

  def storeMessage(message: Message) : Future[Message] = {
      Logger.info(s"\n ********** MessageService.storeMessage::: message->$message ********** \n")
      messageRepository.storeMessage(message)
    }
}
