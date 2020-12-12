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

import nl.knaw.dans.lib.dataverse.model.Lock

/**
 * Thrown by [[DatasetApi#awaitUnlock]] if the maximum number of tries is reached and the dataset is still locked.
 *
 * @param numberOfTimesTried how many times the unlock check was tried
 * @param waitTimeInMilliseconds time waited between tries
 * @param locks list of locks found in last try
 */
case class LockException(numberOfTimesTried: Int, waitTimeInMilliseconds: Int, locks: List[Lock]) extends RuntimeException(s"Still locked after $numberOfTimesTried times with $waitTimeInMilliseconds millisecond pauses. Locks: ${ locks.mkString(", ") }")

