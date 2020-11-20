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

import java.net.URI

import better.files.File
import nl.knaw.dans.lib.dataverse.model.DataMessage
import nl.knaw.dans.lib.dataverse.model.dataset.{ DatasetVersion, DataverseFile, VERSION_LATEST }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import scalaj.http.HttpResponse

import scala.util.Try

/**
 * Functions that operate on a single dataset. See [[https://guides.dataverse.org/en/latest/api/native-api.html#datasets]].
 *
 */
class Dataset private[dataverse](id: String, isPersistentId: Boolean, configuration: DataverseInstanceConfig) extends HttpSupport with DebugEnhancedLogging {
  protected val connectionTimeout: Int = configuration.connectionTimeout
  protected val readTimeout: Int = configuration.readTimeout
  protected val baseUrl: URI = configuration.baseUrl
  protected val apiToken: String = configuration.apiToken
  protected val apiVersion: String = configuration.apiVersion

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-json-representation-of-a-dataset]]
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-version-of-a-dataset]]
   *
   * @param version version to view
   * @return
   */
  def view(version: Option[String] = None): Try[DataverseResponse[model.dataset.DatasetVersion]] = {
    trace(version)
    if (isPersistentId) get2[model.dataset.DatasetVersion](s"datasets/:persistentId/versions/${ version.getOrElse(VERSION_LATEST) }/?persistentId=$id")
    else get2[model.dataset.DatasetVersion](s"datasets/$id/versions/${ version.getOrElse(VERSION_LATEST) }")
  }

  /**
   * Almost the same as [[Dataset.view]] except that `viewLatestVersion` returns a JSON object that starts at the dataset
   * level instead of the dataset version level. The dataset level contains some field, most of wich are replicated at the dataset version level, however.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-json-representation-of-a-dataset]]
   *
   * @return
   */
  def viewLatestVersion(version: Option[String] = None): Try[DataverseResponse[model.dataset.DatasetLatestVersion]] = {
    trace(version)
    if (isPersistentId) get2[model.dataset.DatasetLatestVersion](s"datasets/:persistentId/?persistentId=$id")
    else get2[model.dataset.DatasetLatestVersion](s"datasets/$id")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-versions-of-a-dataset]]
   *
   * @return
   */
  def viewAllVersions(): Try[DataverseResponse[List[DatasetVersion]]] = {
    trace(())
    if (isPersistentId) get2[List[DatasetVersion]](s"datasets/:persistentId/versions?persistentId=$id")
    else get2[List[DatasetVersion]](s"datasets/$id/versions")
  }

  /**
   * Since the export format is generally not JSON you cannot use the [[DataverseResponse.json]] and [[DataverseResponse.data]]
   * on the result. You should instead use [[DataverseResponse.string]].
   *
   * Note that this API does not support specifying a version.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#export-metadata-of-a-dataset-in-various-formats]]
   *
   * @param format the export format
   * @return
   */
  def exportMetadata(format: String): Try[DataverseResponse[Any]] = {
    trace(())
    get2[Any](s"datasets/export/?exporter=$format&persistentId=$id")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-files-in-a-dataset]]
   *
   * @param version the version of the dataset
   * @return
   */
  def listFiles(version: Option[String] = None): Try[DataverseResponse[List[DataverseFile]]] = {
    trace(version)
    if (isPersistentId) get2[List[DataverseFile]](s"datasets/:persistentId/versions/${ version.getOrElse(VERSION_LATEST) }/files?persistentId=$id")
    else get2[List[DataverseFile]](s"datasets/$id/versions/${ version.getOrElse(VERSION_LATEST) }/files")
  }


  def delete(): Try[DataverseResponse[DataMessage]] = {
    trace(())
    if (isPersistentId) deletePath2[DataMessage](s"datasets/:persistentId/?persistentId=$id")
    else deletePath2[DataMessage](s"datasets/$id")
  }



  def listMetadataBlocks(version: Option[String] = None, name: Option[String]): Try[HttpResponse[Array[Byte]]] = {
    trace((version))
    if (isPersistentId) get(s"datasets/:persistentId/${ version.map(v => s"versions/$v/").getOrElse("") }metadata/${ name.getOrElse("") }?persistentId=$id")
    else get(s"datasets/$id/${ version.map(v => s"versions/$v/").getOrElse("") }/metadata/${ name.getOrElse("") }")
  }

  def updateMetadata(json: File, version: Option[String] = None): Try[HttpResponse[Array[Byte]]] = {
    trace(json, version)
    val path = if (isPersistentId) s"datasets/:persistentId/${ version.map(v => s"versions/$v/").getOrElse("") }?persistentId=$id"
               else s"datasets/$id/${ version.map(v => s"versions/$v/").getOrElse("") }/"
    tryReadFileToString(json).flatMap(put(path))
  }

  def editMetadata(json: File, replace: Boolean): Try[HttpResponse[Array[Byte]]] = {
    tryReadFileToString(json).flatMap(s => editMetadata(s, replace))
  }

  def editMetadata(json: String, replace: Boolean = false): Try[HttpResponse[Array[Byte]]] = {
    trace(json, replace)
    val path = if (isPersistentId) s"datasets/:persistentId/editMetadata/?persistentId=$id${
      if (replace) "&replace=$replace"
      else ""
    }"
               else s"datasets/$id/editMetadata/${
                 if (replace) "?replace=$replace"
                 else ""
               }"
    put(path)(json)
  }

  def deleteMetadata(json: File): Try[HttpResponse[Array[Byte]]] = {
    trace(json)
    val path = if (isPersistentId) s"datasets/:persistentId/deleteMetadata/?persistentId=$id"
               else s"datasets/$id/deleteMetadata"
    tryReadFileToString(json).flatMap(put(path))
  }

  def publish(updateType: String): Try[HttpResponse[Array[Byte]]] = {
    trace(updateType)
    val path = if (isPersistentId) s"datasets/:persistentId/actions/:publish/?persistentId=$id&type=$updateType"
               else s"datasets/$id/actions/:publish?type=$updateType"
    postJson(path)(null)
  }

  def deleteDraft(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/versions/:draft/?persistentId=$id"
               else s"datasets/$id/versions/:draft/"
    deletePath(path)
  }

  def setCitationDateField(fieldName: String): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/citationdate?persistentId=$id"
               else s"datasets/$id/citationdate"
    put(path)(s"$fieldName")
  }

  def revertCitationDateField(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/citationdate?persistentId=$id"
               else s"datasets/$id/citationdate"
    deletePath(path)
  }

  def listRoleAssignments(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/assignments?persistentId=$id"
               else s"datasets/$id/assignments"
    get(path)
  }

  def createPrivateUrl(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/privateUrl?persistentId=$id"
               else s"datasets/$id/privateUrl"
    postJson(path)(null)
  }

  def getPrivateUrl: Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/privateUrl?persistentId=$id"
               else s"datasets/$id/privateUrl"
    get(path)
  }

  def deletePrivateUrl(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/privateUrl?persistentId=$id"
               else s"datasets/$id/privateUrl"
    deletePath(path)
  }

  def addFile(dataFile: File, jsonMetadata: Option[File], jsonString: Option[String]): Try[HttpResponse[Array[Byte]]] = {
    trace(dataFile, jsonMetadata, jsonString)
    val path = if (isPersistentId) s"datasets/:persistentId/add?persistentId=$id"
               else s"datasets/$id/add"
    jsonMetadata.map {
      f =>
        tryReadFileToString(f).flatMap {
          s => postFile(path, dataFile, Some(s))(200, formatResponseAsJson = true)
        }
    }.getOrElse {
      postFile(path, dataFile, jsonString)(200, formatResponseAsJson = true)
    }
  }

  def submitForReview(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    val path = if (isPersistentId) s"datasets/:persistentId/submitForReview?persistentId=$id"
               else s"datasets/$id/submitForReview"
    postJson(path)(null)
  }

  def returnToAuthor(reason: String): Try[HttpResponse[Array[Byte]]] = {
    trace(reason)
    val path = if (isPersistentId) s"datasets/:persistentId/returnToAuthor?persistentId=$id"
               else s"datasets/$id/returnToAuthor"
    postJson(path)(s"""{"reasonForReturn": "$reason"}""")
  }

  def link(dataverseAlias: String): Try[HttpResponse[Array[Byte]]] = {
    trace(dataverseAlias)
    val path = if (isPersistentId) s"datasets/:persistentId/link/$dataverseAlias?persistentId=$id"
               else s"datasets/$id/link/$dataverseAlias"
    put(path)()
  }

  def getLocks(lockType: Option[String] = None): Try[HttpResponse[Array[Byte]]] = {
    trace(lockType)
    val path = if (isPersistentId) s"datasets/:persistentId/locks?persistentId=$id${ lockType.map(t => "&type=$t").getOrElse("") }"
               else s"datasets/$id/locks${ lockType.map(t => "?type=$t").getOrElse("") }"
    get(path)
  }
}
