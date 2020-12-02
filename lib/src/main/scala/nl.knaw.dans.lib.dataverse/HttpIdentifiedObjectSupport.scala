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

import better.files.File
import org.json4s.{ DefaultFormats, Formats }

import scala.util.Try

trait HttpIdentifiedObjectSupport extends HttpSupport {
  private implicit val jsonFormats: Formats = DefaultFormats

  protected val endPointBase: String
  protected val id: String
  protected val isPersistentId: Boolean

  protected def getVersioned[D: Manifest](endPoint: String, version: Version = Version.UNSPECIFIED): Try[DataverseResponse[D]] = {
    trace(endPoint, version)
    if (isPersistentId) super.get[D](s"$endPointBase/:persistentId/versions/${
      if (version == Version.UNSPECIFIED) ""
      else version
    }/${ endPoint }?persistentId=$id")
    else super.get[D](s"$endPointBase/$id/versions/${
      if (version == Version.UNSPECIFIED) ""
      else version
    }/${ endPoint }")
  }

  protected def postJsonUnversioned[D: Manifest](endPoint: String, body: String, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, queryParams)
    if (isPersistentId) super.postJson[D](s"${ endPointBase }/:persistentId/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }")(body)
    else super.postJson[D](s"${ endPointBase }/$id/${ endPoint }$queryString")(body)
  }

  protected def postFileUnversioned[D: Manifest](endPoint: String, optFile: Option[File], optMetadata: Option[String], queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, queryParams)
    if (isPersistentId) super.postFile2[D](s"${ endPointBase }/:persistentId/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }", optFile, optMetadata)
    else super.postFile2[D](s"${ endPointBase}/$id/${ endPoint }$queryString", optFile, optMetadata)
  }

}
