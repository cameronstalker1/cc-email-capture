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
import org.joda.time.DateTime
import play.api.Logger
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.api._
import reactivemongo.core.commands.{FindAndModify, Update}
import play.api.libs.json._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessageRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[Message, BSONObjectID]("Email", mongo, Message.formats, ReactiveMongoFormats.objectIdFormats)   {

  def storeMessage(message: Message): Future[Message] = {
    Logger.info(s"\n ********** MessageRepository.storeMessage::: Message $message ********** \n")

    withCurrentTime { implicit time =>
      val update = updateQuery(message, time)
      val createEmailStore = FindAndModify(
        collection = collection.name,
        query = Json.obj("emailAddress" -> message.emailAddress).as[BSONDocument],
        modify = Update((update.as[BSONDocument]), fetchNewObject = true),
        upsert = true
      )

      collection.db.command(createEmailStore).map {
        emailStore =>
          Json.toJson(emailStore).as[Message]
      }
    }
  }

  private def updateQuery(emailData: Message, time: DateTime): JsObject = {
    Logger.info(s"\n ********** MessageRepository.updateQuery:::message $emailData ********** \n")

    emailData.dob.isDefined match {
      case true =>
        Json.obj("$setOnInsert" -> Json.obj("createdAt" -> time.toString),
          "$set" -> Json.obj ("emailAddress" -> emailData.emailAddress, "dob" -> emailData.dob, "updatedAt" -> time.toString, "england" -> emailData.england))
      case false =>
        Json.obj("$setOnInsert" -> Json.obj("createdAt" -> time.toString),
          "$set" -> Json.obj("emailAddress" -> emailData.emailAddress, "updatedAt" -> time.toString, "england" -> emailData.england),
          "$unset" -> Json.obj("dob" -> ""))

    }
  }

}
