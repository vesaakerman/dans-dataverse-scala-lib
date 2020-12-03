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
package nl.knaw.dans.examplesjava;

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.DataverseResponse;
import nl.knaw.dans.lib.dataverse.model.Dataverse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewRootDataverse extends BaseApp {

  private static final Logger logger = LoggerFactory.getLogger(ViewRootDataverse.class);

  public static void main(String[] args) {
    logger.info("Starting ViewRootDataverse");
    try {
      DataverseResponse<Dataverse> response = server.dataverse("root").view().get();
      logger.info("Raw response message: {}", response.string());
      logger.info("JSON AST: {}", response.json());
      Dataverse dataverseInfo = response.data().get();
      logger.info("Dataverse description: '{}'", dataverseInfo.description().isDefined() ? dataverseInfo.description().get() : "NO DESCRIPTION FOUND");
    }
    catch (DataverseException e) {
      logger.error("View dataverse failed", e);
    }
  }
}
