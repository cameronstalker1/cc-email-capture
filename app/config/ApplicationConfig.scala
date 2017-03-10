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

import org.joda.time.LocalDate
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import uk.gov.hmrc.play.config.ServicesConfig
import scala.util.Try

object ApplicationConfig extends ServicesConfig {

  private def getListString(key: String, delimiter: String): List[String] = getString(key).split(delimiter).toList

  def getEventType(eventType: String): Try[String] = Try(getString(eventType.toLowerCase))

  val ccEmailCollection: String = getString("settings.collections.cc")

  val csiRegistrationCollection: String = getString("settings.collections.csi")

  val mongoConnectionUri: String = getString(s"$env.mongodb.uri")

  val mailEnabled: Boolean = getBoolean("mail.enabled")

  val mailDateFormatter: DateTimeFormatter = DateTimeFormat.forPattern(getString("mail.date.format"))

  val mailStartDate: Try[LocalDate] = Try(LocalDate.parse(getString("mail.start.date"), mailDateFormatter))

  val mailEndDate: Try[LocalDate] = Try(LocalDate.parse(getString("mail.end.date"), mailDateFormatter))

  val mailCountries: Try[List[String]] = Try(getListString("mail.countries", ","))

  val mailExcludeSent: Boolean = getBoolean("mail.exclude.sent.emails")
  val mailExcludeDelivered: Boolean = getBoolean("mail.exclude.delivered.emails")
  val mailExcludeBounce: Boolean = getBoolean("mail.exclude.bounce.emails")

  def mailSource: List[String] = getListString("mail.source", ",")

  val mailTemplate: String = getString("mail.template")

  val mailSendingDelayMS: Int = getInt("mail.sending.delay.ms")
  val mailSendingIntervalSec: Int = getInt("mail.sending.interval.s")
  val mailLocking: Int = getInt("mail.locking.interval")

}
