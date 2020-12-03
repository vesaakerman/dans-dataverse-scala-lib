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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.immutable.List;

public class ListFacets extends BaseApp {

  private static final Logger logger = LoggerFactory.getLogger(ListFacets.class);

  public static void main(String[] args) {
    logger.info("Starting {}", ListFacets.class.getName());

    try {
      DataverseResponse<List<String>> response = server.dataverse("root").listFacets().get();
      logger.info("Raw response message: {}", response.string());
      logger.info("JSON AST: {}", response.json());
      String[] facets = new String[response.data().get().size()];
      response.data().get().copyToArray(facets);
      logger.info("The following {} are active: {}", facets.length, StringUtils.join(facets, ", "));
    }
    catch (DataverseException e) {
      logger.error("List facets failed.", e);
    }
  }
}
