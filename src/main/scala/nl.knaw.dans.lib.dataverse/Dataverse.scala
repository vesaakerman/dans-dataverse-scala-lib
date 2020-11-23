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
import nl.knaw.dans.lib.dataverse.model.DefaultRole.DefaultRole
import nl.knaw.dans.lib.dataverse.model._
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetCreationResult
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import scalaj.http.HttpResponse

import scala.util.Try

class Dataverse private[dataverse](dvId: String, configuration: DataverseInstanceConfig) extends HttpSupport with DebugEnhancedLogging {
  trace(dvId)
  protected val connectionTimeout: Int = configuration.connectionTimeout
  protected val readTimeout: Int = configuration.readTimeout
  protected val baseUrl: URI = configuration.baseUrl
  protected val apiToken: String = configuration.apiToken
  protected val apiVersion: String = configuration.apiVersion

  /**
   * Creates a dataverse based on a definition provided in a JSON file.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-dataverse]]
   * @param jsonFile the JSON file with the dataverse definition
   * @return
   */
  def create(jsonFile: File): Try[DataverseResponse[model.Dataverse]] = {
    trace(jsonFile)
    tryReadFileToString(jsonFile)
      .flatMap(postJson[model.Dataverse](s"dataverses/$dvId"))
  }

  /**
   * Creates a dataverse base on a definition provided as model object.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-dataverse]]
   * @param dd the model object
   * @return
   */
  def create(dd: model.Dataverse): Try[DataverseResponse[model.Dataverse]] = {
    trace(dd)
    for {
      jsonString <- serializeAsJson(dd, logger.underlying.isDebugEnabled)
      response <- postJson[model.Dataverse](s"dataverses/$dvId")(jsonString)
    } yield response
  }

  /**
   * Returns the definition of a dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#view-a-dataverse]]
   * @return
   */
  def view(): Try[DataverseResponse[model.Dataverse]] = {
    trace(())
    get(s"dataverses/$dvId")
  }

  /**
   * Deletes a dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-a-dataverse]]
   * @return
   */
  def delete(): Try[DataverseResponse[DataMessage]] = {
    trace(())
    deletePath[DataMessage](s"dataverses/$dvId")
  }

  /**
   * Returns the contents of a dataverse (datasets and sub-verses)
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#show-contents-of-a-dataverse]]
   * @return
   */
  def contents(): Try[DataverseResponse[List[DataverseItem]]] = {
    trace(())
    get[List[DataverseItem]](s"dataverses/$dvId/contents")
  }

  /**
   * Returns the roles defined in a dataverse.
   *
   * @see https://guides.dataverse.org/en/latest/api/native-api.html#list-roles-defined-in-a-dataverse
   * @return
   */
  def listRoles(): Try[DataverseResponse[List[Role]]] = {
    trace(())
    get[List[Role]](s"dataverses/$dvId/roles")
  }

  // DataverseResponse[DataMessage]

  /**
   * Creates a role based on a definition provided in a JSON file.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-new-role-in-a-dataverse]]
   * @param jsonFile the JSON file with the role definition
   * @return
   */
  def createRole(jsonFile: File): Try[DataverseResponse[Role]] = {
    trace(jsonFile)
    tryReadFileToString(jsonFile).flatMap(postJson[Role](s"dataverses/$dvId/roles"))
  }

  /**
   * Creates a role base on a definition provided as model object.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-new-role-in-a-dataverse]]
   * @param role the model object
   * @return
   */
  def createRole(role: Role): Try[DataverseResponse[Role]] = {
    trace(role)
    for {
      jsonString <- serializeAsJson(role, logger.underlying.isDebugEnabled)
      response <- postJson[Role](s"dataverses/$dvId/roles")(jsonString)
    } yield response
  }

  /**
   * Return the data (file) size of a Dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#report-the-data-file-size-of-a-dataverse]]
   * @return
   */
  def storageSize(): Try[DataverseResponse[DataMessage]] = {
    trace(())
    get[DataMessage](s"dataverses/$dvId/storagesize")
  }

  /**
   * Returns the list of active facets for a dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-facets-configured-for-a-dataverse]]
   * @return
   */
  def listFacets(): Try[DataverseResponse[List[String]]] = {
    trace(())
    get[List[String]](s"dataverses/$dvId/facets")
  }

  /**
   * Sets the list of active facets for a dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#set-facets-for-a-dataverse]]
   * @param facets the list of facets
   * @return
   */
  def setFacets(facets: List[String]): Try[DataverseResponse[DataMessage]] = {
    trace(facets)
    for {
      jsonString <- serializeAsJson(facets, logger.underlying.isDebugEnabled)
      response <- postJson[DataMessage](s"dataverses/$dvId/facets")(jsonString)
    } yield response
  }

  /**
   * List all the role assignments at the given dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-role-assignments-in-a-dataverse]]
   * @return
   */
  def listRoleAssignments(): Try[DataverseResponse[List[RoleAssignmentReadOnly]]] = {
    trace(())
    get[List[RoleAssignmentReadOnly]](s"dataverses/$dvId/assignments")
  }

  /**
   * Assigns a default role to a user creating a dataset in a dataverse.
   *
   * Note: there does not seem to be a way to retrieve the current default role via the API.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#assign-default-role-to-user-creating-a-dataset-in-a-dataverse]]
   * @param role the role to assign
   * @return
   */
  def setDefaultRole(role: DefaultRole): Try[DataverseResponse[DataMessage]] = {
    trace(role)
    put[DataMessage](s"dataverses/$dvId/defaultContributorRole/$role")(null)
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#assign-a-new-role-on-a-dataverse]]
   * @param jsonFile file containing the role assignment JSON object
   * @return
   */
  def assignRole(jsonFile: File): Try[DataverseResponse[RoleAssignmentReadOnly]] = {
    trace(jsonFile)
    tryReadFileToString(jsonFile).flatMap(postJson[RoleAssignmentReadOnly](s"dataverses/$dvId/assignments"))
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#assign-a-new-role-on-a-dataverse]]
   * @param roleAssignment object describing the assignment
   * @return
   */
  def assignRole(roleAssignment: RoleAssignment): Try[DataverseResponse[RoleAssignmentReadOnly]] = {
    trace(roleAssignment)
    for {
      jsonString <- serializeAsJson(roleAssignment, logger.underlying.isDebugEnabled)
      response <- postJson[RoleAssignmentReadOnly](s"dataverses/$dvId/assignments")(jsonString)
    } yield response
  }

  /**
   * Use [[Dataverse#listRoleAssignments]] to get the ID.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-role-assignment-from-a-dataverse]]
   * @param assignmentId the ID of the assignment to delete
   * @return
   */
  def deleteRoleAssignment(assignmentId: Int): Try[DataverseResponse[DataMessage]] = {
    trace(assignmentId)
    deletePath[DataMessage](s"dataverses/$dvId/assignments/$assignmentId")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-metadata-blocks-defined-on-a-dataverse]]
   * @return
   */
  def listMetadataBocks(): Try[DataverseResponse[List[MetadataBlockSummary]]] = {
    trace(())
    get[List[MetadataBlockSummary]](s"dataverses/$dvId/metadatablocks")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#define-metadata-blocks-for-a-dataverse]]
   * @param mdBlockIds list of metadata block IDs
   * @return
   */
  def setMetadataBlocks(mdBlockIds: List[String]): Try[DataverseResponse[DataMessage]] = {
    trace(mdBlockIds)
    for {
      jsonString <- serializeAsJson(mdBlockIds, logger.underlying.isDebugEnabled)
      response <- postJson[DataMessage](s"dataverses/$dvId/metadatablocks")(jsonString)
    } yield response
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#determine-if-a-dataverse-inherits-its-metadata-blocks-from-its-parent]]
   * @return
   */
  def isMetadataBlocksRoot: Try[DataverseResponse[Boolean]] = {
    trace(())
    get[Boolean](s"dataverses/$dvId/metadatablocks/isRoot")
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#configure-a-dataverse-to-inherit-its-metadata-blocks-from-its-parent]]
   * @param isRoot whether to make the dataverse a metadata root
   * @return
   */
  def setMetadataBlocksRoot(isRoot: Boolean): Try[DataverseResponse[DataMessage]] = {
    trace(isRoot)
    for {
      jsonString <- serializeAsJson(isRoot, logger.underlying.isDebugEnabled)
      response <- put[DataMessage](s"dataverses/$dvId/metadatablocks/isRoot")(jsonString)
    } yield response
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-dataset-in-a-dataverse]]
   * @param dataset model object defining the dataset
   * @return
   */
  def createDataset(dataset: model.dataset.Dataset): Try[DataverseResponse[DatasetCreationResult]] = {
    trace(dataset)
    for {
      jsonString <- serializeAsJson(dataset, logger.underlying.isDebugEnabled)
      response <- postJson[DatasetCreationResult](s"dataverses/$dvId/datasets")(jsonString)
    } yield response
  }

  /**
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#create-a-dataset-in-a-dataverse]]
   * @param jsonFile JSON file defining the dataset
   * @return
   */
  def createDataset(jsonFile: File): Try[DataverseResponse[DatasetCreationResult]] = {
    trace(jsonFile)
    tryReadFileToString(jsonFile).flatMap(postJson[DatasetCreationResult](s"dataverses/$dvId/datasets"))
  }

  /**
   * Import a dataset with an existing persistent identifier.
   *
   * @param dataset     model object defining the dataset
   * @param autoPublish immediately publish dataset after publication
   * @return
   */
  def importDataset(dataset: model.dataset.Dataset, autoPublish: Boolean = false): Try[DataverseResponse[DatasetCreationResult]] = {
    trace(dataset, autoPublish)
    for {
      pid <- getPid(dataset)
      _ = debug(s"Found pid = $pid")
      jsonString <- serializeAsJson(dataset, logger.underlying.isDebugEnabled)
      response <- postJson[DatasetCreationResult](s"dataverses/$dvId/datasets/:import?pid=$pid&release=$autoPublish")(jsonString)
    } yield response
  }

  private def getPid(dataset: model.dataset.Dataset): Try[String] = Try {
    if (dataset.datasetVersion.protocol.isEmpty
      || dataset.datasetVersion.authority.isEmpty
      || dataset.datasetVersion.identifier.isEmpty) throw new IllegalArgumentException("Dataset object does not contain protocol, authority or identifier fields.")
    else s"${ dataset.datasetVersion.protocol.get }:${ dataset.datasetVersion.authority.get }/${ dataset.datasetVersion.identifier.get }"
  }

  // TODO: importDataset(jsonFile)
  // TODO: importDataset(ddiFile)

  /**
   * Publishes a dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#publish-a-dataverse]]
   *
   * @return
   */
  def publish(): Try[DataverseResponse[DataverseSummary]] = {
    trace(())
    postJson[DataverseSummary](s"dataverses/$dvId/actions/:publish")()
  }
}