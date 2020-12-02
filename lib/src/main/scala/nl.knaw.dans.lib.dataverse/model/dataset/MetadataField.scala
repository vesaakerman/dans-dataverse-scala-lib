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

/**
 * A generic metadata field. Metadata fields of all the type classes have these three attributes in common. The value of the field has a
 * different type for each type class.
 *
 * @param typeClass the type class of the field (primitive, controlledVocabulary, compound)
 * @param typeName the field ID
 * @param multiple whether this field allows multiple values
 */
abstract class MetadataField(val typeClass: String, val typeName: String, val multiple: Boolean)
