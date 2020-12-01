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

case class ControlledSingleValueField(override val typeClass: String, override val typeName: String, override val multiple: Boolean, value: String) extends MetadataField(typeClass, typeName, multiple)

object ControlledSingleValueField {
  def apply(typeName: String, value: String): ControlledSingleValueField = new ControlledSingleValueField(TYPE_CLASS_CONTROLLED_VOCABULARY, typeName, multiple = false, value)
}
