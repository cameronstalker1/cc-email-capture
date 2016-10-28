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
  override val registartionService: RegistartionService = RegistartionService
  override val auditService: AuditEvents = AuditEvents
}

trait RegistrationController extends BaseController with ServicesConfig {
  val emailService: EmailService
  val registartionService: RegistartionService
  val auditService: AuditEvents

  def register : Action[JsObject] = Action.async(parse.json[JsObject]) {
    implicit request =>

      request.body.asOpt[Registration] match {
        case Some(registration) => processRegistration(registration, request.host)
        case _ => {
          Logger.warn("\n ========= SubscribeController: Empty/Invalid JSON received ========= \n")
          Future.successful(
            BadRequest("Empty/Invalid JSON received")
          )
        }
      }
  }

  def processRegistration(registration: Registration, host: String)(implicit hc: HeaderCarrier): Future[Result] = {
//    saveAndSendEmail(registration, host)
    emailService.validEmail(registration.emailAddress).flatMap { validationResult =>
      validationResult.status match {
        case OK => saveAndSendEmail(registration, host)
        case _ => {
          Logger.warn(s"\n ========= SubscribeController: Checking email return status: ${validationResult.status} ========= \n")
          Future(NotFound(s"Checking email returned status: ${validationResult.status}"))
        }
      }
    }.recover {
      case ex: Exception => {
        Logger.error(s"\n ========= SubscribeController: Exception while checking email: ${ex.getMessage} ========= \n")
        InternalServerError("Exception while checking email")
      }
    }
  }

  def saveAndSendEmail(registration: Registration, host: String)(implicit hc: HeaderCarrier): Future[Result] = {
    registartionService.insertOrUpdate(registration).flatMap {
      case true => {

        registartionService.getLocationCount().onSuccess {
          case Some(x) =>
            val locationMap = x.map(x => (x._1, x._2.toString))
            registartionService.getEmailCount().onSuccess {
              case Some(x) =>
                auditService.sendEmailLocationCount(locationMap ++ Map("email-count"->x.toString))
            }
        }

        sendEmail(registration, host)
      }
      case false => {
        Logger.warn(s"******** SubscribeController.saveAndSendEmail: Inser/Update failed ******")
        Future(InternalServerError)
      }
    }
  }

  def sendEmail(registration: Registration, host: String)(implicit hc: HeaderCarrier): Future[Result] = {
    emailService.sendRegistrationEmail(registration, host).map { result =>
      result.status match {
        case ACCEPTED =>
          auditService.sendEmailSuccessEventForInterest(registration.toString)
          Ok
        case BAD_GATEWAY =>
          Logger.warn("******** SubscribeController.sendEmail: Bad Gateway Error ******")
          BadGateway
        case _ =>
          Logger.warn("******** SubscribeController.sendEmail: Internal Server Error ******")
          InternalServerError
      }
    }
  }

}
