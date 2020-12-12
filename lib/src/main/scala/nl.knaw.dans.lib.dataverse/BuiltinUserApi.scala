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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.{ DefaultFormats, Formats }
import org.json4s.native.Serialization

import scala.util.Try

/**
 * Functions to manage builtin user accounts. Note that the [[`BuiltinUsers.KEY` https://guides.dataverse.org/en/5.2/installation/config.html#builtinusers-key]]
 * must be set and configured in [[DataverseInstanceConfig]] for this to work.
 *
 * @param configuration the dataverse instance configuration
 */
class BuiltinUserApi private[dataverse](configuration: DataverseInstanceConfig) extends HttpSupport with DebugEnhancedLogging {
  trace(())
  private implicit val jsonFormats: Formats = DefaultFormats
  protected val connectionTimeout: Int = configuration.connectionTimeout
  protected val readTimeout: Int = configuration.readTimeout
  protected val baseUrl: URI = configuration.baseUrl
  protected val apiToken: String = configuration.apiToken
  protected val sendApiTokenViaBasicAuth = false
  protected val unblockKey: Option[String] = Option.empty
  protected val builtinUserKey: Option[String] = configuration.builtinUserKey
  protected val apiPrefix: String = "api"
  protected val apiVersion: Option[String] = Option(configuration.apiVersion)

  /**
   * @see [[https://guides.dataverse.org/en/5.2/api/native-api.html#create-a-builtin-user]]
   * @param user the user account info
   * @param password the password to set for the new user
   * @return
   */
  def create(user: model.BuiltinUser, password: String): Try[DataverseResponse[Any]] = {
    postJson[Any]("builtin-users", Serialization.write(user), Map.empty, Map("password" -> password, "key" -> builtinUserKey.getOrElse("")))
  }



}
