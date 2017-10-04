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

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.{Application, Configuration, Logger, Play}
import services.{MessageService, RegistrationService, SchedulerService}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config._
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override lazy val auditConnector: AuditConnector = MicroserviceAuditConnector
}

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with WSPatch with HttpPatch  with Hooks with AppName
object WSHttp extends WSHttp

object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig with WSHttp{
  override val authBaseUrl = baseUrl("auth")
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String) : Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) : Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport {
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = MicroserviceAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object CcGlobal extends DefaultMicroserviceGlobal with RunMode {


  override val auditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig("microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = Some(MicroserviceAuthFilter)

  override def onStart(app: Application): Unit = {
    super.onStart(app)

    if(ApplicationConfig.mailEnabled) {
      SchedulerService.getEmails()
    }

    if(ApplicationConfig.mailCountEnabled) {
      MessageService.countSentEmails().flatMap { calc =>
        Logger.warn(s"Number of emails sent from calculator: ${calc.size}")
        RegistrationService.countSentEmails().map { csi =>
          Logger.warn(s"Number of emails sent from csi: ${csi.size}")
          Logger.warn(s"Number of unique emails sent from both: ${(calc ++ csi).distinct.size}")
        }
      }
    }

    if(ApplicationConfig.mailCountByAgeEnabled) {
      ApplicationConfig.mailCountByAgeSentEmails.foreach { sentMails =>
        MessageService.getEmailsByAge(age = None, sentMails = sentMails).flatMap { calc =>
          Logger.warn(s"Age: all, sent: ${sentMails} from cc: ${calc.size}")
          RegistrationService.getEmailsByAge(age = None, sentMails = sentMails).map { csi =>
            Logger.warn(s"Age: all, sent: ${sentMails} from csi: ${csi.size}")
            Logger.warn(s"Age: all, sent: ${sentMails} unique from cc + csi: ${(calc ++ csi).distinct.size}")
          }
        }
        ApplicationConfig.mailCountAgesList.foreach { age =>
          MessageService.getEmailsByAge(age = Some(age), sentMails = sentMails).flatMap { calc =>
            Logger.warn(s"Age: ${age}, sent: ${sentMails} from cc: ${calc.size}")
            RegistrationService.getEmailsByAge(age = Some(age), sentMails = sentMails).map { csi =>
              Logger.warn(s"Age: ${age}, sent: ${sentMails} from csi: ${csi.size}")
              Logger.warn(s"Age: ${age}, sent: ${sentMails} unique from cc + csi: ${(calc ++ csi).distinct.size}")
            }
          }
        }
      }
    }


  }
}
