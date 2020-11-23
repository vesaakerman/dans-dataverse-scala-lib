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
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType.UpdateType
import nl.knaw.dans.lib.dataverse.model.dataset.{ DatasetVersion, DataverseFile, FieldList, MetadataBlock, MetadataBlocks, PrivateUrlData }
import nl.knaw.dans.lib.dataverse.model.{ DataMessage, RoleAssignment, RoleAssignmentReadOnly }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.DefaultFormats
import org.json4s.native.Serialization

import scala.util.{ Failure, Try }

/**
 * Functions that operate on a single dataset. See [[https://guides.dataverse.org/en/latest/api/native-api.html#datasets]].
 *
 */
class Dataset private[dataverse](id: String, isPersistentId: Boolean, configuration: DataverseInstanceConfig) extends HttpSupport with DebugEnhancedLogging {
  private implicit val jsonFormats: DefaultFormats = DefaultFormats

  protected val connectionTimeout: Int = configuration.connectionTimeout
  protected val readTimeout: Int = configuration.readTimeout
  protected val baseUrl: URI = configuration.baseUrl
  protected val apiToken: String = configuration.apiToken
  protected val apiVersion: String = configuration.apiVersion

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-json-representation-of-a-dataset]]
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-version-of-a-dataset]]
   * @param version version to view (optional)
   * @return
   */
  def view(version: Version = Version.LATEST): Try[DataverseResponse[model.dataset.DatasetVersion]] = {
    trace(version)
    getVersioned[model.dataset.DatasetVersion]("", version)
  }

  /**
   * Almost the same as [[Dataset#view]] except that `viewLatestVersion` returns a JSON object that starts at the dataset
   * level instead of the dataset version level. The dataset level contains some fields, most of which are replicated at the dataset version level, however.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-json-representation-of-a-dataset]]
   * @return
   */
  def viewLatestVersion(): Try[DataverseResponse[model.dataset.DatasetLatestVersion]] = {
    trace(())
    getUnversioned[model.dataset.DatasetLatestVersion]("")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-versions-of-a-dataset]]
   * @return
   */
  def viewAllVersions(): Try[DataverseResponse[List[DatasetVersion]]] = {
    trace(())
    getUnversioned[List[DatasetVersion]]("versions")
  }

  /**
   * Since the export format is generally not JSON you cannot use the [[DataverseResponse#json]] and [[DataverseResponse#data]]
   * on the result. You should instead use [[DataverseResponse#string]].
   *
   * Note that this API does not support specifying a version.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#export-metadata-of-a-dataset-in-various-formats]]
   * @param format the export format
   * @return
   */
  def exportMetadata(format: String): Try[DataverseResponse[Any]] = {
    trace(())
    if (!isPersistentId) Failure(new IllegalArgumentException("exportMetadata only works with PIDs"))
    // Cannot use helper function because this API does not support the :persistentId constant
    get[Any](s"datasets/export/?exporter=$format&persistentId=$id")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-files-in-a-dataset]]
   * @param version the version of the dataset
   * @return
   */
  def listFiles(version: Version = Version.UNSPECIFIED): Try[DataverseResponse[List[DataverseFile]]] = {
    trace(version)
    getVersioned[List[DataverseFile]]("files", version)
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-all-metadata-blocks-for-a-dataset]]
   * @param version the version of the dataset
   * @return a map of metadata block identifier to metadata block
   */
  def listMetadataBlocks(version: Version = Version.UNSPECIFIED): Try[DataverseResponse[Map[String, MetadataBlock]]] = {
    trace((version))
    getVersioned[Map[String, MetadataBlock]]("metadata", version)
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-single-metadata-block-for-a-dataset]]
   * @param name    the metadata block identifier
   * @param version the version of the dataset
   * @return
   */
  def getMetadataBlock(name: String, version: Version = Version.UNSPECIFIED): Try[DataverseResponse[MetadataBlock]] = {
    trace(name, version)
    getVersioned[MetadataBlock](s"metadata/$name", version)
  }

  /**
   * Creates or overwrites the current draft's metadata completely.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#update-metadata-for-a-dataset]]
   * @param metadataBlocks map from metadata block id to `MetadataBlock`
   * @return
   */
  def updateMetadata(metadataBlocks: MetadataBlocks): Try[DataverseResponse[DatasetVersion]] = {
    trace(metadataBlocks)
    putVersioned[DatasetVersion]("", Serialization.write(Map("metadataBlocks" -> metadataBlocks)), Version.DRAFT)
  }

  /**
   * Edits the current draft's metadata, adding the fields that do not exist yet. If `replace` is set to `false`, all specified
   * fields must be either currently empty or allow multiple values.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#edit-dataset-metadata]]
   * @param fields  list of fields to edit
   * @param replace wether to replace existing values
   * @return
   */
  def editMetadata(fields: FieldList, replace: Boolean = true): Try[DataverseResponse[DatasetVersion]] = {
    trace(fields)
    putVersioned("editMetadata", Serialization.write(fields), Version.UNSPECIFIED, if (replace) Map("replace" -> "true")
                                                                                   else Map.empty) // Sic! any value for replace is interpreted by Dataverse as "true"
  }

  /**
   * Deletes one or more values from the current draft's metadata. Note that the delete will fail if the
   * result would leave required fields empty.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-dataset-metadata]]
   * @param fields the fields to delete
   * @return
   */
  def deleteMetadata(fields: FieldList): Try[DataverseResponse[DatasetVersion]] = {
    trace(fields)
    putVersioned("deleteMetadata", Serialization.write(fields), Version.UNSPECIFIED)
  }

  /**
   * Publishes the current draft of a dataset as a new version.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#publish-a-dataset]]
   * @param updateType major or minor version update
   * @return
   */
  def publish(updateType: UpdateType): Try[DataverseResponse[DatasetVersion]] = {
    trace(updateType)
    postJsonUnversioned[DatasetVersion]("actions/:publish", "", Map("type" -> updateType.toString))
  }

  /**
   * Deletes the current draft of a dataset.
   *
   * Note: as of writing this there is a bug in Dataverse (v5.1.1) which causes it to use the literal string `:persistendId` in the response message
   * instead of the actual identifier when using a PID.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-dataset-draft]]
   * @return
   */
  def deleteDraft(): Try[DataverseResponse[DataMessage]] = {
    trace(())
    deleteVersioned("", Version.DRAFT)
  }

  /**
   * Sets the dataset citation date field type for a given dataset.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#set-citation-date-field-type-for-a-dataset]]
   * @param fieldName the field name of a date field
   * @return
   */
  def setCitationDateField(fieldName: String): Try[DataverseResponse[DataMessage]] = {
    trace(fieldName)
    putVersioned("citationdate", fieldName)
  }

  /**
   * Restores the default citation date field type.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#revert-citation-date-field-type-to-default-for-dataset]]
   * @return
   */
  def revertCitationDateField(): Try[DataverseResponse[DataMessage]] = {
    deleteVersioned("citationdate")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-role-assignments-in-a-dataset]]
   * @return
   */
  def listRoleAssignments(): Try[DataverseResponse[List[RoleAssignmentReadOnly]]] = {
    trace(())
    getUnversioned("assignments")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#assign-a-new-role-on-a-dataset]]
   * @param roleAssignment object describing the assignment
   * @return
   */
  def assignRole(roleAssignment: RoleAssignment): Try[DataverseResponse[RoleAssignmentReadOnly]] = {
    trace(roleAssignment)
    postJsonUnversioned[RoleAssignmentReadOnly]("assignments", Serialization.write(roleAssignment))
  }

  /**
   * Use [[Dataset#listRoleAssignments]] to get the ID.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-role-assignment-from-a-dataset]]
   * @param assignmentId the ID of the assignment to delete
   * @return
   */
  def deleteRoleAssignment(assignmentId: Int): Try[DataverseResponse[DataMessage]] = {
    trace(assignmentId)
    deleteVersioned[DataMessage](s"assignments/${ assignmentId }")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-private-url-for-a-dataset]]
   * @return
   */
  def createPrivateUrl(): Try[DataverseResponse[PrivateUrlData]] = {
    trace(())
    postJsonUnversioned[PrivateUrlData]("privateUrl", "")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-the-private-url-for-a-dataset]]
   * @return
   */
  def getPrivateUrl(): Try[DataverseResponse[PrivateUrlData]] = {
    trace(())
    getUnversioned("privateUrl")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-the-private-url-from-a-dataset]]
   * @return
   */
  def deletePrivateUrl(): Try[DataverseResponse[DataMessage]] = {
    trace(())
    deleteVersioned[DataMessage]("privateUrl")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#add-a-file-to-a-dataset]]
   * @param dataFile       the file to upload
   * @param fileMedataData optional metadata for the file
   * @return
   */
  def addFile(dataFile: File, fileMedataData: DataverseFile): Try[DataverseResponse[DataverseFile]] = {
    trace(dataFile, fileMedataData)
    postFileUnversioned[DataverseFile]("add", dataFile, Option(Serialization.write(fileMedataData)))
  }

  /*
   * Helper functions.
   */

  private def getVersioned[D: Manifest](endPoint: String, version: Version = Version.UNSPECIFIED): Try[DataverseResponse[D]] = {
    trace(endPoint, version)
    if (isPersistentId) super.get[D](s"datasets/:persistentId/versions/${
      if (version == Version.UNSPECIFIED) ""
      else version
    }/${ endPoint }?persistentId=$id")
    else super.get[D](s"datasets/$id/versions/${
      if (version == Version.UNSPECIFIED) ""
      else version
    }/${ endPoint }")
  }

  private def getUnversioned[D: Manifest](endPoint: String, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    trace(endPoint)
    if (isPersistentId) super.get[D](s"datasets/:persistentId/${ endPoint }/?persistentId=$id")
    else super.get[D](s"datasets/$id/${ endPoint }")
  }

  private def putVersioned[D: Manifest](endPoint: String, body: String, version: Version = Version.UNSPECIFIED, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, body, version, queryParams)
    if (isPersistentId) super.put[D](s"datasets/:persistentId/${
      if (version == Version.UNSPECIFIED) ""
      else s"versions/$version"
    }/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }")(body)
    else super.put[D](s"datasets/$id/${
      if (version == Version.UNSPECIFIED) ""
      else s"versions/$version"
    }/${ endPoint }$queryString")(body)
  }

  private def postJsonUnversioned[D: Manifest](endPoint: String, body: String, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, queryParams)
    if (isPersistentId) super.postJson[D](s"datasets/:persistentId/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }")(body)
    else super.postJson[D](s"datasets/$id/${ endPoint }$queryString")(body)
  }

  private def postFileUnversioned[D: Manifest](endPoint: String, file: File, metadata: Option[String], queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, queryParams)
    if (isPersistentId) super.postFile[D](s"datasets/:persistentId/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }", file, metadata)
    else super.postFile[D](s"datasets/$id/${ endPoint }$queryString", file, metadata)
  }

  private def deleteVersioned[D: Manifest](endPoint: String, version: Version = Version.UNSPECIFIED, queryParams: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    trace(endPoint, version, queryParams)
    if (isPersistentId) super.deletePath[D](s"datasets/:persistentId/${
      if (version == Version.UNSPECIFIED) ""
      else s"versions/$version"
    }/${ endPoint }?persistentId=$id${
      if (queryString.nonEmpty) "&" + queryString
      else ""
    }")
    else super.deletePath[D](s"datasets/$id/${
      if (version == Version.UNSPECIFIED) ""
      else s"versions/$version"
    }/${ endPoint }$queryString")
  }
}
