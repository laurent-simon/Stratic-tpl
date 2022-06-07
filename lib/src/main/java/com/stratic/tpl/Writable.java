/*
 * Copyright 2003 Laurent SIMON
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratic.tpl;

import java.io.IOException;

/**
 * Any part of a template that is writable as a result of the template rendering.
 * <p>
 * Remark: this can be an interface but it is an abstract class to avoid to publish an internal method has public.
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
abstract class Writable {

    protected Writable() {
    }

    /**
     * Writes current content to a given destination.
     *
     * @param dest Destination where to write current content.
     *
     * @throws IOException if an error occur when writing to final destination.
     *
     */
    abstract void write( final TypeWriter dest ) throws IOException;

}
