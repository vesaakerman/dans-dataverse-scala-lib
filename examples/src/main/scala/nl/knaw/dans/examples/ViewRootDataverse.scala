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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.DefaultFormats
import org.json4s.native.Serialization

object ViewRootDataverse extends App with DebugEnhancedLogging with BaseApp {
  private implicit val jsonFormats: DefaultFormats = DefaultFormats
  val result = for {
    response <- server.dataverse("root").view()
    _ = logger.info(s"Raw response message: ${ response.string }")
    _ = logger.info(s"JSON AST: ${ response.json }")
    _ = logger.info(s"JSON serialized: ${ Serialization.writePretty(response.json) }")
    dataverseSummary <- response.data
    _ = logger.info(s"Description of the dataverse: '${ dataverseSummary.description.getOrElse("NO DESCRIPTION FOUND") }'")
  } yield ()
  logger.info(s"result = $result")
}
