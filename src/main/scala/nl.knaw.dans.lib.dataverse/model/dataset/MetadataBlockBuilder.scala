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

import scala.collection.mutable

case class MetadataBlockBuilder(displayName: String) {
  private type FieldId = String
  private val fields = mutable.ListBuffer[MetadataField]()

  def withSingleValueField(fieldId: FieldId, value: String): MetadataBlockBuilder = {
    fields.append(PrimitiveSingleValueField(typeName = fieldId, multiple = false, typeClass = "primitive", value = value))
    this
  }

  def withMultiValueField(fieldId: FieldId, values: List[String]): MetadataBlockBuilder = {
    fields.append(PrimitiveMultipleValueField(typeName = fieldId, multiple = true, typeClass = "primitive", value = values))
    this
  }

  def withControlledSingleValueField(fieldId: FieldId, value: String): MetadataBlockBuilder = {
    fields.append(PrimitiveSingleValueField(typeName = fieldId, multiple = false, typeClass = "controlledVocabulary", value = value))
    this
  }

  def withControlledMultiValueField(fieldId: FieldId, values: List[String]): MetadataBlockBuilder = {
    fields.append(PrimitiveMultipleValueField(typeName = fieldId, multiple = true, typeClass = "controlledVocabulary", value = values))
    this
  }

  def withCompoundField(compoundField: MetadataField): MetadataBlockBuilder = {
    fields.append(compoundField)
    this
  }

  def build(): MetadataBlock = {
    MetadataBlock(
      displayName = displayName,
      fields = fields.toList
    )
  }
}
