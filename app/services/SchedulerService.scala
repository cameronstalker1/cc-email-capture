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
import models.EmailLock
import org.joda.time.Duration
import play.api.Logger
import play.libs.Akka
import reactivemongo.api.FailoverStrategy
import repositories.{MessageRepository, RegistartionRepository}
import uk.gov.hmrc.lock.LockRepository
import uk.gov.hmrc.mongo.SimpleMongoConnection
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import concurrent.duration._

object SchedulerService extends SchedulerService {
  override val mongoConnectionUri: String = ApplicationConfig.mongoConnectionUri
  override val registartionRepository: RegistartionRepository = new RegistartionRepository
  override val messageRepository: MessageRepository = new MessageRepository
  override val emailService: EmailService = EmailService
  override val lockRepository: LockRepository = new LockRepository
}

trait SchedulerService extends SimpleMongoConnection  {
  override val failoverStrategy: Option[FailoverStrategy] = None
  val registartionRepository: RegistartionRepository
  val messageRepository: MessageRepository
  val lockRepository: LockRepository
  val emailService: EmailService

  def getEmailsList(): Future[List[String]] = {
    if (ApplicationConfig.mailSource == List("childcare-schemes-interest-frontend")) {
      registartionRepository.getEmails().map { csiResult =>
        csiResult
      }.recover {
        case ex: Exception => {
          Logger.warn(s"Can't get csi emails: ${ex.getMessage}")
          List()
        }
      }
    } else if (ApplicationConfig.mailSource == List("cc-frontend")) {
      messageRepository.getEmails().map { ccResult =>
        ccResult
      }.recover {
        case ex: Exception => {
          Logger.warn(s"Can't get cc emails: ${ex.getMessage}")
          List()
        }
      }
    } else {
      registartionRepository.getEmails().flatMap { csiResult =>
        messageRepository.getEmails().map { ccResult =>
          (csiResult ++ ccResult).distinct
        }.recover {
          case ex: Exception => {
            Logger.warn(s"Can't get cc emails: ${ex.getMessage}")
            List()
          }
        }
      }.recover {
        case ex: Exception => {
          Logger.warn(s"Can't get csi emails: ${ex.getMessage}")
          List()
        }
      }
    }
  }

  def lockEmails(): Future[Option[List[String]]] = {
    val lock = EmailLock("emailLock", new Duration(ApplicationConfig.mailLocking), lockRepository)
    lock.tryToAcquireOrRenewLock {
      getEmailsList().map { result =>
        result
      }.recover {
        case ex: Exception => {
          Logger.warn(s"Can't lock emails: ${ex.getMessage}")
          List()
        }
      }
    }
  }

  def getEmails() = {
    lockEmails().map { emailsList =>
      sendEmail(emailsList)
    }.recover {
      case ex: Exception => {
        Logger.warn(s"Can't lock emails: ${ex.getMessage}")
      }
    }
  }

  def mailDelivered(email: String, status: List[String]) = {
    if(ApplicationConfig.mailSource.contains("cc-frontend")) {
      messageRepository.emailStatus(email, status).map {
        result => result
      }.recover {
        case ex: Exception => {
          Logger.warn(s"Can't update cc email: ${ex.getMessage}")
        }
      }
    }
    if(ApplicationConfig.mailSource.contains("childcare-schemes-interest-frontend")) {
      registartionRepository.emailStatus(email, status).map {
        result => result
      }.recover {
        case ex: Exception => {
          Logger.warn(s"Can't update csi email: ${ex.getMessage}")
        }
      }
    }
  }

  def sendEmail(emailsList: Option[List[String]]) = {
    if (emailsList.isDefined && emailsList.get.nonEmpty) {

      implicit val hc: HeaderCarrier = new HeaderCarrier()

      var emailsToSend = emailsList.get

      Akka.system.scheduler.schedule(ApplicationConfig.mailSendingDelayMS milliseconds, ApplicationConfig.mailSendingIntervalSec seconds) {
        if (emailsToSend.nonEmpty) {
          val email = emailsToSend.head
          emailService.send(ApplicationConfig.mailTemplate, email, "scheduler").map { result =>
            emailsToSend = emailsToSend.tail
            if(ApplicationConfig.mailSource.contains("cc-frontend")) {
              messageRepository.markEmailAsSent(email).recover {
                case ex: Exception => {
                  Logger.warn(s"Can't update cc emails: ${ex.getMessage}")
                }
              }
            }
            if(ApplicationConfig.mailSource.contains("childcare-schemes-interest-frontend")) {
              registartionRepository.markEmailAsSent(email).recover {
                case ex: Exception => {
                  Logger.warn(s"Can't update csi emails: ${ex.getMessage}")
                }
              }
            }
          }.recover {
            case ex: Exception => {
              Logger.warn(s"Can't send email: ${ex.getMessage}")
            }
          }
        }
      }
    }
  }

}
