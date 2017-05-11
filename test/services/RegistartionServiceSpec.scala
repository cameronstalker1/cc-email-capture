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

import fixtures.RegistrationData
import models.Registration
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import repositories.RegistrationRepository
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.Future

class RegistartionServiceSpec extends UnitSpec with MockitoSugar with RegistrationData {

  val registartionService: RegistrationService = new RegistrationService {
    override val registrationRepository: RegistrationRepository = mock[RegistrationRepository]
    override val mongoConnectionUri: String = ""
  }

  "insertOrUpdate" should {

    val results: List[(Boolean, String, Future[Boolean])] = List(
      (true, "true", Future.successful(true)),
      (false, "false", Future.successful(false)),
      (false, "exception", Future.failed(new RuntimeException))
    )

    results.foreach { case (res, message, repositoryResult) =>
      s"return ${res} if RegistrationRepository.insertOrUpdate returns ${message}" in {
        when(
          registartionService.registrationRepository.inserOrUpdate(any[Registration]())
        ).thenReturn(
          repositoryResult
        )

        val result = await(registartionService.insertOrUpdate(registration))
        result shouldBe res
      }
    }

  }

  "countEmails" should {

    val results: List[(Int, String, Future[Int], Boolean)] = List(
      (1, "1", Future.successful(1), true),
      (2, "2", Future.successful(2), true),
      (-1, "exception", Future.failed(new RuntimeException), true),
      (1, "1", Future.successful(1), false),
      (2, "2", Future.successful(2), false),
      (-1, "exception", Future.failed(new RuntimeException), false)
    )

    results.foreach { case (res, message, repositoryResult, withDOB) =>
      s"return ${res} if RegistrationRepository.countEmails returns ${message} when with dob = ${withDOB}" in {
        when(
          registartionService.registrationRepository.countEmails(anyBoolean())
        ).thenReturn(
          repositoryResult
        )

        val result = await(registartionService.countEmails(withDOB))
        result shouldBe res
      }
    }

  }

}
