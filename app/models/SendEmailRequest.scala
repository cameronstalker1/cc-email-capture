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

package models

import play.api.libs.json.{JsPath, Format}
import play.api.libs.functional.syntax._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayJsonFormats.emailAddressReads
import uk.gov.hmrc.emailaddress.PlayJsonFormats.emailAddressWrites

case class SendEmailRequest(to: List[EmailAddress], templateId: String, parameters: Map[String, String], force: Boolean, eventUrl: Option[String] = None)

object SendEmailRequest {
  implicit val sendEmailRequestFormat: Format[SendEmailRequest] = (
      (JsPath \ "to").format[List[EmailAddress]] and
        (JsPath \ "templateId").format[String] and
          (JsPath \ "parameters").format[Map[String, String]] and
            (JsPath \ "force").format[Boolean] and
              (JsPath \ "eventUrl").formatNullable[String]
    )(SendEmailRequest.apply, unlift(SendEmailRequest.unapply))
}
