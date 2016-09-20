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

import play.api.libs.json.{JsResult, JsValue, Json, Reads}

case class CallBackEvent(eventType: String)

case class CallBackEventList(callBackEvents: List[CallBackEvent])

object CallBackEvent {
  val reader = new Reads[CallBackEvent] {
    def reads(js: JsValue): JsResult[CallBackEvent] = {
      for {
        eventType <- (js \ "event").validate[String]
      } yield {
        CallBackEvent(
          eventType = eventType
        )
      }
    }
  }
  implicit val formats = Json.format[CallBackEvent]
}


object CallBackEventList {

  val reader = new Reads[CallBackEventList] {
    def reads(js: JsValue): JsResult[CallBackEventList] = {
      for {
        callBackEvents <- (js \ "events").validate[List[CallBackEvent]](Reads.list(CallBackEvent.reader))
      } yield {
        CallBackEventList(callBackEvents = callBackEvents)
      }
    }
  }

  implicit val formats = Json.format[CallBackEventList]
}
