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

import models.Registration
import play.api.Logger
import repositories._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object RegistartionService extends RegistartionService {
  override val registartionRepository: RegistartionRepository = Repositories.registartionRepository
}

trait RegistartionService {
  val registartionRepository: RegistartionRepository

  def insertOrUpdate(registration: Registration): Future[Boolean] = {
    registartionRepository.inserOrUpdate(registration).map(res => res).recover {
      case ex => {
        Logger.error(s"\n ========= RegistartionService: InserOrUpdate failed with exception ${ex.getMessage} ========= \n")
        false
      }
    }
  }

}
