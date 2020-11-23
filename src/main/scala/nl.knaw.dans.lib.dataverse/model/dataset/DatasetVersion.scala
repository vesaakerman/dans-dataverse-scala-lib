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
package nl.knaw.dans.lib.dataverse.model.dataset

case class DatasetVersion(id: Option[Int] = None,
                          datasetId: Option[Int] = None,
                          datasetPersistentId: Option[String] = None,
                          storageIdentifier: Option[String] = None,
                          versionState: Option[String] = None,
                          lastUpdateTime: Option[String] = None,
                          createTime: Option[String] = None,
                          fileAccessRequest: Option[Boolean] = None,
                          termsOfUse: Option[String] = None,
                          license: Option[String] = None,
                          protocol: Option[String] = None,
                          authority: Option[String] = None,
                          identifier: Option[String] = None,
                          metadataBlocks: MetadataBlocks = Map.empty,
                          files: List[DataverseFile] = List.empty)
