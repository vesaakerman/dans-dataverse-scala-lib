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

import nl.knaw.dans.lib.dataverse.model.{ Dataverse, DataverseContact }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.{ DefaultFormats, Formats }

object RoundTrip extends App with DebugEnhancedLogging with BaseApp {
  private implicit val jsonFormats: Formats = DefaultFormats

  val brave = Dataverse(
    name = "Brave New Dataverse",
    alias = "brave",
    dataverseType = "JOURNALS",
    dataverseContacts = List(
      DataverseContact(
        contactEmail = "a@huxley.org"
      )
    ))

  val subsub = Dataverse(
    name = "sub-universe",
    alias = "subsub",
    description = Some("A universe within a universe"),
    dataverseType = "JOURNALS",
    dataverseContacts = List(
      DataverseContact(
        contactEmail = "a@huxley.org"
      )
    ))

  val result = for {
    // Create a new subverse
    response <- server.dataverse("root").create(brave)
    dv <- response.data
    _ = logger.info(s"${ dv.alias } created")

    // View its info
    response <- server.dataverse("brave").view()
    dv <- response.data
    _ = logger.info(s"The type of dataverse is: ${dv.dataverseType}")

    // Create a subsubverse
    response <- server.dataverse("brave").create(subsub)
    dv <- response.data
    _ = logger.info(s"${ dv.description.getOrElse("n/a") } created")

    // Publish brave
    response <- server.dataverse("brave").publish()
    dv <- response.data
    _ = logger.info(s"Published dataverse '${dv.alias}' which was created at: ${dv.creationDate.getOrElse("n/a")}")

    // List facets
    response <- server.dataverse("brave").listFacets()
    facets <- response.data
    _ = logger.info(s"Active facets: ${facets.map(f=> s"'${f}'").mkString(",")}")

    // Create a new dataset
    // TODO: create dataset, add files, create versions, delete dataset?

    // Delete the subsubverse
    response <- server.dataverse("subsub").delete()
    m <- response.data
    _ = logger.info(s"${m.message}")

    // Delete the subverse
    response <- server.dataverse("brave").delete()
    m <- response.data
    _ = logger.info(s"${m.message}")
  } yield ()
  logger.info(s"Result = $result")
}