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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * The factory Loader is the templating framework entry point.
 * It's an engine able to locate templates source code and build corresponding template's factories.
 * It act for a {@link Factory factory}, like a Java class loader act for a class.
 * <p>
 * The {@link #getDefault() default implementation} looks for templates definitions in current classpath.
 * By extending this class, more specific implementations can load templates from any source (from a database, for example).
 * <p>
 * Like class loaders, factory loaders can be chained together to form a cascading delegation system.
 * If a resource is not resolved at a given level, then, the resolution is delegated to the parent level. 
 *
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public class FactoryLoader {
    
   
    /**
     * Signal that a template definition could not be located.
     *
     **/
    public final static class TemplateNotFound extends RuntimeException {

        private static final long serialVersionUID = 3645503406379331219L;
	
        /** Name of the template that was not found. **/
	private final String name;
	
	/**
	 * Create a new TemplateNotFound exception.
	 *
	 * @param name template name
	 *
	 **/
	protected TemplateNotFound( final String name ) {
	    super("Unable to locate template '" + name + "'");
	    this.name = name;
	}
	
	
	/**
	 * Create a new TemplateNotFound exception.
	 *
	 * @param name template name.
         * @param reason Original cause.
	 *
	 **/
	public TemplateNotFound( final String name, Throwable reason ) {
	    super("Unable to load template '" + name + "': " + reason.getMessage(), reason);
	    this.name = name;
	}	

        /**
         * Gets the name of the template that was not found.
         * 
         * @return The name of the template that was not found.
         */
        public final String getFactoryName() {
	    return name;
	}
	
    }

    private static ClassLoader defaultClassLoader() {
        return (FactoryLoader.class).getClassLoader() ;
    }
    
    /**
     * Get the default factory loader.
     *
     * This default loader locate templates as system resources using the current classpath.
     * 
     * @return The default factory loader.
     **/
    public final static FactoryLoader getDefault() {
	return new FactoryLoader( null, defaultClassLoader(), Charset.defaultCharset() );
    }

    /**
     * Get the default factory loader for a given character set.
     *
     * This default loader locate templates as system resources using the current classpath.
     * 
     * @param charset The character set to be used by the factory loader.
     * 
     * @return A default factory loader using the given character set.
     **/
    public final static FactoryLoader getDefault( final Charset charset ) {
	return new FactoryLoader( null, defaultClassLoader(), charset );
    }
    
    /**
     * Get a default factory loader that uses a given class loader.
     *
     * This default loader locate templates as system resources using the given classpath.
     *
     * @param classLoader Class loader where to look for resources.
     *
     * @return A default factory loader that load resources using the given class loader.
     **/
    public final static FactoryLoader getDefault( final ClassLoader classLoader ) {
	return new FactoryLoader( null, classLoader, Charset.defaultCharset() );
    }

    /**
     * Get a default factory loader that uses given class loader and character set.
     *
     * This default loader locate templates as system resources using the given classpath.
     * 
     * @param classLoader Class loader where to look for resources.
     * @param charset The character set to be used by the factory loader.
     * 
     * @return A default factory loader that load resources using the given class loader and character set.
     **/
    public final static FactoryLoader getDefault( final ClassLoader classLoader, final Charset charset ) {
	return new FactoryLoader( null, classLoader, charset );
    }

    /** Parent loader **/
    private final FactoryLoader parent;
    
    /** templates factories map **/
    private final HashMap<String,Factory> factories = new HashMap<>();
    
    /** Class loader to use to locate resources **/
    private final ClassLoader classLoader;

    /** Templates files encoding for this factory loader **/
    private final Charset charset;
    
    /**
     * Create a new Factory loader
     *
     * @param parent loader
     * @param classLoader Class loader to load resources.
     * @param charset Character set used for templates.
     **/
    protected FactoryLoader( final FactoryLoader parent, final ClassLoader classLoader, final Charset charset ) {
        this.classLoader = classLoader;
	this.parent = parent;
        this.charset = charset;
    }
    
    /**
     * Get a given factory by its name.
     * If the factory has not been created yet, then,
     * if the corresponding source code is found, the factory is automatically created.
     * <p>
     * The source code resource resolution depends on factory loader implementation.
     * The default implementation looks for a file with the given name.
     *
     * @param name Factory name to get
     *
     * @return Corresponding factory or <code>null</code> if no factory with given name exists.
     *
     * @throws FactoryLoader.TemplateNotFound If template could not be located.
     * @throws Template.Error If template definition contain errors.
     **/
    public final Factory getFactory( final String name ) throws TemplateNotFound, Template.Error {
	Factory f;
	for (FactoryLoader ldr = this; ldr != null; ldr = ldr.parent ) {
	    if ( (f = ldr.locateFactory(name) ) != null) {
		return f;
	    }
	}
	// no loader was able to locate it
	throw new TemplateNotFound(name);
    }
    
    
    /**
     * Lookup for an existing factory.
     *
     * @param name factory name to look for.
     * 
     * 
     * @return The requested factory, or {@code null} if there is no factory with the given name.
     **/
    private Factory locateFactory( final String name ) throws Template.Error, TemplateNotFound {

	// lookup in local table
	final Factory f = factories.get( name );
	if ( f != null ) {
		// found => return  it
		return f;
	}
	
	
	// not already loaded => look if it is defined here
	final char[] src = loadSourceCode( name );
	if (src == null) {
	    // not defined here => abort
	    return null;
	}
	
	// Factory defined here => build it.
        return create( name, src );
    }
    
    
    /**
     * Creates a new template factory.
     * 
     * @param name Name of the factory to be created.
     * @param src Source code of the templates created by the factory.
     * 
     * @return A factory able to create templates with the given source code.
     */    
    public Factory create( final String name, final String src ) {
       return create( name, src.toCharArray() );
    }
    
    
    /**
     * Creates a new template factory.
     * 
     * @param name Name of the factory to be created.
     * @param src Source code of the templates created by the factory.
     * 
     * @return A factory able to create templates with the given source code.
     */
    public Factory create( final String name, final char[] src ) {
       // Factory defined here => build it.
	final Engine eng = new Engine( null, name, null);
	factories.put( name, eng); // register the new factory before parse to allow recursive references
	try {
	    eng.parse( this, src );
	}
	catch ( Template.Error err ) {
	    factories.remove( name ); // remove invalid entry
	    throw err;
	}
	return eng;
    }
    
    /**
     * Gets source code from a resource name.
     * 
     * @param name Factory name.
     * 
     * @return Source code for the factory with the given name.
     * 
     * @throws com.stratic.tpl.FactoryLoader.TemplateNotFound When corresponding source code cannot be found.
     */
    protected char[] loadSourceCode( final String name ) throws TemplateNotFound {
        try (
            final InputStream stream = this.classLoader.getResourceAsStream( name );
            final InputStreamReader reader = stream == null ? null : new InputStreamReader(
                    new BufferedInputStream( stream, 3000 ),
                    this.charset
            )
        ) {
            return reader == null ? null : readSource( reader, stream.available() );
        }
        catch ( final IOException e ) {
            throw new TemplateNotFound( name, e);
        }
    }
        
    private static char[] readSource( final InputStreamReader reader, final int capacity ) throws IOException {
        final StringBuilder builder = new StringBuilder( capacity );
        for ( int c = reader.read(); c >= 0; c = reader.read() ) {
            builder.append( (char) c );
        }
        return toChars( builder );
    }
    
    private static char[] toChars( final StringBuilder builder ) {
        final char[] chars = new char[ builder.length() ];
        builder.getChars( 0, chars.length, chars, 0 );
        return chars;        
    }
    
    /**
     * Helper function used to read factory definition from a file.
     *
     * @param f File where to read the definition
     *
     * @return corresponding source code
     * 
     * @throws java.io.IOException If the file cannot be read.
     **/
    protected final char[] readFromFile( final File f ) throws IOException {
	try ( // try to build it
            final FileInputStream stream = new FileInputStream( f )
        ) {
	    final byte[] bytes = new byte[ stream.available() ];
	    stream.read( bytes );
	    return ( new String( bytes ) ).toCharArray();
	}
    }
     
}
