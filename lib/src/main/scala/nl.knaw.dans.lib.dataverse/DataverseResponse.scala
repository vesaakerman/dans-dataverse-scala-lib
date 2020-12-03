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
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataFieldSerializer
import org.json4s.native.JsonMethods
import org.json4s.{ DefaultFormats, Formats, JValue }
import scalaj.http.HttpResponse

import scala.util.Try

/**
 * Encapsulates a response message from Dataverse. This is often, but not always, a JSON document. This class gives access to
 * the response at the following levels as far as applicable (from lower to higher levels of abstraction): `httpResponse`, `string`,
 * `json`, `data`.
 *
 * For example if the response contains metadata about a dataset, you would use the [[data]] method to retrieve a model object that
 * provides easy access to that structure. If JSON is returned but it is not modelled by a case class in [[nl.knaw.dans.lib.dataverse.model]],
 * then you could use the `json` method to get the json4s AST and query that to get to the information you need. If the body contains
 * UTF-8 encoded plan text, use `string`. Finally, if the body contains binary data, use `httpResponse`.
 *
 * @param httpResponse the underlying raw HTTP response
 * @tparam D the model object type that can be extracted (if none is available, this is set to `Any`).
 */
case class DataverseResponse[D: Manifest] private[dataverse](httpResponse: HttpResponse[Array[Byte]]) {
  private implicit val jsonFormats: Formats = DefaultFormats + MetadataFieldSerializer

  /**
   * The body of the response, decoded as UTF-8 string.
   */
  def string: Try[String] = Try {
    new String(httpResponse.body, StandardCharsets.UTF_8) // TODO: attempt to get correct char encoding from Content-Type header?
  }

  /**
   * The body of the response, decoded as a `org.json4s.JValue`.
   */
  def json: Try[JValue] = {
    string.map(s => JsonMethods.parse(s))
  }

  /**
   * The corresponding model object extracted from the body.
   *
   * @see [[nl.knaw.dans.lib.dataverse.model]]
   */
  def data: Try[D] = {
    json
      .map(_.extract[DataverseMessage[D]])
      .map(_.data.get)
  }

  def message: Try[String] = {
    json
      .map(_.extract[DataverseMessage[D]])
      .map(_.message.get)
  }
}
