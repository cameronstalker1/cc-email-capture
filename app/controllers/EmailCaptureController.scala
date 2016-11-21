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

import config.ApplicationConfig
import models.{CallBackEventList, EmailResponse, Message}
import play.api.Logger
import play.api.Play._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.JsValue
import play.api.mvc._
import reactivemongo.core.errors.ReactiveMongoException
import services.{AuditEvents, EmailService, MessageService}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.JsonConstructor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object EmailCaptureController extends EmailCaptureController {
  override val messageService = MessageService
  override val emailService = EmailService
  override val auditService = AuditEvents
}

trait EmailCaptureController extends BaseController with ServicesConfig {
  val messageService: MessageService
  val auditService: AuditEvents
  val emailService: EmailService

  def captureEmail : Action[JsValue]  = Action.async(parse.json) { implicit request =>
    val registrationData = request.body.asOpt[Message]
    registrationData match {
      case Some(data) => {
        emailService.validEmail(data.emailAddress).flatMap { result =>
          result.status match {
            case OK => storeAndSend(data, request.host)
            case NOT_FOUND => Future(NotFound)
            case BAD_GATEWAY => Future(BadGateway)
            case _ => Future(InternalServerError)
          }
        }.recover {
          case e: Exception => {
            Logger.warn(s"\n ========= EmailCaptureController: captureEmail Exception while checking email:${e.getMessage} ========= \n")
            auditService.sendServiceFailureEvent(data, e)
            InternalServerError
          }
        }
      }
      case None => {
        Logger.warn(s"\n ========= EmailCaptureController: Empty/Invalid JSON received ========= \n")
        auditService.emptyJSONEvent(new BadRequestException("Empty JSON data"))
        Future.successful(BadRequest)
      }
    }
  }

  def storeAndSend(message: Message, host : String)(implicit hc: HeaderCarrier): Future[Status] = {
    messageService.storeMessage(message).flatMap { result =>
      result match {
        case data: Message => {
          auditService.sendDataStoreSuccessEvent(message)
          emailService.sendEmail(message, host).map { emailResponse =>
            emailResponse.status match {
              case ACCEPTED =>
                auditService.sendEmailSuccessEvent(message)
                Ok
              case BAD_GATEWAY =>
                Logger.warn(s"******** storeAndSend: Bad Gateway Error ******")
                auditService.sendEmailFailureEvent(message)
                BadGateway
              case _ =>
                Logger.warn(s"******** storeAndSend: Internal Server Error ******")
                auditService.sendEmailFailureEvent(message)
                InternalServerError
            }
          }
        }
      }
    }.recover {
      case e: ReactiveMongoException => {
        Logger.warn(s"\n ========= EmailCaptureController: captureEmail ReactiveMongoException while storing data: ${e.getMessage} ========= \n")
        auditService.sendDataStoreFailureEvent(message, e)
        InternalServerError
      }
      case e: IllegalStateException => {
        Logger.warn(s"\n ========= EmailCaptureController: captureEmail IllegalStateException while storing data: ${e.getMessage} ========= \n")
        auditService.sendDataStoreFailureEvent(message, e)
        InternalServerError
      }
      case e: Exception => {
        Logger.warn(s"\n ========= EmailCaptureController: captureEmail Exception while storing data: ${e.getMessage} ========= \n")
        auditService.sendDataStoreFailureEvent(message, e)
        InternalServerError
      }
    }
  }

  private def getResponseJson(request: Request[AnyContent], responseFun: (JsValue) => Future[Result]): Future[Result] = {
    request.body match {
      case js: AnyContentAsJson =>
        responseFun.apply(js.json)
      case _ =>
        Logger.warn("Invalid request body type passed to microservice - just JSON accepted")
        Future.successful(InternalServerError(JsonConstructor.constructErrorJson(Messages("content_type.invalid"))))
    }
  }

  def receiveEvent(emailAddress: String, source: String) : Action[AnyContent] = Action.async {
    implicit request =>
      def response(requestJson: JsValue) = {

        Try(requestJson.as[CallBackEventList](CallBackEventList.reader).callBackEvents) match {
          case Success(callbackEventList) =>
            callbackEventList.foreach {
              event =>
                ApplicationConfig.getEventType(event.eventType) match {
                  case Success(_) =>
                    auditService.emailStatusEventForType(emailAddress + ":::" + event.eventType, source)
                  case _ =>
                    Logger.warn("No need to audit the Event Received: " + event.eventType)
                }
            }
            Future.successful(Ok)
          case Failure(e) =>
            Logger.warn("receiveEvent: Internal Server Error")
            Future.successful(InternalServerError(JsonConstructor.constructErrorResponse(EmailResponse
              (INTERNAL_SERVER_ERROR, Some(e.getMessage)))))
        }
      }
      getResponseJson(request, response)
  }
}
