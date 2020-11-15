/**
 * Copyright (C) 2020 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.dataverse.model.dataset

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.native.{ JsonMethods, Serialization }
import org.json4s.{ DefaultFormats, Formats, JArray, JString, JValue }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CompoundFieldBuilderSpec extends AnyFlatSpec with Matchers with DebugEnhancedLogging {
  implicit val jsonFormats: Formats = DefaultFormats

  private def getJson(v: Any): JValue = {
    val jsonString = Serialization.writePretty(v)
    debug(jsonString)
    JsonMethods.parse(jsonString)
  }

  "build" should "produce empty list of values if none were added" in {
    getJson(CompoundFieldBuilder("myCompoundField").build()) \ "value" shouldBe JArray(List.empty)
  }

  it should "set typeName to the specified id" in {
    val name = "myCompoundField"
    getJson(CompoundFieldBuilder(name).build()) \ "typeName" shouldBe JString(name)
  }

  it should "produce list with one value if one was added" in {
    val json = getJson(CompoundFieldBuilder("myCompoundField")
      .withSingleValueField("field1", "value1")
      .build())

    // TODO: add check
  }

  // TODO: add more tests. First read https://github.com/json4s/json4s to find efficient queries of JSON
}
