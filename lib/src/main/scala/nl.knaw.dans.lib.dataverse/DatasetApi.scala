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
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType.UpdateType
import nl.knaw.dans.lib.dataverse.model.dataset.{ DatasetLatestVersion, DatasetVersion, FieldList, FileList, MetadataBlock, MetadataBlocks, PrivateUrlData }
import nl.knaw.dans.lib.dataverse.model.{ DataMessage, DatasetPublicationResult, Lock, RoleAssignment, RoleAssignmentReadOnly }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.native.Serialization
import org.json4s.{ DefaultFormats, Formats }
import java.net.URI

import nl.knaw.dans.lib.dataverse.model.file.FileInfo

import scala.util.{ Failure, Try }

/**
 * Functions that operate on a single dataset. See [[https://guides.dataverse.org/en/latest/api/native-api.html#datasets]].
 *
 */
class DatasetApi private[dataverse](datasetId: String, isPersistentDatasetId: Boolean, configuration: DataverseInstanceConfig) extends TargetedHttpSuport with DebugEnhancedLogging {
  private implicit val jsonFormats: Formats = DefaultFormats

  protected val connectionTimeout: Int = configuration.connectionTimeout
  protected val readTimeout: Int = configuration.readTimeout
  protected val baseUrl: URI = configuration.baseUrl
  protected val apiToken: String = configuration.apiToken
  protected val sendApiTokenViaBasicAuth = false
  protected val unblockKey: Option[String] = Option.empty
  protected val apiPrefix: String = "api"
  protected val apiVersion: Option[String] = Option(configuration.apiVersion)

  protected val targetBase: String = "datasets"
  protected val id: String = datasetId
  protected val isPersistentId: Boolean = isPersistentDatasetId

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-json-representation-of-a-dataset]]
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-version-of-a-dataset]]
   * @param version version to view (optional)
   * @return
   */
  def view(version: Version = Version.LATEST): Try[DataverseResponse[DatasetVersion]] = {
    trace(version)
    getVersionedFromTarget[DatasetVersion]("", version)
  }

  /**
   * Almost the same as [[DatasetApi#view]] except that `viewLatestVersion` returns a JSON object that starts at the dataset
   * level instead of the dataset version level. The dataset level contains some fields, most of which are replicated at the dataset version level, however.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-json-representation-of-a-dataset]]
   * @return
   */
  def viewLatestVersion(): Try[DataverseResponse[DatasetLatestVersion]] = {
    trace(())
    getUnversionedFromTarget[DatasetLatestVersion]("")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-versions-of-a-dataset]]
   * @return
   */
  def viewAllVersions(): Try[DataverseResponse[List[DatasetVersion]]] = {
    trace(())
    getUnversionedFromTarget[List[DatasetVersion]]("versions")
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
  def listFiles(version: Version = Version.LATEST): Try[DataverseResponse[List[FileInfo]]] = {
    trace(version)
    getVersionedFromTarget[List[FileInfo]]("files", version)
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-all-metadata-blocks-for-a-dataset]]
   * @param version the version of the dataset
   * @return a map of metadata block identifier to metadata block
   */
  def listMetadataBlocks(version: Version = Version.LATEST): Try[DataverseResponse[MetadataBlocks]] = {
    trace((version))
    getVersionedFromTarget[MetadataBlocks]("metadata", version)
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-single-metadata-block-for-a-dataset]]
   * @param name    the metadata block identifier
   * @param version the version of the dataset
   * @return
   */
  def getMetadataBlock(name: String, version: Version = Version.LATEST): Try[DataverseResponse[MetadataBlock]] = {
    trace(name, version)
    getVersionedFromTarget[MetadataBlock](s"metadata/$name", version)
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
    // Cheating with endPoint here, because the only version that can be updated is :draft anyway
    putToTarget[DatasetVersion]("versions/:draft", Serialization.write(Map("metadataBlocks" -> metadataBlocks)))
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
    putToTarget("editMetadata",
      Serialization.write(fields),
      if (replace) Map("replace" -> "true")
      else Map.empty) // Sic! any value for "replace" is interpreted by Dataverse as "true", even "replace=false"
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
    putToTarget("deleteMetadata", Serialization.write(fields))
  }

  /**
   * Publishes the current draft of a dataset as a new version.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#publish-a-dataset]]
   * @param updateType major or minor version update
   * @return
   */
  def publish(updateType: UpdateType): Try[DataverseResponse[DatasetPublicationResult]] = {
    trace(updateType)
    postJsonToTarget[DatasetPublicationResult]("actions/:publish", "", Map("type" -> updateType.toString))
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
    deleteAtTarget("versions/:draft")
  }

  /**
   * Sets the dataset citation date field type for a given dataset.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#set-citation-date-field-type-for-a-dataset]]
   * @param fieldName the field name of a date field
   * @return
   */
  def setCitationDateField(fieldName: String): Try[DataverseResponse[Nothing]] = {
    trace(fieldName)
    putToTarget("citationdate", fieldName)
  }

  /**
   * Restores the default citation date field type.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#revert-citation-date-field-type-to-default-for-dataset]]
   * @return
   */
  def revertCitationDateField(): Try[DataverseResponse[Nothing]] = {
    deleteAtTarget("citationdate")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-role-assignments-in-a-dataset]]
   * @return
   */
  def listRoleAssignments(): Try[DataverseResponse[List[RoleAssignmentReadOnly]]] = {
    trace(())
    getUnversionedFromTarget("assignments")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#assign-a-new-role-on-a-dataset]]
   * @param roleAssignment object describing the assignment
   * @return
   */
  def assignRole(roleAssignment: RoleAssignment): Try[DataverseResponse[RoleAssignmentReadOnly]] = {
    trace(roleAssignment)
    postJsonToTarget[RoleAssignmentReadOnly]("assignments", Serialization.write(roleAssignment))
  }

  /**
   * Use [[DatasetApi#listRoleAssignments]] to get the ID.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-role-assignment-from-a-dataset]]
   * @param assignmentId the ID of the assignment to delete
   * @return
   */
  def deleteRoleAssignment(assignmentId: Int): Try[DataverseResponse[Nothing]] = {
    trace(assignmentId)
    deleteAtTarget[Nothing](s"assignments/${ assignmentId }")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-private-url-for-a-dataset]]
   * @return
   */
  def createPrivateUrl(): Try[DataverseResponse[PrivateUrlData]] = {
    trace(())
    postJsonToTarget[PrivateUrlData]("privateUrl", "")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#get-the-private-url-for-a-dataset]]
   * @return
   */
  def getPrivateUrl: Try[DataverseResponse[PrivateUrlData]] = {
    trace(())
    getUnversionedFromTarget("privateUrl")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-the-private-url-from-a-dataset]]
   * @return
   */
  def deletePrivateUrl(): Try[DataverseResponse[Nothing]] = {
    trace(())
    deleteAtTarget[Nothing]("privateUrl")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#add-a-file-to-a-dataset]]
   * @param dataFile       the file to upload
   * @param fileMedataData optional metadata for the file
   * @return
   */
  def addFile(dataFile: File, fileMedataData: FileInfo): Try[DataverseResponse[FileList]] = {
    trace(dataFile, fileMedataData)
    postFileToTarget[FileList]("add", Option(dataFile), Option(Serialization.write(fileMedataData)))
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#dataset-locks]]
   * @return
   */
  def getLocks: Try[DataverseResponse[List[Lock]]] = {
    trace(())
    getUnversionedFromTarget[List[Lock]]("locks")
  }
}
