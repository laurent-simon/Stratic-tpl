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
 * Dynamic expressions interpreter.
 * It allows to read a property value using a path expression (like {@code ${person.address.city.name}} }).
 * When there is a {@code null} value on the path, then the overall expression evaluates to {@code null}.
 * <p>
 * An {@code ExpressionGetter} is in fact a linked list of {@code ExpressionGetter}s.
 * Each {@code ExpressionGetter} evaluates a member of the path expression.
 * The global expression value is the last value in the linked list.
 * <p>
 * Path members can be of any type (concrete, abstract class or interface).
 * Concrete cases are discovered dynamically at runtime.
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public final class ExpressionGetter {

    /**
     * Specific accessor to be used for each possible concrete class of the object containing the evaluated member.
     * <p>
     */
    private Getters getters = null;

    /**
     * Name of the accessed property.
     */
    private final String member;

    /**
     * Full expression path.
     */
    private final String path;

    /**
     * Next {@link ExpressionGetter} in the path. *
     */
    private final ExpressionGetter next;

    /**
     * Creates a new instance of ExpressionGetter.
     * 
     * @param path Path of the expression to be read (example: {@code customer.addreess.city.name}).
     */
    private ExpressionGetter( final String path ) {

        this.path = path;
        final int dot = path.indexOf( '.' );
        this.member = dot < 0 ? path : path.substring( 0, dot );  // solves only the first member of the path
        this.next = dot > 0 ? new ExpressionGetter( path.substring( dot + 1 ) ) : null;
    }

    /**
     * Gets an {@link ExpressionGetter} which is able to evaluate a given expression.
     * <p>
     * At this stage, the expression correctness is not checked.
     * It will be later at runtime, against a specific class.
     *
     * @param expr The expression to be read.
     *
     * @return An {@link ExpressionGetter} able to read the expression, or {@code null} if the expression is empty.
     */
    public final static ExpressionGetter forExpression( final String expr ) {
        return expr != null && expr.length() > 0 ? new ExpressionGetter( expr ) : null;
    }

    /**
     * Gets the expression solved by this {@code ExpressionGetter}.
     *
     * @return The expression solved by this {@code ExpressionGetter}.
     */
    public final String getExpression() {
        return path;
    }

    /**
     * Gets the expression value for a given object.
     *
     * @param object An object where the expression can be evaluated.
     *
     * @return The value of this expression for the given object.
     */
    public final Object get( final Object object ) {
        if ( object == null ) {
            return null;
        }

        final Object value = getter( object.getClass() )
                .get( object );
        return next == null ? value             // last value => it's the one we are looking for 
                        : value == null ? null  // No more values => The overall expression cannot be evaluated
                        : next.get( value );    // get following value from this one
    }

    private Getter getter( final Class<?> cls ) {
        final Getter get = existingGetter( cls );
        return get != null ? get : newGetter( cls );
    }

    private Getter existingGetter( final Class<?> cls ) {
        return getters != null ? getters.get( cls ) : null;
    }

    private Getter newGetter( final Class<?> cls ) {
        final Getter get = Property.getterFor( cls, member );
        if ( get == null ) {
            throw new AccessorException(
                member,
                "'%s' expression is not valid: '%s' did not exists on class '%s'", path, member, cls
            );
        }
        getters = new Getters( getters, get );
        return get;
    }

    /**
     * Member's possible getters (one for each possible concrete class).
     */
    private final static class Getters {
        // Implementation choice:
        //  - There is only very few possible concrete classes for a member
        //  - A linked list is a good choice for that.

        /**
         * Current getter in the list.
         */
        private final Getter getter;
        /**
         * Next list part. *
         */
        private final Getters next;

        private Getters( final Getters next, final Getter getter ) {
            this.getter = getter;
            this.next = next;
        }

        /**
         * Gets the specific getter to use for a given class.
         * 
         * @param cls Class for which the getter is requested.
         * 
         * @return the specific getter to use for the given class.
         */
        private Getter get( final Class<?> cls ) {
            for ( Getters getters = this; getters != null; getters = getters.next ) {
                if ( getters.getter.getType() == cls ) {
                    return getters.getter;
                }
            }
            return null;
        }
    }
}
