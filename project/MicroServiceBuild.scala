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

import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "cc-email-capture"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.PlayImport._
  import play.core.PlayVersion

  private val microServiceBootstrapVersion = "4.4.0"
  private val playHealthVersion = "1.1.0"
  private val playJsonLoggerVersion = "2.1.1"
  private val playReactiveMongoVersion = "4.8.0"
  private val emailAddressVersion = "1.1.0"
  private val playConfigVersion = "2.1.0"
  private val playAuthorisationVersion = "3.3.0"
  private val jsonEncryptionVersion = "2.0.0"

  private val hmrcTestVersion = "1.8.0"
  private val scalaTestVersion = "2.2.6"
  private val scalaTestPlusVersion = "1.2.0"
  private val pegDownVersion = "1.6.0"
  private val reactiveMongoTestVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactiveMongoVersion,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microServiceBootstrapVersion,
    "uk.gov.hmrc" %% "play-authorisation" % playAuthorisationVersion,
    "uk.gov.hmrc" %% "play-json-logger" % playJsonLoggerVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "emailaddress" % emailAddressVersion,
    "uk.gov.hmrc" %% "json-encryption" % jsonEncryptionVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegDownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "org.scalatestplus" %% "play" % scalaTestPlusVersion % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

