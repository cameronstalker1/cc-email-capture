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

package controllers

import models.Registration
import play.api.Logger
import play.api.libs.json.JsObject
import services.{AuditEvents, RegistartionService, EmailService}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RegistrationController extends RegistrationController {
  override val emailService: EmailService = EmailService
  override val registrationService: RegistartionService = RegistartionService
  override val auditService: AuditEvents = AuditEvents
}

trait RegistrationController extends BaseController with ServicesConfig {
  val emailService: EmailService
  val registrationService: RegistartionService
  val auditService: AuditEvents

  def register : Action[JsObject] = Action.async(parse.json[JsObject]) {
    implicit request =>
      request.body.asOpt[Registration] match {
        case Some(registration) => processRegistration(registration)
        case _ => {
          Logger.warn("\n ========= SubscribeController: Empty/Invalid JSON received ========= \n")
          Future.successful(BadRequest("Empty/Invalid JSON received"))
        }
      }
  }

  def processRegistration(registration: Registration)(implicit hc: HeaderCarrier): Future[Result] = {
    emailService.validEmail(registration.emailAddress).flatMap { validationResult =>
      validationResult.status match {
        case OK => saveAndSendEmail(registration)
        case _ => {
          Logger.warn(s"\n ========= SubscribeController: Checking email return status: ${validationResult.status} ========= \n")
          Future(NotFound(s"Not a valid email: ${validationResult.status}"))
        }
      }
    }.recover {
      case ex: Exception => {
        Logger.warn(s"\n ========= SubscribeController: Exception while checking email: ${ex.getMessage} ========= \n")
        BadGateway(s"Exception:: Email service unavailable::: ${ex.getMessage}")
      }
    }
  }

  def saveAndSendEmail(registration: Registration)(implicit hc: HeaderCarrier): Future[Result] = {
    registrationService.insertOrUpdate(registration).flatMap {
      case true => {
        registration.dob.map { dob =>
          auditService.sendDOB(Map("dob" -> dob.toString))
        }
        auditEmailLocationCount()
        sendEmail(registration)
      }
      case false => {
        Logger.warn(s"******** SubscribeController.saveAndSendEmail: Insert/Update failed ******")
        Future(ServiceUnavailable)
      }
    }
  }

  def sendEmail(registration: Registration)(implicit hc: HeaderCarrier): Future[Result] = {
    emailService.sendRegistrationEmail(registration).map { result =>
      result.status match {
        case ACCEPTED =>
          auditService.sendEmailSuccessEventForInterest(registration.toString)
          Ok
        case BAD_GATEWAY =>
          Logger.warn("******** SubscribeController.sendEmail: Bad Gateway Error ******")
          BadGateway
        case _ =>
          Logger.warn("******** SubscribeController.sendEmail: Service Unavailable Error ******")
          ServiceUnavailable
      }
    }
  }

  private def auditEmailLocationCount() (implicit hc: HeaderCarrier): Unit = {
    registrationService.getLocationCount().onSuccess {
      case locationCountMap if !locationCountMap.isEmpty =>
        registrationService.getEmailCount().onSuccess {
          case totalCount if totalCount >= 0 =>
            auditService.sendEmailLocationCount(
              locationCountMap.map(x => (x._1, x._2.toString)) ++ Map("email-count" -> totalCount.toString)
            )
        }
    }

  }

}
