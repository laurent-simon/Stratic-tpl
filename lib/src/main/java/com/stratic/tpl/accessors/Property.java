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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * Access point to the properties of a class.
 * 
 * A property can be materialized by:
 * <ul>
 * <li>A field.</li>
 * <li>A couple of setter and getter methods, in the form of:
 *      <ul>
 *      <li>Standard java accessors (in the form {@code getXxxx() or IsXxxx() and setXxxx(value) })</li>
 *      <li>or methods with the property name (example: {@code xxx() and xx(value) }).</li>
 *      </ul>
 * </li>
 * </ul>
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public final class Property {

    @SuppressWarnings( "rawtypes" )
    private final static Class<?>[] NO_ARGS_PROTO = new Class[ 0 ];

    private Property() {
    }

    /**
     * Gets an accessor for a given property.
     * 
     * @param cls Class where the property is defined.
     * @param property Name of the property.
     * 
     * @return An {@link Accessor} that give access to the property. 
     */
    public final static Accessor accessorFor( final Class<?> cls, final String property ) {
        // en lecture
        final Getter getter = getterFor( cls, property );
        if ( getter == null ) {
            throw new AccessorException(
                    property,
                    "On class %s, the %s property is not readable", cls, property
            );
        }

        // en ecriture
        final Setter setter = setterFor( cls, property, getter.getType() );
        if ( setter == null ) {
            throw new AccessorException(
                    property,
                    "On class %s, the %s property is not writeable", cls, property
            );
        }

        return new Accessor( getter, setter );
    }

    /**
     * Gets a getter for a given property.
     * 
     * @param cls Class where the property is defined.
     * @param property Name of the property.
     * 
     * @return A {@link Setter} that allow to define the property value.
     */
    public final static Getter getterFor( final Class<?> cls, final String property ) {
        final Method m = findGetMethod( cls, property );
        return m != null ? (Getter) new MethodGetter( m ) : fieldGetterFor( cls, property );
    }

    /**
     * Gets a setter for a given property.
     *
     * @param cls      Class where the property is defined.
     * @param property Name of the property.
     *
     * @return  A {@link Setter} that allow to define the property value,
     *         or {@code null} if the class has no corresponding member.
     *
     */
    public final static Setter setterFor( final Class<?> cls, final String property ) {
        final Method m = findMethod( cls, setterName( property ) );
        return m != null ? (Setter) new MethodSetter( m ) : fieldSetterFor( cls, property );
    }

    /**
     * Gets a setter of a given type for a  property.
     *
     * @param cls      Class where the property is defined.
     * @param property Name of the property.
     * @param type     Expected type for the setter.
     *
     * @return  A {@link Setter} that allow to define the property value,
     *         or {@code null} if the class has no corresponding member.
     *
     */
    public final static Setter setterFor( final Class<?> cls, final String property, final Class<?> type ) {
        final Method m = findSetMethod( cls, property, type );
        return m != null ? (Setter) new MethodSetter( m ) : fieldSetterFor( cls, property );
    }

    private static FieldSetter fieldSetterFor( final Class<?> cls, final String property ) {
        final Field f = findField( cls, property );
        return f != null ? new FieldSetter( f ) : null;
    }

    private static FieldGetter fieldGetterFor( final Class<?> cls, final String property ) {
        final Field f = findField( cls, property );
        return f != null ? new FieldGetter( f ) : null;
    }

    private static Method findMethod( final Class<?> cls, final String mthName ) {
        final Method[] methods = cls.getDeclaredMethods();
        Method match = null;
        for ( int i = methods.length - 1; i >= 0; i-- ) {
            if ( methods[ i ].getName()
                    .equals( mthName ) && methods[ i ].getParameterTypes().length == 1 ) {
                if ( match == null ) {
                    match = methods[ i ];
                }
                else {
                    throw new AccessorException(
                            mthName,
                            "Unable to resolve property: There is more than one '%s' method on class '%s'", mthName, cls
                    );
                }

            }
        }
        if ( match == null ) {
            final Class<?> ancestor = cls.getSuperclass();
            return ancestor != null ? findMethod( ancestor, mthName ) : null;
        }
        return match;
    }

    /**
     * Checks if a given method is a good candidate to set a property value.
     *
     * @param mth Method to check.
     *
     * @return {@code true} if the method fits, else {@code false}.
     *
     */
    private static boolean isValidSetter( final Method mth ) {
        final int modifiers = mth.getModifiers();
        if ( Modifier.isStatic( modifiers )
             || mth.getParameterTypes().length != 1 ) {
            return false;
        }
        if ( !Modifier.isPublic( modifiers ) ) {
            mth.setAccessible( true );
        }
        return true;
    }

    /**
     * Checks if a given method is a good candidate to read a property value.
     *
     * @param mth Method to check.
     *
     * @return {@code true} if the method fits, else {@code false}.
     *
     */
    private static boolean isValidGetter( final Method mth ) {
        final int modifiers = mth.getModifiers();
        if ( Modifier.isStatic( modifiers )
             || mth.getParameterTypes().length > 0 ) {
            return false;
        }
        if ( !Modifier.isPublic( modifiers ) ) {
            mth.setAccessible( true );
        }
        return true;
    }

    /**
     * Checks if a field is a good candidate as a property.
     *
     * @param field Field to check.
     *
     * @return {@code true} if the field fits, else {@code false}.
     *
     */
    private static boolean isValid( final Field field ) {
        final int modifiers = field.getModifiers();
        if ( Modifier.isStatic( modifiers ) ) {
            return false;
        }
        if ( !Modifier.isPublic( modifiers ) ) {
            field.setAccessible( true );
        }
        return true;
    }

    private static Method findSetMethod( final Class<?> cls, final String name, final Class<?> type ) {
        
        @SuppressWarnings( "rawtypes" )
        Method m = findMethod( cls, setterName( name ), new Class[]{type} );
        
        if ( m != null && !isValidSetter( m ) ) {
            throw new IllegalArgumentException( 
                    "Le méthode '" + m.getName() + "' n'est pas une méthode d'instance ou retourne un type non reconnu" );
        }
        return m;
    }

    private static Method findGetMethod( final Class<?> cls, final String name ) {

        final Method m = findMethod(
                cls,
                new String[]{accessorName( "get", name ), accessorName( "is", name ), name},
                NO_ARGS_PROTO
        );

        if ( m == null ) {
            return null;
        }
        if ( !isValidGetter( m ) ) {
            throw new IllegalArgumentException( 
                    "Le méthode '" + m.getName() + "' n'est pas une méthode d'instance ou retourne un type non reconnu" );
        }
        return m;
    }

    private static String setterName( final String name ) {
        return accessorName( "set", name );

    }

    private static String accessorName( final String prefix, final String name ) {
        String mthName = prefix + name.substring( 0, 1 )
                .toUpperCase();
        if ( name.length() > 1 ) {
            mthName += name.substring( 1 );
        }
        return mthName;
    }

    private static Method findMethod( final Class<?> cls, final String[] mthNames, final Class<?>[] args ) {
        for( String mthName : mthNames ) {
            final Method m = findMethod( cls, mthName, args );
            if ( m != null ) {
                return m;
            }
        }
        return null;
    }

    private static Method findMethod( final Class<?> cls, final String mthName, final Class<?>[] args ) {
        try {
            return cls.getDeclaredMethod( mthName, args );
        }
        catch ( final Exception e ) {
            final Class<?> ancestor = cls.getSuperclass();
            return ancestor != null ? findMethod( ancestor, mthName, args ) : null;
        }
    }

    private static Field findField( final Class<?> cls, final String name ) {
        try {
            final Field f = cls.getDeclaredField( name );
            if ( f == null ) {
                return null;
            }
            if ( !isValid( f ) ) {
                throw new IllegalArgumentException( 
                        "L'attribut '" + name + "' n'est pas un attribut d'instance ou est de type non reconnu" );
            }
            return f;

        }
        catch ( final Exception e ) {
            final Class<?> ancestor = cls.getSuperclass();
            if ( ancestor != null ) {
                return findField( ancestor, name );
            }
            return null;
        }
    }
}
