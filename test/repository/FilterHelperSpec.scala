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

package repository

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import repositories.FilterHelper
import uk.gov.hmrc.play.test.UnitSpec

class FilterHelperSpec extends UnitSpec with MockitoSugar {
  val helper = new FilterHelper {}

  "builfAgeFilter" when {

    val currentDate = LocalDate.parse("2017-07-06")

    val sentMails = List(true, false)

    sentMails.foreach { sentMail =>
      s"sentMails is ${sentMail}" should {

        "return None if age is not given" in {
          helper.builfAgeFilter(None, sentMail, currentDate) shouldBe Json.obj(
            "sent" -> Json.obj(
              "$exists" -> sentMail
            )
          )
        }

        "return dob > '2015-07-06' and dob <= '2016-07-06' if looking for 1 years old on 2017-07-06" in {
          helper.builfAgeFilter(Some(1), sentMail, currentDate) shouldBe Json.obj(
            "sent" -> Json.obj(
              "$exists" -> sentMail
            ),
            "dob" -> Json.obj(
              "$elemMatch" -> Json.obj(
                "$gt" -> "2015-07-06",
                "$lte" -> "2016-07-06"
              )
            )
          )
        }

        "return dob > '2014-07-06' and dob <= '2015-07-06' if looking for 2 years old on 2017-07-06" in {
          helper.builfAgeFilter(Some(2), sentMail, currentDate) shouldBe Json.obj(
            "sent" -> Json.obj(
              "$exists" -> sentMail
            ),
            "dob" -> Json.obj(
              "$elemMatch" -> Json.obj(
                "$gt" -> "2014-07-06",
                "$lte" -> "2015-07-06"
              )
            )
          )
        }
      }
    }

  }

}
