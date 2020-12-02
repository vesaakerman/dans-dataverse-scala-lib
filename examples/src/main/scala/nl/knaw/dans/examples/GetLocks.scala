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

object GetLocks extends App with DebugEnhancedLogging with BaseApp {
  private implicit val jsonFormats: DefaultFormats = DefaultFormats
  private val persistentId = args(0)

  /*
   * Start the example and then do something with the dataset that causes a lock, such as ingesting a
   * tabular file.
   */
  for (_ <- Range(1, 300)) {
    for {
      response <- server.dataset(persistentId).getLocks
      locks <- response.data
      _ = if (locks.nonEmpty)
            logger.info(s"Dataset is currently locked by: ${
              if (locks.isEmpty) "NOTHING"
              else locks.map(_.lockType).mkString(", ")
            } ")
    } yield ()
    Thread.sleep(50)
  }
}