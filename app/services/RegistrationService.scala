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
import models.Registration
import play.api.Logger
import reactivemongo.api.FailoverStrategy
import repositories._
import uk.gov.hmrc.mongo.SimpleMongoConnection
import uk.gov.hmrc.play.config.RunMode
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RegistrationService extends RegistrationService with RunMode {
  override val mongoConnectionUri: String = ApplicationConfig.mongoConnectionUri
  override val registrationRepository: RegistrationRepository = new RegistrationRepository
}

trait RegistrationService extends SimpleMongoConnection {
  val failoverStrategy: Option[FailoverStrategy] = None
  val registrationRepository: RegistrationRepository

  def countSentEmails(): Future[List[String]] = {
    registrationRepository.countSentEmails().map(res => res).recover {
      case ex: Exception => {
        Logger.error(s"Exception counting sent emails from csi: ${ex.getMessage}")
        List.empty
      }
    }
  }

  def countEmails(withDOB: Boolean): Future[Int] = {
    registrationRepository.countEmails(withDOB).recover {
      case ex: Exception => {
        Logger.error(s"Exception counting emails with DOB = ${withDOB} from csi: ${ex.getMessage}")
        -1
      }
    }
  }

  def getEmailCount(): Future[Int] = {
    registrationRepository.getEmailCount().map(res => res).recover {
      case ex => {
        Logger.error(s"\n ========= RegistrationService: EmailCount failed with exception ${ex.getMessage} ========= \n")
        -1
      }
    }
  }

  def getLocationCount(): Future[Map[String, Int]] = {
    registrationRepository.getLocationCount().map(res => res).recover {
      case ex => {
        Logger.error(s"\n ========= RegistrationService: LocationCount failed with exception ${ex.getMessage} ========= \n")
        Map.empty
      }
    }
  }

  def insertOrUpdate(registration: Registration): Future[Boolean] = {
    registrationRepository.inserOrUpdate(registration).map(res => res).recover {
      case ex => {
        Logger.error(s"\n ========= RegistrationService: InserOrUpdate failed with exception ${ex.getMessage} ========= \n")
        false
      }
    }
  }

}
