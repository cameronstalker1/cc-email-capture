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

package services

import fixtures.RegistrationData
import models.Registration
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import repositories.RegistartionRepository
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.Future

class RegistartionServiceSpec extends UnitSpec with MockitoSugar with RegistrationData {

  val registartionService: RegistartionService = new RegistartionService {
    override val registartionRepository: RegistartionRepository = mock[RegistartionRepository]
  }

  val results: List[(Boolean, String, Future[Boolean])] = List(
    (true, "true", Future.successful(true)),
    (false, "false", Future.successful(false)),
    (false, "exception", Future.failed(new RuntimeException))
  )

  "insertOrUpdate" should {

    results.foreach { case (res, message, repositoryResult) =>
      s"return ${res} if RegistrationRepository returns ${message}" in {
        when(
          registartionService.registartionRepository.inserOrUpdate(any[Registration]())
        ).thenReturn(
          Future.successful(res)
        )

        val result = await(registartionService.insertOrUpdate(registration))
        result shouldBe res
      }
    }

  }

}
