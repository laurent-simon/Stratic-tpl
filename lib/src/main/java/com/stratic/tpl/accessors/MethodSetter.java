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

import java.lang.reflect.Method;

/**
 * A {@link Setter} able to set values using a method.
 * 
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
final class MethodSetter implements Setter {

    /**
     * Method to be used. *
     */
    private final Method mth;

    /**
     * Creates a new instance of MethodSetter.
     * 
     * @param mth Method to be used.
     */
    MethodSetter( final Method mth ) {
        this.mth = mth;
    }

    @Override
    public void set( final Object target, final Object value ) {
        try {
            mth.invoke( target, new Object[]{value} );
        }
        catch ( final Exception e ) {
            throw new AccessorException(
                    e,
                    mth.getName(),
                    "Unable to set property value with method '%s'", mth
            );
        }
    }

    @Override
    public Class<?> getType() {
        return mth.getParameterTypes()[ 0 ];
    }

}
