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
package nl.knaw.dans.examples

import nl.knaw.dans.lib.dataverse.model.dataset.DataverseFile
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.DefaultFormats

object UpdateFileMetadata extends App with DebugEnhancedLogging with BaseApp {
  private implicit val jsonFormats: DefaultFormats = DefaultFormats
  private val databaseId = args(0).toInt
  private val description = args(1)
  private val directoryLabel = args(2)
  private val restrict = args(3).toBoolean

  val fileMetadata = DataverseFile(description = Some(description),
    directoryLabel = Some(directoryLabel),
    restrict = Some(restrict))

  val result = for {
    response <- server.file(databaseId).updateMetadata(fileMetadata)
    _ = logger.info(s"Raw response message: ${ response.string }")
    // Unfortunately, Dataverse gives invalid JSON in the response. The proper JSON is prefixed with the
    // text "File Metadata update has been completed".
  } yield ()
  logger.info(s"result = $result")
}