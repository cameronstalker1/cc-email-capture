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

import models.Message
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.http.HeaderCarrier

object AuditEvents extends AuditEvents {
  override lazy val customAuditConnector = AuditService
}

trait AuditEvents {

  val customAuditConnector: AuditService

  def sendDataStoreSuccessEvent(userData: Message)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent(AuditTypes.Tx_SUCCESSFUL, Map(("user-data" -> userData.toString())))

  def emailStatusEventForType(emailStatus: String, source: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit= {
    source match {
      case "childcare-interest" => sendEmailStatusEventForInterest(emailStatus)
      case "cc-frontend" => sendEmailStatusEvent(emailStatus)
    }
  }

  def scheduledEmailsToSend(numberOfEmails: Int, source: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent(
      "scheduled-emails-to-send", Map(
        "numberOfEmails" -> numberOfEmails.toString,
        "source" -> source
      )
    )

  def sendingScheduledEmails(email: String, status: String, emailResponse: Option[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent(
      "scheduled-emails", Map(
        "email" -> email,
        "status" -> status,
        "emailResponse" -> emailResponse.getOrElse(-1).toString
      )
    )

  def sendEmailStatusEvent(emailStatus: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent("email-sent-status", Map(("email-sent-status" -> emailStatus)))

  def sendEmailStatusEventForInterest(emailStatus: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent("email-sent-status-for-interest", Map(("email-sent-status-for-interest" -> emailStatus)))

  def sendEmailSuccessEvent(userData: Message)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent("email-send-success", Map(("email-send-success" -> userData.toString())))

  def sendEmailLocationCount(userData: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent("childcare-schemes-interest-email-location-count", userData)

  def sendEmailSuccessEventForInterest(userData: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent("email-send-success-for-interest", Map(("email-send-success-for-interest" -> userData)))

  def sendDOB(userData: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent("dob", userData)

  def sendServiceFailureEvent(userData: Message, error: Throwable)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent(AuditTypes.Tx_FAILED, Map(("user-data" -> userData.toString()), ("error" -> error.toString())))

  def sendDataStoreFailureEvent(userData: Message, error: Throwable)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit =
    sendEvent(AuditTypes.Tx_FAILED, Map(("user-data" -> userData.toString()), ("error" -> error.toString())))

  def sendEmailFailureEvent(userData: Message)(implicit hc: HeaderCarrier, ec: ExecutionContext)  : Unit =
    sendEvent(AuditTypes.Tx_FAILED, Map(("user-data" -> userData.toString())))

  def emptyJSONEvent(error: Throwable)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Unit  =
    sendEvent(AuditTypes.Tx_FAILED, Map(("error" -> error.toString())))

  private def sendEvent(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    customAuditConnector.sendEvent(eventFor(auditType, detail))

  private def eventFor(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier) =
    DataEvent(
      auditSource = "cc-email-capture",
      auditType = auditType,
      tags = hc.headers.toMap,
      detail = detail)

  object AuditTypes {
    val Tx_FAILED = "TxFailed"
    val Tx_SUCCESSFUL = "TxSuccessful"
  }
}
