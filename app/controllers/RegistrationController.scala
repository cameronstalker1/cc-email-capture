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
import services.EmailService
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RegistrationController extends RegistrationController {
  override val emailService = EmailService
}

trait RegistrationController extends BaseController with ServicesConfig {
  val emailService: EmailService

  def register = Action.async(parse.json[JsObject]) {
    implicit request =>
      request.body.asOpt[Registration] match {
        case Some(registration) => {
          emailService.validEmail(registration.emailAddress).map { validationResult =>
            validationResult.status match {
              case OK => Ok // TODO
              case _ => {
                Logger.warn(s"\n ========= SubscribeController: Checking email return status: ${validationResult.status} ========= \n")
                NotFound(s"Checking email returned status: ${validationResult.status}")
              }
            }
          }.recover {
            case ex: Exception => {
              Logger.warn(s"\n ========= SubscribeController: Exception while checking email: ${ex.getMessage} ========= \n")
              InternalServerError("Exception while checking email")
            }
          }
        }
        case _ => {
          Logger.warn("\n ========= SubscribeController: Empty/Invalid JSON received ========= \n")
          Future.successful(
            BadRequest("Empty/Invalid JSON received")
          )
        }
      }

  }

}
