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

package services

import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.OneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication
import repositories.{RegistartionRepository, MessageRepository}
import uk.gov.hmrc.lock.LockRepository
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._
import scala.concurrent.Future

class SchedulerServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

//  "SchedulerService" when {
//
//    "call lockEmails" should {
//
//      val csiFakeApp = new GuiceApplicationBuilder(
//        disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
//      ).configure(
//        Map(
//          "Test.mongodb.uri" -> "mongodb://localhost:27017/cc-email-capture",
//          "mail.source" -> "childcare-schemes-interest-frontend"
//        )
//      ).build()
//
//      val ccFakeApp = new GuiceApplicationBuilder(
//        disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
//      ).configure(
//        Map(
//          "Test.mongodb.uri" -> "mongodb://localhost:27017/cc-email-capture",
//          "mail.source" -> "cc-frontend"
//        )
//      ).build()
//
//      val ccCSIFakeApp = new GuiceApplicationBuilder(
//        disabled = Seq(classOf[com.kenshoo.play.metrics.PlayModule])
//      ).configure(
//        Map(
//          "Test.mongodb.uri" -> "mongodb://localhost:27017/cc-email-capture",
//          "mail.source" -> "childcare-schemes-interest-frontend,cc-frontend"
//        )
//      ).build()
//
//      val csiEmails = List(
//        "test_email@test.test",
//        "test_csi_email_1@test.test",
//        "test_csi_email_2@test.test"
//      )
//
//      val ccEmails = List(
//        "test_email@test.test",
//        "test_cc_email_1@test.test",
//        "test_cc_email_2@test.test"
//      )
//
//
//      "return csi emails if mailSource contains only childcare-schemes-interest-frontend" in running(csiFakeApp) {
//        val service = new SchedulerService {
//          override val mongoConnectionUri: String = ""
//          override val registartionRepository: RegistartionRepository = mock[RegistartionRepository]
//          override val messageRepository: MessageRepository = mock[MessageRepository]
//          override val emailService: EmailService = mock[EmailService]
//          override val lockRepository: LockRepository = mock[LockRepository]
//        }
//
//        when(
//          service.registartionRepository.getEmails()
//        ).thenReturn(
//          Future.successful(csiEmails)
//        )
//        val result = await(service.getEmailsList())
//        result shouldBe csiEmails
//      }
//
//
//
//      "return cc emails if mailSource contains only cc-frontend" in running(ccFakeApp) {
//        val service = new SchedulerService {
//          override val mongoConnectionUri: String = ""
//          override val registartionRepository: RegistartionRepository = mock[RegistartionRepository]
//          override val messageRepository: MessageRepository = mock[MessageRepository]
//          override val emailService: EmailService = mock[EmailService]
//          override val lockRepository: LockRepository = mock[LockRepository]
//        }
//
//        when(
//          service.messageRepository.getEmails()
//        ).thenReturn(
//          Future.successful(ccEmails)
//        )
//        val result = await(service.getEmailsList())
//        result shouldBe ccEmails
//      }
//
//
//      "return cc and csi emails if mailSource contains both childcare-schemes-interest-frontend and cc-frontend" in running(ccCSIFakeApp) {
//        val service = new SchedulerService {
//          override val mongoConnectionUri: String = ""
//          override val registartionRepository: RegistartionRepository = mock[RegistartionRepository]
//          override val messageRepository: MessageRepository = mock[MessageRepository]
//          override val emailService: EmailService = mock[EmailService]
//          override val lockRepository: LockRepository = mock[LockRepository]
//        }
//
//        when(
//          service.registartionRepository.getEmails()
//        ).thenReturn(
//          Future.successful(csiEmails)
//        )
//
//        when(
//          service.messageRepository.getEmails()
//        ).thenReturn(
//          Future.successful(ccEmails)
//        )
//        val result = await(service.getEmailsList())
//        result shouldBe (csiEmails ++ ccEmails).distinct
//      }
//    }
//
//  }
}
