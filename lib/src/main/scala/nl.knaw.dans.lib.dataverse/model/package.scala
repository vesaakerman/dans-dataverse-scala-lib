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

import nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlock

/**
 * Classes that model the JSON objects that Dataverse produces and consumes.
 *
 * TODO: add few simple examples.
 */
package object model {


  /**
   * Enumeration of the default roles that can be assigned.
   *
   * Note: if you are using the API directly, the role names are all lowercase.
   */
  object DefaultRole extends Enumeration {
    type DefaultRole = Value
    val curator, contributor, none = Value
  }
}

