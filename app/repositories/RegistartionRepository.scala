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
  extends ReactiveRepository[Registration, BSONObjectID](registrationCollection, mongo, Registration.registrationFormat,
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

}
