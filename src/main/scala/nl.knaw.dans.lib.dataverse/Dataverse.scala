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
import nl.knaw.dans.lib.dataverse.model.{ DataMessage, DataverseItem, Role, RoleAssignment, RoleAssignmentReadOnly }
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
      .flatMap(postJson2[model.Dataverse](s"dataverses/$dvId"))
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
      response <- postJson2[model.Dataverse](s"dataverses/$dvId")(jsonString)
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
    get2(s"dataverses/$dvId")
  }

  /**
   * Deletes a dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#delete-a-dataverse]]
   * @return
   */
  def delete(): Try[DataverseResponse[DataMessage]] = {
    trace(())
    deletePath2[DataMessage](s"dataverses/$dvId")
  }

  /**
   * Returns the contents of a dataverse (datasets and sub-verses)
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#show-contents-of-a-dataverse]]
   * @return
   */
  def contents(): Try[DataverseResponse[List[DataverseItem]]] = {
    trace(())
    get2[List[DataverseItem]](s"dataverses/$dvId/contents")
  }

  /**
   * Returns the roles defined in a dataverse.
   *
   * @see https://guides.dataverse.org/en/latest/api/native-api.html#list-roles-defined-in-a-dataverse
   * @return
   */
  def listRoles(): Try[DataverseResponse[List[Role]]] = {
    trace(())
    get2[List[Role]](s"dataverses/$dvId/roles")
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
    tryReadFileToString(jsonFile).flatMap(postJson2[Role](s"dataverses/$dvId/roles"))
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
      response <- postJson2[Role](s"dataverses/$dvId/roles")(jsonString)
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
    get2[DataMessage](s"dataverses/$dvId/storagesize")
  }

  /**
   * Returns the list of active facets for a dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-facets-configured-for-a-dataverse]]
   * @return
   */
  def listFacets(): Try[DataverseResponse[List[String]]] = {
    trace(())
    get2[List[String]](s"dataverses/$dvId/facets")
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
      response <- postJson2[DataMessage](s"dataverses/$dvId/facets")(jsonString)
    } yield response
  }

  /**
   * List all the role assignments at the given dataverse.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#list-role-assignments-in-a-dataverse]]
   *
   * @return
   */
  def listRoleAssignments(): Try[DataverseResponse[List[RoleAssignmentReadOnly]]] = {
    trace(())
    get2[List[RoleAssignmentReadOnly]](s"dataverses/$dvId/assignments")
  }

  /**
   * Assigns a default role to a user creating a dataset in a dataverse.
   *
   * Note: there does not seem to be a way to retrieve the current default role via the API.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#assign-default-role-to-user-creating-a-dataset-in-a-dataverse]]
   *
   * @param role the role to assign
   * @return
   */
  def setDefaultRole(role: DefaultRole): Try[DataverseResponse[DataMessage]] = {
    trace(role)
    put2[DataMessage](s"dataverses/$dvId/defaultContributorRole/$role")(null)
  }

  def assignRole(jsonDef: File): Try[HttpResponse[Array[Byte]]] = {
    trace(jsonDef)
    tryReadFileToString(jsonDef).flatMap(postJson(s"dataverses/$dvId/assignments"))
  }

  /**
   * 
   *
   * @param roleAssignment
   * @return
   */
  def assignRole(roleAssignment: RoleAssignment): Try[DataverseResponse[Any]] = {
    trace(roleAssignment)
    for {
      jsonString <- serializeAsJson(roleAssignment, logger.underlying.isDebugEnabled)
      response <- postJson2[Any](s"dataverses/$dvId/asignments")(jsonString)
    } yield response
  }



  def unassignRole(assignmentId: String): Try[HttpResponse[Array[Byte]]] = {
    trace(assignmentId)
    deletePath(s"dataverses/$dvId/assignments/$assignmentId")
  }

  def listMetadataBocks(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    get(s"dataverses/$dvId/metadatablocks")
  }

  def setMetadataBlocks(mdBlockIds: Seq[String]): Try[HttpResponse[Array[Byte]]] = {
    trace(mdBlockIds)
    postJson(s"dataverses/$dvId/metadatablocks")(mdBlockIds.map(s => s""""$s"""").mkString("[", ",", "]"))
  }

  def isMetadataBlocksRoot: Try[HttpResponse[Array[Byte]]] = {
    trace(())
    get(s"dataverses/$dvId/metadatablocks/isRoot")
  }

  def setMetadataBlocksRoot(isRoot: Boolean): Try[HttpResponse[Array[Byte]]] = {
    trace(isRoot)
    put(s"dataverses/$dvId/metadatablocks/isRoot")(isRoot.toString.toLowerCase)
  }

  def createDataset(json: File): Try[HttpResponse[Array[Byte]]] = {
    trace(json)
    tryReadFileToString(json).flatMap(postJson(s"dataverses/$dvId/datasets"))
  }

  def createDataset(json: String): Try[HttpResponse[Array[Byte]]] = {
    trace(json)
    postJson(s"dataverses/$dvId/datasets")(json)
  }

  def importDataset(json: String, isDdi: Boolean = false, pid: String, keepOnDraft: Boolean = false): Try[HttpResponse[Array[Byte]]] = {
    trace(json)
    postJson(s"dataverses/$dvId/datasets/:import${
      if (isDdi) "ddi"
      else ""
    }?pid=$pid&release=${ !keepOnDraft }")(json)
  }

  def publish(): Try[HttpResponse[Array[Byte]]] = {
    trace(())
    postJson(s"dataverses/$dvId/actions/:publish")()
  }
}