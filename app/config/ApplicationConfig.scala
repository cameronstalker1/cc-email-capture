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

package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig
import scala.util.Try

object ApplicationConfig extends ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  lazy val registrationCollection = Try(loadConfig("settings.registrationCollection")).getOrElse("")

  lazy val mongoConnectionUri = Try(loadConfig(s"$env.mongodb.uri")).getOrElse("")

  def getEventType(eventType: String): Try[String] = Try(loadConfig(eventType.toLowerCase))

}
