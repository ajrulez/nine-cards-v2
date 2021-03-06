/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cards.nine.commons.utils

import java.io._

import cards.nine.commons.contexts.ContextSupport
import cards.nine.commons.utils.impl.StreamWrapperImpl

import scala.util.Try
import scala.util.control.Exception._

class FileUtils(streamWrapper: StreamWrapper = new StreamWrapperImpl)
    extends ImplicitsAssetException {

  def readFile(filename: String)(implicit context: ContextSupport): Try[String] =
    Try {
      withResource[InputStream, String](streamWrapper.openAssetsFile(filename)) { stream =>
        {
          streamWrapper.makeStringFromInputStream(stream)
        }
      }
    }

  private[this] def withResource[C <: Closeable, R](closeable: C)(f: C => R) =
    allCatch.andFinally(closeable.close())(f(closeable))

}
