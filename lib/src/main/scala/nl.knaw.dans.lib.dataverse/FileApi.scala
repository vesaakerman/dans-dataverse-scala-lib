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
import nl.knaw.dans.lib.dataverse.model.dataset.DataverseFile
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.{ DefaultFormats, Formats }
import org.json4s.native.Serialization
import scalaj.http.HttpResponse

import scala.util.Try

class FileApi private[dataverse](filedId: String, isPersistentFileId: Boolean, configuration: DataverseInstanceConfig) extends TargetedHttpSuport with DebugEnhancedLogging {
  protected val connectionTimeout: Int = configuration.connectionTimeout
  protected val readTimeout: Int = configuration.readTimeout
  protected val baseUrl: URI = configuration.baseUrl
  protected val apiToken: String = configuration.apiToken
  protected val sendApiTokenViaBasicAuth = false
  protected val unblockKey: Option[String] = Option.empty
  protected val apiPrefix: String = "api"
  protected val apiVersion: Option[String] = Option(configuration.apiVersion)

  protected val targetBase: String = "files"
  protected val id: String = filedId
  protected val isPersistentId: Boolean = isPersistentFileId

  private implicit val jsonFormats: Formats = DefaultFormats

  /**
   * Unfortunately, the body of the response is not valid JSON.
   *
   * @see [[https://guides.dataverse.org/en/latest/api/native-api.html#updating-file-metadata]]
   * @param fm file metadata
   * @return
   */
  def updateMetadata(fm: DataverseFile): Try[DataverseResponse[Nothing]] = {
    trace(fm)
    postFileToTarget[Nothing]("metadata", optFile = None, optMetadata = Option(Serialization.write(fm)))
  }

}
