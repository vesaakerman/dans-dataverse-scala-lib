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

import scala.util.Try

class WorkflowsApi private[dataverse](configuration: DataverseInstanceConfig) extends HttpSupport with DebugEnhancedLogging {
  protected val connectionTimeout: Int = configuration.connectionTimeout
  protected val readTimeout: Int = configuration.readTimeout
  protected val baseUrl: URI = configuration.baseUrl
  protected val apiToken: String = configuration.apiToken
  protected val sendApiTokenViaBasicAuth = false
  protected val unblockKey: Option[String] = Option.empty
  protected val builtinUserKey: Option[String] = Option.empty
  protected val apiPrefix: String = "api"
  protected val apiVersion: Option[String] = Option(configuration.apiVersion)

  def resume(invocationId: String): Try[DataverseResponse[Nothing]] = {
    postText(s"workflows/$invocationId")(body = "")
  }
}
