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

import models.Registration
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import config.ApplicationConfig._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RegistartionRepository()(implicit mongo: () => DB)
  extends ReactiveRepository[Registration, BSONObjectID](registrationCollection, mongo, Registration.registrationFormat, ReactiveMongoFormats.objectIdFormats)  {

  def inserOrUpdate(registration: Registration): Future[Boolean] = {

    collection.update(
      selector = BSONDocument("emailAddress" -> registration.emailAddress),
      update = registration,
      upsert = true
    ).map { result =>
      result.ok
    }

  }

}
