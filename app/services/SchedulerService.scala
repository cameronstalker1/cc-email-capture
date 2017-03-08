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
import controllers.routes
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
      }
    } else if (ApplicationConfig.mailSource == List("cc-frontend")) {
      messageRepository.getEmails().map { ccResult =>
        ccResult
      }
    } else {
      registartionRepository.getEmails().flatMap { csiResult =>
        messageRepository.getEmails().map { ccResult =>
          (csiResult ++ ccResult).distinct
        }
      }
    }
  }

  def lockEmails(): Future[Option[List[String]]] = {
    val lock = EmailLock("emailLock", new Duration(60000), lockRepository)
    lock.tryToAcquireOrRenewLock {
      getEmailsList().map(result => result)
    }
  }

  def getEmails() = {
    lockEmails().map { emailsList =>
      sendEmail(emailsList)
    }
  }

  def sendEmail(emailsList: Option[List[String]]) = {
    // Send email
    implicit val hc: HeaderCarrier = new HeaderCarrier()

    println("------------- emails: " + emailsList)


    import concurrent.duration._


    if (emailsList.isDefined && emailsList.get.nonEmpty) {
      var emailsToSend = emailsList.get

      def job(email: String) = {
        println("---------- send to: " + email)
        emailService.send(ApplicationConfig.mailTemplate, email, "", "").map { result =>
          emailsToSend = emailsToSend.tail
          if(ApplicationConfig.mailSource.contains("cc-frontend")) {
            messageRepository.markEmailAsSent(email)
          }
          if(ApplicationConfig.mailSource.contains("childcare-schemes-interest-frontend")) {
            registartionRepository.markEmailAsSent(email)
          }
        }. recover {
          case ex: Exception => {
            // TODO: Log exception
            Logger.error("Can't send email")
          }
        }
      }

      Akka.system.scheduler.schedule(10 milliseconds, 10 seconds) {
        if (emailsToSend.nonEmpty) {
          job(emailsToSend.head)
        }
      }
    }
  }

}
