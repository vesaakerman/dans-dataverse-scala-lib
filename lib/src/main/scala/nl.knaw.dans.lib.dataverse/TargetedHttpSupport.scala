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

/**
 * HTTP support that targets a specific type of object, such as datasets. The target is specified with an ID.
 */
private[dataverse] trait TargetedHttpSupport extends HttpSupport {
  private implicit val jsonFormats: Formats = DefaultFormats

  protected val targetBase: String
  protected val id: String
  protected val isPersistentId: Boolean

  /**
   * Get a specific version of something.
   *
   * @param endPoint the API endpoint
   * @param version  the version or version label
   * @tparam D the type of model object to expect in the response message
   * @return
   */
  protected def getVersionedFromTarget[D: Manifest](endPoint: String, version: Version = Version.LATEST): Try[DataverseResponse[D]] = {
    trace(endPoint, version)
    if (isPersistentId) super.get[D](s"${ targetBase }/:persistentId/versions/${ version }/${ endPoint }",  Map("persistentId" -> id))
    else super.get[D](s"${ targetBase }/$id/versions/${ version }/${ endPoint }")
  }

  /**
   * Get something for which versions do not apply
   *
   * @param endPoint the API endpoint
   * @tparam D the type of model object to expect in the response message
   * @return
   */
  protected def getUnversionedFromTarget[D: Manifest](endPoint: String): Try[DataverseResponse[D]] = {
    trace(endPoint)
    if (isPersistentId) super.get[D](s"${ targetBase }/:persistentId/${ endPoint }", Map("persistentId" -> id))
    else super.get[D](s"${ targetBase }/$id/${ endPoint }")
  }

  protected def postJsonToTarget[D: Manifest](endPoint: String, body: String, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    if (isPersistentId) super.postJson[D](s"${ targetBase }/:persistentId/${ endPoint }")(body, Map("persistentId" -> id) ++ queryParams)
    else super.postJson[D](s"${ targetBase }/$id/${ endPoint }")(body, queryParams)
  }

  protected def postFileToTarget[D: Manifest](endPoint: String, optFile: Option[File], optMetadata: Option[String], queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, queryParams)
    if (isPersistentId) super.postFile[D](s"${ targetBase }/:persistentId/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }", optFile, optMetadata)
    else super.postFile[D](s"${ targetBase }/$id/${ endPoint }$queryString", optFile, optMetadata)
  }

  protected def putToTarget[D: Manifest](endPoint: String, body: String, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, body, queryParams)
    if (isPersistentId) super.put[D](s"${ targetBase }/:persistentId/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }")(body)
    else super.put[D](s"${ targetBase }/$id/${ endPoint }$queryString")(body)
  }

  protected def deleteAtTarget[D: Manifest](endPoint: String, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, queryParams)
    if (isPersistentId) super.deletePath[D](s"${ targetBase }/:persistentId/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }")
    else super.deletePath[D](s"${ targetBase }/$id/${ endPoint }$queryString")
  }
}
