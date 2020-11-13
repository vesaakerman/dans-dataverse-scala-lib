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
package nl.knaw.dans.lib.dataverse.model

// TODO: clean up example
/**
 * Response message sent by Dataverse. This can be extracted into case class objects with json4s.
 *
 * @example
 * {{{
 * val config = DataverseInstanceConfig(
 *   apiToken = "2a0e79d2-13ed-4e1a-8ffc-c93b97b01f50",
 *   baseUrl = new URI("https://ddd.dans.knaw.nl/"))
 *
 * implicit val resultOutputStream: PrintStream = System.out
 * val dv = new DataverseInstance(config)
 * val r = dv.dataverse("root").listRoleAssignments().get
 * val result = JsonMethods.parse(new String(r.body)).extract[DataverseMessage[List[RoleAssignment]]]
 * result.status
 * result.data(1)._roleAlias
 * }}}
 * @param status OK or ERROR
 * @param data   payload of the response; in case of an ERROR: the error message
 * @tparam P the expected type of payload
 * */
case class DataverseMessage[P](status: String, data: P)

object DataverseMessage {

  /**
   * Constructs a [[DataverseMessage]] that contains an error. The `message` field content is put in the `data` field of the `DataverseMessage` object.
   *
   * @param status  the status (which will be `ERROR`)
   * @param message the error message
   * @return the DataverseMessage object
   */
  def apply(status: String, message: String): DataverseMessage[String] = {
    DataverseMessage(status, data = message)
  }
}

/**
 * @see [[Dataverse#view]]
 * @param id            the internal database ID of the dataverse
 * @param alias         the dataverse alias
 * @param name          the display name
 * @param permissionRoot
 * @param description   the description of the dataverse
 * @param dataverseType the category of dataverse (journal, department, etc)
 * @param creationDate  the timestamp of creation
 *
 */
case class DataverseSummary(id: Int, alias: String, name: String, permissionRoot: Boolean, description: String, dataverseType: String, creationDate: String)

/**
 *
 *
 *
 * @param id
 * @param assignee
 * @param roleId
 * @param _roleAlias
 * @param definitionPointId
 */
case class RoleAssignment(id: Int, assignee: String, roleId: Int, _roleAlias: String, definitionPointId: Int)



