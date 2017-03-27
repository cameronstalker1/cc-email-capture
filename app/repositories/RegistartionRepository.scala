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
import config.ApplicationConfig._
import models.Registration
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.api.{DB, ReadPreference}
import reactivemongo.bson.{BSONDocument, BSONObjectID, _}
import reactivemongo.core.commands.{Aggregate, GroupField, SumValue}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistartionRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[Registration, BSONObjectID](csiRegistrationCollection, mongo, Registration.registrationFormat,
    ReactiveMongoFormats.objectIdFormats)  {

  def inserOrUpdate(registration: Registration): Future[Boolean] = {
    collection.update(
      selector = BSONDocument("emailAddress" -> registration.emailAddress),
      update = registration,
      upsert = true
    ).map { result =>
      result.ok
    }

  }

  def getEmailCount(): Future[Int] = {
    collection.count().map{ result => result}
  }

  def getLocationCount() : Future[Map[String, Int]]  = {

    val countries : List[String] = List("england", "wales", "northern-ireland", "scotland")

    val statusZeroCounts = countries.map(_ -> 0).toMap

    val processingLocationMap: Reads[(String, Int)] = (
      (JsPath \ "_id").read[String] and
        (JsPath \ "count").read[Int]
      ).tupled

    collection.db.command(
      Aggregate(
        collection.name,
        Seq(GroupField("location")("count" -> SumValue(1)))
      ),
      ReadPreference.secondaryPreferred
    ).map(locationMap => statusZeroCounts ++ locationMap.toSeq.map(Json.toJson(_).as(processingLocationMap)))

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

  private def filterByCountries() = {
    if(ApplicationConfig.mailCountries.isSuccess) {
      Json.obj(
        "location" -> Json.obj(
          "$in" -> ApplicationConfig.mailCountries.get
        )
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

  private def filterBySent() = {
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
        "permanentbounce" -> Json.obj(
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
    val excludeSentEmails = filterBySent()
    val excludeDelivered = filterByDelivered()
    val excludeBounce = filterByBounce()
    collection.find(
      countries ++ startPeriod.deepMerge(endPeriod) ++ excludeSentEmails ++ excludeDelivered ++ excludeBounce
      ).cursor[Registration]().collect[List]().map(
      _.map(
        _.emailAddress
      )
    )
  }

}
