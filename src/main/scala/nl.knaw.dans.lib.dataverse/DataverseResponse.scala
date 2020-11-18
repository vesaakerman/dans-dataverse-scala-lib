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
package nl.knaw.dans.lib.dataverse

import java.nio.charset.StandardCharsets

import nl.knaw.dans.lib.dataverse.model.DataverseMessage
import org.json4s.native.JsonMethods
import org.json4s.{ DefaultFormats, Formats, JValue }
import scalaj.http.HttpResponse

import scala.util.Try

case class DataverseResponse[P: Manifest](httpResponse: HttpResponse[Array[Byte]]) {
  private implicit val jsonFormats: Formats = new DefaultFormats {}

  def string: Try[String] = Try {
    new String(httpResponse.body, StandardCharsets.UTF_8)
  }

  def json: Try[JValue] = {
    string.map(s => JsonMethods.parse(s))
  }

  def data: Try[P] = {
    json
      .map(_.extract[DataverseMessage[P]])
      .map(_.data)
  }
}
