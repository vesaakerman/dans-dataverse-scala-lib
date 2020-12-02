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
package nl.knaw.dans.lib.dataverse.model

case class DataverseItem(/*
                          * Both
                          */
                         `type`: String,
                         id: Int,

                         /*
                          * Only dataverses
                          */
                         title: Option[String],

                         /*
                          * Only datasets
                          */
                         identifier: Option[String],
                         persistentUrl: Option[String],
                         protocol: Option[String],
                         authority: Option[String],
                         publisher: Option[String],
                         publicationDate: Option[String],
                         storageIdentifier: Option[String])


