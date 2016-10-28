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

import models.Registration
import play.Play
import play.api.Logger
import reactivemongo.api.FailoverStrategy
import repositories._
import uk.gov.hmrc.mongo.SimpleMongoConnection
import uk.gov.hmrc.play.config.RunMode
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RegistartionService extends RegistartionService with RunMode {
  override val mongoConnectionUri: String = Play.application().configuration().getString(s"$env.mongodb.uri")
  override val registartionRepository: RegistartionRepository = new RegistartionRepository
}

trait RegistartionService extends SimpleMongoConnection {
  val failoverStrategy: Option[FailoverStrategy] = None
  val registartionRepository: RegistartionRepository

  def getEmailCount(): Future[Int] = {
    registartionRepository.getEmailCount().map(res => res).recover {
      case ex => {
        Logger.error(s"\n ========= RegistartionService: EmailCount failed with exception ${ex.getMessage} ========= \n")
        -1
      }
    }
  }

  def getLocationCount(): Future[Map[String, Int]] = {
    registartionRepository.getLocationCount().map(res => res).recover {
      case ex => {
        Logger.error(s"\n ========= RegistartionService: LocationCount failed with exception ${ex.getMessage} ========= \n")
        Map.empty
      }
    }
  }

  def insertOrUpdate(registration: Registration): Future[Boolean] = {
    registartionRepository.inserOrUpdate(registration).map(res => res).recover {
      case ex => {
        Logger.error(s"\n ========= RegistartionService: InserOrUpdate failed with exception ${ex.getMessage} ========= \n")
        false
      }
    }
  }

}
