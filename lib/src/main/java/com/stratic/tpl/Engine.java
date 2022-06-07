/*
 * Copyright 2003-2007 Laurent SIMON
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

import com.stratic.tpl.accessors.AccessorException;
import com.stratic.tpl.accessors.ExpressionGetter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.stratic.tpl.FactoryLoader.TemplateNotFound;
import com.stratic.tpl.Template.Error;
import com.stratic.tpl.Template.UnknowTag;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Templates instances producer.
 *
 * @todo add more comments
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
final class Engine implements ContentProducer, Factory {

   /** end of file text representation **/
   private final static String EOF = "<End Of File>";

   /** embedded template start tag prefix **/
   private final static String TAG_START = "tag:";

   /** embedded template end tag prefix **/
   private final static String TAG_END = "/" + TAG_START;

   /** indentation start mark **/
   private final static String INDENT_START = "indent>";

   /** indentation end mark **/
   private final static String INDENT_END = "/indent>";

   /** separator keyword */
   private final static String SEPARATOR = "separator";

   /** Embedded template start tag prefix as a char sequence **/
   private final static char[] TAG_START_CHARS = TAG_START.toCharArray();

   /** Embedded template end tag prefix as a char sequence **/
   private final static char[] TAG_END_CHARS = TAG_END.toCharArray();

   /** indentation start mark as a char sequence **/
   private final static char[] INDENT_START_CHARS = INDENT_START.toCharArray();

   /** indentation end mark as a char sequence **/
   private final static char[] INDENT_END_CHARS = INDENT_END.toCharArray();

   /** separator keyword as a char sequence **/
   private final static char[] SEPARATOR_CHARS = SEPARATOR.toCharArray();

   private final static Writable PUSH_INDENT = new Writable() {
       
      @Override
      final void write( final TypeWriter dest ) throws IOException {
         dest.pushIndent();
      }
      
   };

   private final static Writable POP_INDENT = new Writable() {
       
      @Override
      final void write( final TypeWriter dest ) throws IOException {
         dest.popIndent();
      }
      
   };

   private final static ContentProducer PUSH_INDENT_BUILDER = () -> PUSH_INDENT;

   private final static ContentProducer POP_INDENT_BUILDER = () -> POP_INDENT;


   private final class TagIndexComparator implements Comparator<String> {

      @Override
      public int compare( final String tag1, final String tag2 ) {
         final int i1 = tagIndex( tag1 );
         final int i2 = tagIndex( tag2 );

         return i1 > i2 ? 1 :
                i1 < i2 ? -1 :
                0;
      }

   }


   /**
    * Exception thrown when a template contain two tags with same name at same level.
    *
    * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
    **/
   final static class DuplicatedTag extends RuntimeException {

      private static final long serialVersionUID = 469073104314720722L;

      /** Create a new DuplicateTag exception **/
      DuplicatedTag( final String name ) {
         super( "tag '" + name + "' is already defined" );
      }
   }


   /**
    * Text element definition.
    * Instances of this class are used both as builders and/or as a result elements.
    *
    * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
    **/
   private static final class Text extends Writable implements ContentProducer {

      /** text to insert in templates **/
      private final String txt;


      /**
       * Create a new static text definition
       *
       * @param txt Text to insert in templates.
       **/
      Text( final String txt ) {
         this.txt = txt;
      }


      /**
       * Create a new element from this text.
       **/
      @Override
      final public Writable content() {
         return this;
      }


      @Override
      final void write( final TypeWriter dest ) throws IOException {
         dest.write( txt );
      }

   }


   /**
    * Expression element builder.
    *
    * The expression will be interpreted at runtime.
    *
    * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
    **/
   private static final class VariableBuilder extends Writable implements ContentProducer {

      /** variable name **/
      private final String name;
      /** Default value if it is null **/
      private final String dflt;
      /** Expression to be read from the variable **/
      private final ExpressionGetter getter;


      /** Create a new Expression **/
      VariableBuilder( final String name, final String dflt ) {

         final int dot = name.indexOf( '.' );
         this.name = dot < 0 ? name : name.substring( 0, dot );
         this.getter = dot >= 0 ? ExpressionGetter.forExpression( name.substring( dot + 1 ) ) : null;
         this.dflt = dflt;
      }


      /**
       * Write current value for this variable on a given type writer.
       * If current value is not defined then default value is used.
       *
       * @param dest TypeWriter where to write the value on.
       *
       **/
      @Override
      protected final void write( final TypeWriter dest ) throws IOException {
         try {
            final Object obj =
               getter == null ? dest.getObject( name ) :
               getter.get( dest.getObject( name ) );

            final String toWrite = obj != null ? obj.toString() : dflt;
            if ( toWrite != null ) {
               dest.write( toWrite );
            }
         }
         catch ( final AccessorException accessor ) {
            throw new UnableToEvaluateExpression( name + '.' + getter.getExpression(), accessor );
         }
      }


      /**
       * Create a new instance from this definition.
       **/
      @Override
      public final Writable content() {
         return this;
      }
   }


   /**
    * Unable to evaluate expression exception.
    *
    * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
    **/
   private final static class UnableToEvaluateExpression extends RuntimeException {

        private static final long serialVersionUID = 6250395660786179326L;

      /** Create a new DuplicateTag exception **/
      UnableToEvaluateExpression( final String expr, final Throwable origin ) {
         super( "Unable to evaluate expression '" + expr + "': " + origin.getMessage(), origin );
      }

   }


   /**
    * Tag reference.
    *
    * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
    **/
   private final static class IncludeTag implements ContentProducer {

      /** Original factory to use **/
      final Engine factory;


      /** Create a new Expression **/
      IncludeTag( final Engine factory ) {
         this.factory = factory;
      }


      /**
       * Create a new instance from this definition.
       *
       * @param parent parent template whose request the new instance.
       **/
      @Override
      public Writable content() {
         return new Tag( factory );
      }
   }


   /**
    * Template syntax parser.
    *
    * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
    **/
   private final class Parser {

      /**
       * Signal an error during parse (low level utility).
       *
       * @param unexpected Unexpected character that was encountered.
       * @param expected   Expected character the parser is waiting for.
       *
       * @throw ParseException to signal the error.
       **/
      private void _err( final char unexpected, final char expected ) throws Error {
         _err( "Character '" + unexpected + "'", "character '" + expected + "'" );
      }


      /**
       * Signal an error during parse (low level utility).
       *
       * @param unexpected Unexpected sequence that was encountered.
       * @param expected   Expected sequence the parser is waiting for.
       *
       * @throw ParseException to signal the error.
       **/
      private void _err( final String unexpected, final String expected ) throws Error {
         _err( unexpected + " encountered when expecting " + expected );
      }


      private void _err( final Exception e ) throws Error {
         _err( e.getMessage() );
      }


      private void _err( final String msg ) throws Error {
         final int pos = Math.min( this.idx, last ); // take care that idx can be out off bounds.

         // compute current position coordinates (line,column)
         int line = 0;
         int col = pos;
         for ( int i = pos; i >= 0; i-- ) {	// backward scan of the text
            if ( txt[i] == '\n' ) {		// if carriage return encountered
               if ( line == 0 ) {		// if it is the first one
                  col = pos - i - "\n".length();		// compute relative column position
               }
               ++line;
            }
         }

         throw new Error( name, txt, pos, line, col, msg );
      }

      /** text to parse. **/
      private final char[] txt;

      /** Last character position in the text. **/
      private final int last;

      /** Buffer used to collect static text portions. **/
      private StringBuilder buf;

      /** Current character position when parsing text. **/
      private int idx = 0;

      /** Execution environment **/
      private final FactoryLoader ldr;


      /**
       * Creates a new instance of Parser
       *
       * @param ldr Current loader.
       * @param txt Text to parse.
       *
       **/
      private Parser( final FactoryLoader ldr, final char[] txt ) {
         this.txt = txt;
         this.last = this.txt.length - 1;
         this.buf = new StringBuilder( last + 1 );
         this.ldr = ldr;
      }


      /**
       * Check if parse is not at end of text.
       *
       * @return <code>true</code> if it is not, else <code<false</code>.
       **/
      private boolean notEOF() {
         return idx <= last;
      }


      /**
       * Check if parse is at the end of text.
       *
       * @return <code>true</code> if it is, else <code<false</code>.
       **/
      private boolean eof() {
         return idx > last;
      }


      /**
       * Collect static text if present and add a corresponding TextTemplate.
       * <p>
       * After this, the buffer content is consumed.
       * The buffer is ready to collect further static text.
       *
       * @param container Container where to place collected text.
       *
       **/
      private void grabText( final Engine container ) {
         if ( buf != null && buf.length() > 0 ) {			// if there is some gathered text
            container.addText( buf.toString() );		// push text in the container
            if ( eof() ) {
               buf = null;
            }
            else {
               buf.setLength( 0 );
            }
         }
      }


      /**
       * Handle a tag delimiter mark ('<') encountered during text parse.
       *
       * @param container Container where to put corresponding templates.
       *
       * @return <code>true</code> if it close current tag, else <code>false</code>.
       **/
      private boolean tagDelimiter( final Engine container ) throws Error {
         if ( match( TAG_START_CHARS ) ) {  // '<tag:' => template start
            grabText( container );
            parseTag( container );
            return false;
         }
         if ( match( TAG_END_CHARS ) ) {  // '</tag:' template end

            // ensure tag is closed properly
            final String tag = identifier();
            if ( !container.name.equals( tag ) ) {
               _err( tag.length() > 0 ? "<" + TAG_END + tag : ( eof() ? EOF : "Character '" + txt[idx] + "'" ), "<" + TAG_END +
                                                                                                                container.name );
            }
            eatBlanks();
            expect( '>' );

            grabText( container );	// handle gattered text
            return true;			// return control to parent level
         }
         if ( match( INDENT_START_CHARS ) ) { // '<indent>' => indentation begin
            grabText( container );
            container.pushIndent();
            return false;
         }
         if ( match( INDENT_END_CHARS ) ) { // '<indent/>' => indentation end
            grabText( container );
            container.popIndent();
            return false;
         }
         // default treat it as normal char
         buf.append( '<' );
         return false;
      }


      /**
       * Handle a value inclusion mark ('$') was encountered during text parse.
       *
       * @param container Container where to put corresponding templates.
       **/
      private void variableMark( final Engine container ) throws Error {
         if ( eof() || !match( '{' ) ) {   // if '$' is the last character or is not followed by '{'
            buf.append( '$' );	    // '$' as no special meaning
            match( '\\' );		    // skip next character if it is the escape one
            return;
         }
         // "${" mark => start of expression
         grabText( container );	    // flush current text
         parseVariable( container );	    // parse value to include
      }


      /**
       * Handle a template inclusion mark ('@') was encountered during text parse.
       *
       * @param container Container where to put corresponding templates.
       **/
      private void includeMark( final Engine container ) throws Error {
         if ( eof() || !match( '{' ) ) {   // if '@' is the last character or is not followed by '{'
            buf.append( '@' );	    // '@' as no special meaning
            match( '\\' );		    // skip next character if it is the escape one
            return;
         }
         // "@{" mark => start of expression
         grabText( container );
         parseInclude( container );
      }


      /**
       * Parse a text section (any non specific tag mark is static text).
       *
       * @param container Container where to put corresponding templates.
       **/
      private void parseText( final Engine container ) throws Error {
         char c;
         while ( notEOF() ) {		    // while not at the end of string
            switch ( c = txt[idx++] ) {	    // read current character and then move to next one
            case '$':		    // expression start ?
               variableMark( container );
               break;
            case '@':		    // include start ?
               includeMark( container );
               break;
            case '<':		    // tag start or end ?
               if ( tagDelimiter( container ) ) { // if current templare end
                  return;			  // return to enclosiing template
               }
               break;
            default:
               buf.append( c );    // normal character
            }
         }

         // end of file encountered
         grabText( container );
         if ( !( container.isRoot() ) ) { // if container is not the root one, it is still open !
            _err( EOF, "<" + TAG_END + container.name + ">" );
         }
      }


      private String parseValue() throws Error {
         final StringBuilder b = new StringBuilder( 256 );
         char c;
         while ( notEOF() ) {		    // while not at the end of string
            switch ( c = txt[idx++] ) {	    // read current character and then move to next one
            case '}':		    // end of value
               return b.toString().trim();
            case '\\':		    // escape character
               if ( notEOF() ) {	    // if not at end of file
                  c = txt[idx++];	    // read next character (without special meaning)
               }
               b.append( c );
               break;
            default:		    // normal char
               b.append( c );	    // take it in expression
            }
         }
         _err( EOF, "\"" );	    // closing cote not found
         return null; // dead code => just for the compiler
      }


      /**
       * Parse a variable expression.
       * <pre>
       * variable expresion syntax is : <variableName>[=<value>]
       * </pre>
       *
       * @param container Container where to put resulting template.
       **/
      private void parseVariable( final Engine container ) throws Error {
         eatBlanks();
         final String varName = parseExpression();
         if ( varName.length() == 0 ) {
            _err( "variable name expected" );
         }
         eatBlanks();

         // handle default value
         String dflt = "";
         if ( match( '=' ) ) {
            dflt = parseValue();
         }
         else {
            expect( '}' );
         }
         container.addVariable( varName, dflt );
      }


      /**
       * Parse include tag.
       *
       * @param containerr where to add corresponding template.
       **/
      private void parseInclude( final Engine container ) throws Error {

         try {
            if ( match( TAG_START_CHARS ) ) { // include by reference
               final String tag = identifier();
               if ( tag.length() == 0 ) {
                  _err( "tag identifier expected" );
               }
               final Engine factory = parseReference( container, '}' );
               container.includeTag( tag, factory );
            }
            else { // include by value
               container.include( parseReference( container, '}' ) );
            }
         }
         catch ( DuplicatedTag e ) {
            _err( e.getMessage() );
         }

         expect( '}' );
      }
      
      
//        private String parseUntil( final char stop ) {
//          final StringBuilder b = new StringBuilder( 64 );
//            while ( notEOF() && txt[idx] != stop ) {
//               b.append( txt[idx] );
//               idx++;
//            }
//          return b.toString();        
//        }
        
        private String parseUntil( final char... stops ) {
          final StringBuilder b = new StringBuilder( 64 );
            while ( notEOF() ) {
               for ( char stop : stops ) {
                   if ( txt[idx] == stop ) {
                       return b.toString();
                   }
               }
               b.append( txt[idx] );
               idx++;
            }
          return b.toString();        
        }


      /**
       * Parse a template factory reference.
       *
       * @return Engine pointed by the reference.
       **/
      private Engine parseReference( Engine factory, final char stop ) throws Error {
         eatBlanks();

         // read file spec if present
         if ( match( '/' ) ) {
            final String factoryName = parseUntil( '#', stop ).trim();
            if ( factoryName.length() > 0 ) {
               try {
                  factory = (Engine) ldr.getFactory( factoryName );
               }
               catch ( final TemplateNotFound e ) {
                  _err( e );
               }
            }
         }

         // read root indicator if present
         if ( match( '#' ) ) {
            factory = factory.root();
         }

         // read tag name if present
         String tag;
         while ( ( tag = identifier() ).length() > 0 ) { // while there is identifiers
            factory = factory.getEngine( tag );
            if ( factory == null ) {
               _err( "invalid tag reference '" + tag + "'" );
            }
            if ( !match( '.' ) ) {	    // if next character is not a dot
               break;                  // end of path
            }
         }

         eatBlanks();

         return factory;
      }


      /**
       * Check if text at current position match a given chars sequence.
       * If the text match the sequence, then corresponding characters will be consumed, else the parser stay at current text position.
       *
       * @param chars Sequence to match.
       *
       * @return <code>true</code> if text at current position match the sequence, else <code>false</code>.
       **/
      private boolean match( final char[] chars ) {
         int pos = idx;
         for ( int i = 0; i < chars.length; i++ ) {
            if ( pos > last || chars[i] != txt[pos++] ) {
               return false;
            }
         }
         idx = pos;
         return true;
      }


      /**
       * Check if text at current position match a given character.
       * If the text match the character, then 1 character is consumed, else the parser stay at current text position.
       *
       * @param c character to match.
       *
       * @return <code>true</code> if text at current position match the character, else <code>false</code>.
       **/
      private boolean match( final char c ) {
         if ( idx > last || txt[idx] != c ) { // if character is not the expect one
            return false;
         }
         idx++;			   // it is the expected one => consume it
         return true;
      }


      /**
       * Read an identifier at current parser position.
       *
       * @return The identifier. It is an empty string if no identifier characters are found at current position.
       **/
      private String identifier() {
         if ( idx > last ) {
            return "";
         }
         final StringBuilder b = new StringBuilder( 15 );
         while ( idx <= last && ( Character.isJavaIdentifierPart( txt[idx] ) ||
                                  txt[idx] == '\\' ) ) {
            if ( txt[idx] == '\\' && ++idx > last ) {
               break;
            }
            b.append( txt[idx++] );
         }
         return b.toString();
      }


      private String parseExpression() {

         if ( idx > last ) {
            return "";
         }
         final StringBuilder buf = new StringBuilder( 15 );
         final int start = idx;		    // mark first character
         while ( idx <= last && ( Character.isJavaIdentifierPart( txt[idx] ) ||
                                  txt[idx] == '.' ||
                                  txt[idx] == '\\' ) ) {
            if ( txt[idx] == '\\' && ++idx > last ) {
               break;
            }
            buf.append( txt[idx++] );
         }
         //return new String(txt, start, idx - start);
         return buf.toString();
      }


      /**
       * Read an expected symbol at current position.
       *
       * @param expected expected symbol.
       **/
      private void expect( final char expected ) throws Error {
         if ( idx > last ) {
            _err( "<End of file>", "" + expected );
         }
         if ( txt[idx++] != expected ) {
            _err( txt[idx - 1], expected );
         }
      }


      private void eatBlanks() {
         while ( idx <= last && Character.isWhitespace( txt[idx] ) ) {
            idx++;
         }
      }


      private Engine parseSeparator( final Engine container ) throws Error {
         eatBlanks();
         if ( !match( SEPARATOR_CHARS ) ) {  // separator = ?
            return null;
         }
         eatBlanks();
         expect( '=' );
         eatBlanks();
         expect( '"' );
         final Engine separator = parseReference( container, '"' );
         expect( '"' );
         eatBlanks();
         return separator;
      }


      /**
       * Parse a template definition.
       *
       * @param container parent definition.
       **/
      private void parseTag( final Engine container ) throws Error {
         // read tag header
         final String tag = identifier();
         if ( tag.length() == 0 ) {
            _err( eof() ? EOF : "Character " + txt[idx], "<A tag name>" );
         }
         final Engine separator = parseSeparator( container );
         final boolean embeded = !match( '/' ); // no empty tag shortcut => embeded template expected.
         expect( '>' );

         // create the tag
         try {
            final Engine factory = container.addTag( tag, separator ); // add the tag to current container
            if ( embeded ) {	  // if there is an embeded template definition
               parseText( factory ); // parse it and push it's definition in the tag factory
            }
         }
         catch ( DuplicatedTag e ) {
            _err( e.getMessage() );
         }

      }

   }

   /** template name **/
   private final String name;

   /** Parent factory which include this one. **/
   private final Engine parent;

   /** nested builders sequence. **/
   private final ArrayList<ContentProducer> sequence = new ArrayList<>();

   /** tags directory (nested **/
   private final HashMap<String,Integer> tags = new HashMap<>();

   /** Separator to use in tags **/
   final Template separator;


   /**
    * Creates a new instance of Engine.
    *
    * @param parent    Parent definition in where this one is included.
    * @param name      Name of this new definition.
    * @param separator template to be inserted between templates produced by this engine
    */
   Engine( final Engine parent, final String name, final Template separator ) {
      this.parent = parent;
      this.name = name;
      this.separator = separator;
   }

   
   final void parse( final FactoryLoader ldr, final char[] syntax ) throws Error {
      ( new Parser( ldr, syntax ) ).parseText( this );
   }


   /**
    * Add a bookmark for a tag at the end of this sequence
    * <p>
    * <B>Warning:</B> corresponding element must be added only after this call.
    *
    * @param name tag name.
    **/
   private void bookmarkTag( final String name ) throws DuplicatedTag {
      if ( tags.containsKey( name ) ) {
         throw new DuplicatedTag( name );
      }
      tags.put( name, sequence.size() );
   }


   /**
    * Add a nested template insertion point at current place.
    *
    * @param name Name of the tag used to mark the insertion point.
    *
    * @return an empty factory that can be used to describe a nested template.
    **/
   private Engine addTag( final String name, final Engine separator ) throws DuplicatedTag {
      bookmarkTag( name );
      final Engine f = new Engine( this, name, separator != null ? separator.create() : null );
      sequence.add( f );
      return f;
   }


   /**
    * Add a variable value occurrence.
    *
    * @param name    Name of the variable.
    * @param default Default value if variable is not set.
    **/
   private void addVariable( final String name, final String dflt ) {
      sequence.add( new VariableBuilder( name, dflt ) );
   }


   /**
    * Add an indentation level.
    **/
   private void pushIndent() {
      sequence.add( PUSH_INDENT_BUILDER );
   }


   /**
    * remove an indentation level.
    **/
   private void popIndent() {
      sequence.add( POP_INDENT_BUILDER );
   }


   /**
    * Add a nested template insertion point at current place using an existing factory.
    *
    * @param name    Name of the nested tag.
    * @param factory factory to be used to produce templates with this tag.
    *
    **/
   private void includeTag( final String name, final Engine factory ) throws DuplicatedTag {
      bookmarkTag( name );
      sequence.add( factory );
   }


   /**
    * Add a static text in templates at current place.
    *
    * @param text Text to insert at runtime.
    **/
   private void addText( final String text ) {
      sequence.add( new Text( text ) );
   }


   /**
    * Include an already existing definition in this one, at current position.
    *
    * @param other Existing definition to be included.
    *
    * @throws DuplicateTag if names of included definition conflicts with existing ones.
    **/
   private void include( final Engine other ) throws DuplicatedTag {

      final int delta = sequence.size(); // memo delta to add to copied tags

      this.sequence.addAll( other.sequence );	// add a copy of other factory sequence at the end od this one

      String key;

      // compute tags bookmarks for new entries
      final Iterator<String> others = other.tags.keySet().iterator();
      while ( others.hasNext() ) {
         key = others.next();
         // ok => create bookmark (index = original index + delta)
         int otherIdx = other.tags.get( key );

         if ( tags.put(key, otherIdx + delta) != null ) { // if bookmark already exist
            throw new DuplicatedTag( key );
         }
      }

   }


   /**
    * Get the internal index of a specified tag.
    *
    * @param name Name of the tag to look for.
    *
    * @return Position of the given tag in this factory.
    *
    * @throw UnknowTag if the given tag name did not exist.
    **/
   final int tagIndex( final String name ) throws UnknowTag {
      final Integer idx = tags.get( name );
      if ( idx == null ) {
         throw new UnknowTag( name );
      }
      return idx;
   }


   @Override
   public final Factory getFactory( final String tagName ) {
      final int dot = tagName.indexOf( '.' );
      if ( dot < 0 ) {
         return getEngine( tagName );
      }

      // it's a relative path
      return getEngine( tagName.substring( 0, dot ) )
         .getFactory( tagName.substring( dot + 1 ) );
   }


   /**
    * Get the engine for a given tag name.
    *
    * @param name Tag name where corresponding engine is requested
    *
    * @return Corresponding engine or <code>null</code> if the given tag name is undefined. 
    **/
   final Engine getEngine( final String name ) {
      try {
         return (Engine) sequence.get( tagIndex( name ) );
      }
      catch ( final UnknowTag e ) {
         return null;
      }
   }


   /**
    * Get the root factory.
    *
    * @return the main factory which include this one.
    **/
   private Engine root() {
      Engine tpl = this;
      while ( tpl.parent != null ) {
         tpl = tpl.parent;
      }
      return tpl;
   }


   /**
    * Check if this factory is the root one.
    *
    * @return <code>true</code> if it is, else <code>false</code>.
    **/
   private boolean isRoot() {
      return parent == null;
   }


   /**
    * Gets tags names included in this factory.
    *
    * @return a set of strings where each entry is a valid tag name for produced templates.
    **/
   @Override
   public final Set<String> getTagsNames() {
      return tags.keySet();
   }


   /**
    * Gets tags names inluded in this factory.
    *
    * @return a set of strings where each entry is a valid tag name for produced templates.
    **/
   @Override
   public final List<String> getTagsList() {
      final ArrayList<String> lst = new ArrayList<>( tags.keySet() );
      Collections.sort( lst, new TagIndexComparator() );
      return lst;
   }


   /**
    * Check if a given tag name is defined by this factory.
    *
    * @param tagName tag name to be checked for existence.
    *
    * @return <code>true</code> if given tag name is defined for this factory templates, else {@code false }.
    **/
   @Override
   public final boolean tagExists( final String tagName ) {
      final int dot = tagName.indexOf( '.' );
      if ( dot < 0 ) {
         return tags.containsKey( tagName );
      }

      // it's a relative path
      final Engine sub = getEngine( tagName.substring( 0, dot ) );
      return sub != null ? sub.tagExists( tagName.substring( dot + 1 ) ) :
             null;

   }


   /**
    * Create a new template from this factory.
    *
    * @return A new template created by this factory.
    **/
   @Override
   public final Template create() {
      final int cnt = sequence.size();
      final Writable[] elts = new Writable[cnt];
      final Template t = new Template( this, elts );
      for ( int i = 0; i < cnt; i++ ) {
         elts[i] = sequence.get( i ).content();
      }
      return t;
   }


   /**
    * Create a new <code>Element</code> with this factory.
    *
    * @param interp Interpreter to use to evaluate expressions.
    *
    * @return A new <code>Element</code>.
    **/
   @Override
   public final Writable content() {
      return new Tag( this );
   }

}
