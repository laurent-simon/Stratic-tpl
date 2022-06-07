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
 * Exception thrown a property value cannot be accessed.
 * This occurs when a property did not exists or is not accessible.
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public class AccessorException extends RuntimeException {

    private static final long serialVersionUID = -3612641430001705796L;

    /** Name of the property that cannot be accessed. */
    private final String property;

    
    /**
     * Creates a new instance of AccessorException.
     *
     * @param property Property name (with it's full path).
     * @param msg Error message.
     * @param args Message arguments
     */
    public AccessorException( final String property, final String msg, final Object... args ) {
        this( (Throwable) null, property, msg );
    }
        
    /**
     * Creates a new instance of AccessorException.
     *
     * @param origin Origin of the error
     * @param property Property name (with it's full path).
     * @param msg Error message.
     * @param args Message arguments
     */
    public AccessorException( final Throwable origin, final String property, final String msg, final Object... args  ) {
        super( args.length > 0 ?  String.format( msg, args) : msg, origin );
        this.property = property;
    }

    /**
     * Gets the name of the property that cannot be accessed.
     *
     * @return the name of the property that cannot be accessed.
     */
    public String getPropertyName() {
        return property;
    }
}
