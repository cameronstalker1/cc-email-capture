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

package test

import play.api.libs.iteratee.Input
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.test.FakeRequest

object JsonRequestHelper {

  import scala.concurrent.ExecutionContext.Implicits.global
  
  def executeAction(action: Action[JsValue], request: FakeRequest[_], payload: String) =
    action(request).feed(Input.El(payload.getBytes)).flatMap(_.run)
}
