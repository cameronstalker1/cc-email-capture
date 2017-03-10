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

package controllers

import com.kenshoo.play.metrics.PlayModule
import org.scalatest.Suite
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import akka.stream.Materializer
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait FakeCCEmailApplication extends OneAppPerSuite {
  this: Suite =>

  val config : Map[String, _] = Map(
    "csrf.sign.tokens" -> false,
    "Test.microservice.services.email.host" -> "localhost",
    "Test.microservice.services.email.port" -> "8300",
    "Test.mongodb.uri" -> "mongodb://localhost:27017/cc-email-capture",
    "mail.enabled" -> false
  )

  implicit override lazy val app: Application = new GuiceApplicationBuilder(
    disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
  ).configure(config)
    .build()

  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val messages: Messages = applicationMessages
  implicit val hc = HeaderCarrier()
}
