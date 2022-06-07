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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Set;


/**
 * A template is an instance of textual source code.
 * It is created by a {@link Factory}.
 * It can be used without knowing anything about its textual content, but only the names of the tags
 * and variables.
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public final class Template extends Writable {

   /**
    * Empty string constant.
    * Just used to avoid repeating a constant string.
    * */
   private final static String EMPTY_STRING = "";


   /**
    * Exception thrown when an error is encountered during template parsing.
    *
    * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
    * */
   public static final class Error extends RuntimeException {

        private static final long serialVersionUID = -1883738444700005379L;

      /** Name of the template where the error was detected. **/
      private final String name;

      /** template source code. **/
      final char[] src;

      /** line where the error was detected. **/
      private final int line;

      /** column where the error was detected. **/
      private final int col;

      /** absolute position of the error in the text. **/
      private final int idx;


      Error( final String name, final char[] src, final int idx, final int line, final int col, final String msg ) {
         super( msg + " at line " + ( line + 1 ) + " column " + ( col + 1 ) + " in template " + name + "." );
         this.name = name;
         this.src = src;
         this.idx = idx;
         this.line = line;
         this.col = col;
      }


      /**
       * Get the line number where the error was detected.
       *
       * @return the line number where the error was detected (starting from 0).
       * */
      public final int getLine() {
         return line;
      }


      /**
       * Get the column number where the error was detected.
       *
       * @return the column number where the error was detected (starting from 0).
       * */
      public final int getColumn() {
         return col;
      }


      /**
       * Gets the absolute character's position where the error was detected in the text.
       *
       * @return the position where the error was detected in the text (starting from 0).
       * */
      public final int getErrorIndex() {
         return idx;
      }


      /**
       * Gets the name of the template where the error was detected.
       *
       * @return the name of the template.
       * */
      public final String getTemplateName() {
         return name;
      }


      /**
       * Gets the template source code.
       *
       * @return Source code where the error was encountered.
       * */
      public final String getSource() {
         return new String( src );
      }

   }


   /**
    * Exception thrown when attempting to access a tag that does not exist in a template.
    * */
   public static final class UnknowTag extends RuntimeException {

        private static final long serialVersionUID = 7156468702352611285L;

      /**
       * Create a new exception.
       *
       * @param tag Invalid tag name.
       * */
      UnknowTag( final String tag ) {
         super( "tag '" + tag + "' does not exist." );
      }
   }

   /** Engine used to handle this template. * */
   final Engine factory;

   /** Collected elements. * */
   final Writable[] elements;

   /** variables values * */
   final HashMap<String,Object> vars = new HashMap<>( 10 );


   /**
    * Create a new Template.
    *
    * @param factory Engine used for creation
    * @param content Elements to put in the template.
    * */
   Template( final Engine factory, final Writable[] elements ) {
      this.factory = factory;
      this.elements = elements;
   }


   /**
    * Get an embedded tag inside this template.
    * The returned <code>Tag</code> can be used future to apply it.
    * 
    * <p>
    * Using this is recommended when a tag need be applied multiple times in a loop.
    * For example :
    * </p>
    * Instead of writing :
    * <pre>{@code
    * 
    *	    Template mail = ...
    *	    String[] recipients  = ...
    *	    for ( int i = 0; i < recipients.length; i++) {
    *		mail.add("Recipient").set("name", recipient[i]);
    *	    }
    * 
    * }</pre>
    * You can write :
    * <pre>{@code
    * 
    *	    Template mail = ...
    *	    final Tag recipientsTag = mail.getTag("Recipient");
    *	    String[] recipients  = ...
    *	    for ( int i = 0; i < recipients.length; i++) {
    *		recipientsTag.add().set("name", recipient[i]);
    *	    }
    * 
    * }</pre>
    *
    * This second form is faster because the "Recipients" tag name resolution is done only once.
    *
    * @param name Name of the tag to get.
    *
    * @return The embeded tag with the given name.
    *
    * @throws Template.UnknowTag if the given tag name did not exist.
    * */
   public final Tag getTag( final String name ) throws UnknowTag {
      return (Tag) elements[factory.tagIndex( name )];
   }


   /**
    * Create a new template using the given tag as a factory.
    * The new template is not inserted anywhere.
    * <p>
    * It is a shortcut for the following code :
    * <pre>
    *   getTag( name ).getFactory().create();
    * </pre>
    * Avoid to use this shortcut in loops.
    *
    * @param name Name of the tag to be used as a factory.
    *
    * @return a new template ready to be inserted somewhere else with the <code>add</code> method.
    *
    * @throws UnknowTag when tag with given name did not exits.
    *
    * @see #getTag( String )
    * */
   public final Template create( final String name ) throws UnknowTag {
      return getTag( name ).getFactory().create();
   }


   /**
    * Create and insert a new template in a tag using it's default template definition.
    * If there is variables in the nested template they should be filled later.
    * <p>
    * It is a shortcut for the following code :
    * <pre>
    *   getTag( name ).create();
    * </pre>
    *
    * @param name Name of the tag where to add a new template.
    *
    * @return The embedded template newly added
    *
    * @throws Template.UnknowTag if the given tag name did not exist.
    *
    * @see #add( String, Template )
    * @see #getTag( String )
    * */
   public final Template add( final String name ) throws UnknowTag {
      return getTag( name ).create();
   }


   /**
    * Insert an existing template in a given tag of this one.
    * <p>
    * It is a shortcut for the following code :
    * <pre>
    *   getTag( name ).add( tpl );
    * </pre>
    *
    * @param name Name of the tag where to add the exiting template.
    * @param tpl  The existing template to add. It's type can be different than the default one.
    *
    * @return The embedded tag with the given name or {@code null } if no corresponding tag is found.
    *
    * @throws Template.UnknowTag if the given tag name did not exist.
    *
    * @see #add( String )
    * @see #getTag( String )
    * */
   public final Template add( final String name, final Template tpl ) throws UnknowTag {
      return getTag( name ).add( tpl );
   }

//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final boolean value ) {
//      vars.put( name, value ? Boolean.TRUE : Boolean.FALSE );
//      return this;
//   }
//
//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final byte value ) {
//      vars.put(name, value);
//      return this;
//   }
//
//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final short value ) {
//      vars.put( name, value );
//      return this;
//   }
//
//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final int value ) {
//      vars.put( name, value );
//      return this;
//   }
//
//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final long value ) {
//      vars.put( name, value );
//      return this;
//   }
//
//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final float value ) {
//      vars.put( name, value );
//      return this;
//   }
//
//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final double value ) {
//      vars.put( name, value );
//      return this;
//   }
//
//
//   /**
//    * Set the value for a variable in this template.<p>
//    * No error is thrown if the variable did not exist in the template,
//    * so you can use different templates with a program that push always the same data.
//    *
//    *
//    * @param name  Name of the variable to set.
//    * @param value Value to be assigned to the variable.
//    *
//    * @return this template.
//    *
//    * */
//   public final Template set( final String name, final char value ) {
//      vars.put( name, value );
//      return this;
//   }
//

   /**
    * Set the value for a variable in this template.<p>
    * No error is thrown if the variable did not exist in the template,
    * so you can use different templates with a program that push always the same data.
    *
    *
    * @param name  Name of the variable to set.
    * @param value Value to be assigned to the variable.
    *
    * @return this template.
    *
    * */
   public final Template set( final String name, final Object value ) {
      if ( value != null ) {
         vars.put( name, value );
      }
      return this;
   }


   /**
    * Get current string value for a given variable
    *
    * @param name Name of the variable.
    *
    * @return Current value for this variable as a string or <code>null</code> if it is undefined.
    * */
   public final String get( final String name ) {
      final Object value = vars.get( name );
      return value == null ? EMPTY_STRING : value.toString();
   }


   /**
    * Get current value for a given variable
    *
    * @param name Name of the variable.
    *
    * @return Current value for this variable or <code>null</code> if it is undefined.
    * */
   public final Object getObject( final String name ) {
      return vars.get( name );
   }


   /**
    * Gets a factory defined locally in this template.
    *
    * @param name factory name composed of tags names separated per dots.
    *
    * @return Corresponding factory or <code>null</code> if no factory was found with given name.
    * */
   public final Factory getFactory( final String name ) {
      //return factory.getEngine( name );
      return factory.getFactory( name );
   }


   /**
    * gets the factory used to build this template.
    *
    * @return factory where this template come from.
    * */
   public final Factory getFactory() {
      return factory;
   }


   /**
    * Gets the set of valid tags names for this template.
    * This is a shorthand method for : <code>aTemplate.getFactory().getTagNames();</code>.
    *
    * @return a set of strings where each entry is a valid tag name for this template.
    * */
   public final Set<String> getTagsNames() {
      return factory.getTagsNames();
   }


   /**
    * Check if a given tag name is defined in this template.
    * This method is a shorthand for : <code>aTemplate.getFactory().ttagExists( tagName );</code>.
    *
    * @param tagName tag name to be checked for existence.
    *
    * @return <code>true</code> if given tag name is defined for this template, else <code>false</code>.
    * */
   public final boolean tagExists( final String tagName ) {
      return factory.tagExists( tagName );
   }


   /**
    * write current content to a given destination.
    *
    * @param dest Destination where to write current content.
    *
    * @throws IOException if an error occur when writing to final destination.
    * */
   @Override
   final void write( final TypeWriter dest ) throws IOException {
      dest.push( this );
       for ( Writable element : elements ) {
           element.write( dest );
       }
      dest.pop();
   }


   /**
    * Write this template content to any standard writer.
    *
    * @param writer Writer where to write content.
    * @throws java.io.IOException When the content cannot be written.
    **/
   public final void write( final Writer writer ) throws IOException {
      this.write( new TypeWriter( writer ) );
   }

   /**
    * Write this template content to an output stream.
    *
    * @param out Output stream where to write content.
    * 
    * @throws java.io.IOException When the content cannot be written.
    **/
   public final void write( final OutputStream out ) throws IOException {
    try (
        OutputStreamWriter osw = new OutputStreamWriter( out );
        BufferedWriter w = new BufferedWriter( osw )
    ) {
        this.write( w );
    }
   }

   /**
    * Write this template content to an output stream using a given character set
    *
    * @param out Output stream where to write content.
    * @param charset Character set to use.
    * 
    * @throws java.io.IOException When the content cannot be written.
    **/
   public final void write( final OutputStream out, final Charset charset ) throws IOException {
        try (
            OutputStreamWriter os = new OutputStreamWriter( out, charset );
            BufferedWriter w = new BufferedWriter( os )
        ) {
            this.write( w );
        }
   }

   /**
    * Write this template content to an output stream.
    *
    * @param out         Output stream where to write content.
    * @param charsetName Name of character set to use for encoding.
    * 
    * @throws java.io.IOException When the content cannot be written.
    * */
   public final void write( final OutputStream out, final String charsetName ) throws IOException {
        try (
            OutputStreamWriter osw = new OutputStreamWriter( out, charsetName );
            BufferedWriter w = new BufferedWriter( osw )
        ) {
            this.write( w );
        }
   }

   /**
    * Save this template content to a file.
    *
    * @param fileName Name of the file where to write content.
    * 
    * @throws java.io.IOException When the content cannot be written.
    **/
   public final void saveTo( final String fileName ) throws IOException {
      saveTo( new File( fileName ) );
   }

   /**
    * Save this template content to a file.
    *
    * @param file File where to write content.
    * 
    * @throws java.io.IOException When the content cannot be written.
    **/
   public final void saveTo( final File file ) throws IOException {
        file.getParentFile().mkdirs();
        try (
            FileOutputStream out = new FileOutputStream( file )
        ) {
           write( out );
        }
   }



   /**
    * Save this template content to a file.
    *
    * @param file File where to write content.
    * @param charset Character set to use in the file.
    * 
    * @throws java.io.IOException When the content cannot be written.
    **/
   public final void saveTo( final File file, final Charset charset ) throws IOException {
      file.getParentFile().mkdirs();
      try ( FileOutputStream out = new FileOutputStream( file ) ) {
         write( out, charset );
      }
   }




   /**
    * Save this template content to a file.
    *
    * @param file File where to write content.
    * @param charset Name of the character set to use in the file.
    * 
    * @throws java.io.IOException When the content cannot be written.
    **/   
   public final void saveTo( final File file, final String charset ) throws IOException {
      file.getParentFile().mkdirs();
      try ( FileOutputStream out = new FileOutputStream( file ) ) {
         write( out, charset );
      }
   }


   /**
    * Convert this template content to it's <code>String</code> representation.
    *
    * @return string representation for this template.
    * */
   @Override
   public final String toString() {
      try ( final StringWriter w = new StringWriter( 1024 ) ) {
         this.write( w );
         return w.toString();
      }
      catch ( final IOException e ) { // should not append with a StringWriter (no IO in fact)
         throw new RuntimeException( "Unexpected IOException from StringWriter", e );
      }
   }

}
