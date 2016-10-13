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

package fixtures

import play.api.libs.json.{Json, JsObject}

trait RegistrationData {
  val validPayload: JsObject = Json.obj(
    "location" -> "england",
    "emailAddress" -> "example@example.example"
  )

  val invalidPayloads: List[JsObject] = List(
    Json.obj(),
    Json.obj("location" -> "england"),
    Json.obj("emailAddress" -> "example@example.example"),
    Json.obj("field1" -> "value1", "field2" -> "value2")
  )

}
