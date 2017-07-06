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

  val mailEnabled: Boolean = getString("mail.enabled").toBoolean

  val mailDateFormatter: DateTimeFormatter = DateTimeFormat.forPattern(getString("mail.date.format"))

  val mailStartDate: Try[LocalDate] = Try(LocalDate.parse(getString("mail.start.date"), mailDateFormatter))

  val mailEndDate: Try[LocalDate] = Try(LocalDate.parse(getString("mail.end.date"), mailDateFormatter))

  val mailCountries: Try[List[String]] = Try(getListString("mail.countries", ","))

  val mailDeliveredStatuses: Try[List[String]] = Try(getListString("mail.delivered.statuses", ","))

  val mailExcludeSent: Boolean = getString("mail.exclude.sent.emails").toBoolean
  val mailExcludeDelivered: Boolean = getString("mail.exclude.delivered.emails").toBoolean
  val mailExcludeBounce: Boolean = getString("mail.exclude.bounce.emails").toBoolean
  val mailWithNODOB: Boolean = getString("mail.no.dob").toBoolean

  def mailSource: List[String] = getListString("mail.source", ",")

  val mailTemplate: String = getString("mail.template")

  val mailSendingDelayMS: Int = getString("mail.sending.delay.ms").toInt
  val mailSendingIntervalSec: Int = getString("mail.sending.interval.s").toInt
  val mailLocking: Int = getString("mail.locking.interval").toInt

  val mailCountEnabled: Boolean = getString("mail.count.enabled").toBoolean

  val mailCountByAgeEnabled: Boolean = getString("mail.count.by.age.enabled").toBoolean
  val mailCountAgesList: List[Int] = Try(getListString("mail.count.by.age.ages", ",").map(_.toInt)).getOrElse(List())
  val mailCountByAgeSentEmails: List[Boolean] = Try(getListString("mail.count.by.age.sent.emails", ",").map(_.toBoolean)).getOrElse(List())

}
