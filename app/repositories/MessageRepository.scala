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

package repositories

import org.joda.time.LocalDateTime
import config.ApplicationConfig
import models.{Registration, Message}
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

  def countEmails(withDOB: Boolean): Future[Int] = {
    val filter = Json.obj(
      "dob" -> Json.obj(
        "$exists" -> withDOB
      )
    )
    collection.count(Some(filter))
  }

  def storeMessage(message: Message): Future[Message] = {
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

  def markEmailAsSent(email: String): Future[Boolean] = {
    val selector = Json.obj(
      "emailAddress" -> email
    )
    val update = Json.obj(
      "$push" -> Json.obj(
        "sent" -> LocalDateTime.now().toString
      )
    )
    collection.update(selector, update).map { result =>
      result.ok
    }
  }

  def emailStatus(email: String, statuses: List[String]) : Future[Boolean] = {
    val localDateTime = LocalDateTime.now().toString
    val selector = Json.obj(
      "emailAddress" -> email
    )
    var statusesList = Json.obj()
    for(status <- statuses) {
      statusesList = statusesList ++ Json.obj(
        status -> localDateTime
      )
    }
    val update = Json.obj(
      "$push" -> statusesList
    )
    collection.update(selector, update).map { result =>
      result.ok
    }
  }


  private def filterByCountries() = {
    if(ApplicationConfig.mailCountries.isSuccess &&
      !(ApplicationConfig.mailCountries.get.contains("england") && ApplicationConfig.mailCountries.get.exists(_ != "england"))
    ) {
      Json.obj(
        "england" -> ApplicationConfig.mailCountries.get.contains("england")
      )
    }
    else {
      Json.obj()
    }
  }

  private def filterByStartDate() = {
    if(ApplicationConfig.mailStartDate.isSuccess) {
      Json.obj(
        "dob" -> Json.obj(
          "$elemMatch" -> Json.obj(
            "$gte" -> ApplicationConfig.mailStartDate.get
          )
        )
      )
    }
    else {
      Json.obj()
    }
  }

  private def filterByEndDate() = {
    if(ApplicationConfig.mailEndDate.isSuccess) {
      Json.obj(
        "dob" -> Json.obj(
          "$elemMatch" -> Json.obj(
            "$lte" -> ApplicationConfig.mailEndDate.get
          )
        )
      )
    }
    else {
      Json.obj()
    }
  }

  private def filterByExcludeSent() = {
    if(ApplicationConfig.mailExcludeSent) {
      Json.obj(
        "sent" -> Json.obj(
          "$exists" -> false
        )
      )
    }
    else {
      Json.obj()
    }
  }

  private def filterByDelivered() = {
    if(ApplicationConfig.mailExcludeDelivered && ApplicationConfig.mailDeliveredStatuses.isSuccess) {
      val deliveredStatuses = ApplicationConfig.mailDeliveredStatuses.get
      Json.obj(
        "$or" -> {
          for (status <- deliveredStatuses) yield Json.obj(
            status -> Json.obj(
              "$exists" -> false
            )
          )
        }
      )
    }
    else {
      Json.obj()
    }
  }

  private def filterByBounce() = {
    if(ApplicationConfig.mailExcludeBounce) {
      Json.obj(
        "PermanentBounce" -> Json.obj(
          "$exists" -> false
        )
      )
    }
    else {
      Json.obj()
    }
  }

  def getEmails(): Future[List[String]] = {
    val countries = filterByCountries()
    val startPeriod = filterByStartDate()
    val endPeriod = filterByEndDate()
    val excludeSentEmails = filterByExcludeSent()
    val excludeDelivered = filterByDelivered()
    val excludeBounce = filterByBounce()
    val filter = countries ++ startPeriod.deepMerge(endPeriod) ++ excludeSentEmails ++ excludeDelivered ++ excludeBounce
    collection.find(filter).cursor[Message]().collect[List]().map(
      _.map(
        _.emailAddress
      )
    )
  }

}
