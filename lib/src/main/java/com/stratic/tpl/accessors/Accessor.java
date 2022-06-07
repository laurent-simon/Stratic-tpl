/*
 * Copyright 2007 Laurent SIMON
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
package com.stratic.tpl.accessors;

/**
 * Accessor used to dynamically read and write a specific property on an object.
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public final class Accessor implements Getter, Setter {

    /** Getter used to read the property value. */
    private final Getter getter;

    /** Setter used to set the property value. */
    private final Setter setter;

    /**
     * Creates a new instance of Accessor.
     * @param getter Value getter
     * @param setter Value setter
     */
    Accessor( final Getter getter, final Setter setter ) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Object get( final Object target ) {
        return getter.get( target );
    }

    @Override
    public Class<?> getType() {
        return getter.getType();
    }

    @Override
    public void set( final Object target, final Object value ) {
        setter.set( target, value );
    }

}
