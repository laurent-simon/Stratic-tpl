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
import java.io.Writer;
import java.util.Arrays;
import java.util.EmptyStackException;

/**
 * Utility class used to format properly templates results.
 * <p>
 * This utility handles the indentation for embedded templates.
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
final class TypeWriter {

    /**
     * templates call stack
     */
    private Template[] stack = new Template[ 30 ];

    /**
     * current level in stack *
     */
    private int level = -1;

    /**
     * Indentation stack.
     * This stack contain fillers to use for each indentation level.
     *
     */
    private final Fifo<char[]> indentStack = new Fifo<>( 16 );

    /**
     * Current filler.
     * there is no filler when it is null.
     *
     */
    private char[] filler = null;

    /**
     * Final destination where to write
     *
     */
    private final Writer dest;

    /**
     * line buffer.
     * This variable contain characters of current line already send to destination.
     * It is use to capture fillers content.
     *
     */
    private char[] buf = new char[ 256 ];

    /**
     * current line position *
     */
    private int size = 0;

    /**
     * Creates a new instance of CodeWriter
     */
    TypeWriter( final Writer writer ) {
        this.dest = writer;
    }

    /**
     * Extend line buffer size.
     * The buffer size is increased by approximately 50%
     *
     */
    private void extendbuffer() {
        final char[] old = buf;
        buf = new char[ buf.length + ( buf.length / 2 ) + 1 ];
        System.arraycopy( old, 0, buf, 0, size );
    }

    /**
     * Append a single character.
     *
     * @param c character to write.
     *
     */
    final void write( final char c ) throws IOException {
        dest.write( c );	    // write it to destination
        if ( c == '\n' ) {	    // if it is a linefeed
            if ( filler != null ) {	// if there is an active filler
                // write corresponding characters to destination
                dest.write( filler );

                // keep line buffer in sync
                size = filler.length;
                System.arraycopy( filler, 0, buf, 0, size );	// remark : no test required => buf size cannot be less than filler size
                return;
            }
            // no filler => just reset line buffer position
            size = 0;
            return;
        }
        // normal character
        if ( size == buf.length ) { // if line buffer is full
            extendbuffer();		// extend it
        }
        buf[ size++ ] = c;	    // push character to line buffer
    }

    /**
     * Append a string to current destination.
     *
     * @param s String to write.
     *
     */
    final void write( final String s ) throws IOException {
        final int len = s.length();
        for ( int i = 0; i < len; i++ ) {
            write( s.charAt( i ) );
        }
    }

    /**
     * Template stack overflow handling (double the stack size).
     *
     */
    private void overflow() {
        final Template[] newStack = new Template[ stack.length * 2 ];
        System.arraycopy( stack, 0, newStack, 0, stack.length / 2 );
        stack = newStack;
    }

    /**
     * Push a template on top of context stack.
     *
     * @param t template to push in context stack.
     *
     */
    final void push( final Template t ) {
        if ( ++level == stack.length ) {
            overflow();
        }
        stack[ level ] = t;
    }

    /**
     * Remove the last template from context stack.
     *
     */
    final void pop() {
        --level;
    }

    /**
     * Get a value from current context.
     *
     * @param name variable name for which value is requested.
     *
     * @return Current value or <code>null</code> if there is no corresponding variable in current context.
     *
     */
    final String get( final String name ) {
        String val;
        for ( int i = level; i >= 0; i-- ) {
            if ( ( val = stack[ i ].get( name ) ) != null ) {
                return val;
            }
        }
        return null;
    }

    /**
     * Get a value from current context.
     *
     * @param name variable name for which value is requested.
     *
     * @return Current value or <code>null</code> if there is no corresponding variable in current context.
     *
     */
    final Object getObject( final String name ) {
        Object val;
        for ( int i = level; i >= 0; i-- ) {
            if ( ( val = stack[ i ].getObject( name ) ) != null ) {
                return val;
            }
        }
        return null;
    }

    /**
     * Define a nested indentation point at current position.
     * Current line content became the new filler
     * After this each new line will be prefixed this filler.
     *
     */
    final void pushIndent() {
        indentStack.push( filler );   // memo previous filler value in stack to restore it later

        // define the new filler
        if ( size > 0 ) { // if current line is not empty
            // use it's content as a filler
            filler = new char[ size ];
            System.arraycopy( buf, 0, filler, 0, size );
            return;

        }
        // no filler
        filler = null;
    }

    /**
     * Remove current indentation
     *
     */
    final void popIndent() {
        // restore previous filler from stack 
        try {
            filler = indentStack.pop();
        }
        catch ( EmptyStackException e ) {
            // we are back to ground level
            filler = null;
        }
    }
    
    /**
     * Simple FIFO stack, more efficient than Stack and lighter than Dequeue implementations.
     * 
     * @param <T> Type of data stored in the stack.
     */
    private final static class Fifo<T>  {
        private Object[] stack;
        private int idx = -1;
        final int increment;
        
        Fifo( final int capacity ) {
            this.increment = capacity;
            this.stack = new Object[capacity];
        }
        
        void push( final T elem ) {
             if ( ++idx == stack.length ) {
                 stack = Arrays.copyOf( stack, newCapacity() );
             }
             stack[ idx ] = elem;
        }
        
        private int newCapacity() {
            final int len = stack.length;
            if ( len == Integer.MAX_VALUE ) {
                throw new OutOfMemoryError( "Fifo stack size overflow" );
            }
            return len >= Integer.MAX_VALUE - increment ? Integer.MAX_VALUE : len + increment;
        }
        
        @SuppressWarnings("unchecked")
        public T peek() {
            return idx < 0 ? null : (T) stack[idx];
        }
        
        @SuppressWarnings("unchecked")
        public T pop() {
            return idx < 0 ? null : (T) stack[idx--];
        }
        
    }

}
