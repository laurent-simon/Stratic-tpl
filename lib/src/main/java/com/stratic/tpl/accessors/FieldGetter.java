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

import java.lang.reflect.Field;

/**
 * A {@link Getter} able to read the value of a field.
 * 
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
final class FieldGetter implements Getter {

    /** The field used to read the value. **/
    final Field field;

    /**
     * Creates a new instance of FieldGetter
     * 
     * @param field The field used to get the value.
     */
    public FieldGetter( final Field field ) {
        this.field = field;
    }

    @Override
    public Object get( final Object target ) {
        try {
            return field.get( target );
        }
        catch ( final Exception e ) {
            throw new AccessorException(
                    e,
                    field.getName(),
                    "Unable to read value of field '%s'", field
            );
        }
    }

    @Override
    public Class<?> getType() {
        return field.getType();
    }

}