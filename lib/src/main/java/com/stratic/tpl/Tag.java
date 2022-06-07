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
import java.util.ArrayList;

/**
 * A tag is a named template part. It defines two things:
 * <ul>
 * <li>A location where nested content can be added.</li>
 * <li>A default template for its content.</li>
 * </ul>
 * 
 * A tag can be used to play both roles or only one of them.
 * <p>
 * For example, if we have the following template for a mail:
 * <pre>{@code
 * Subject: ${subject}
 * To:      <tag:recipient>${name};</tag:recipient>;
 * Body:    ${body}
 * }</pre>
 * The notation "{@code <tag:recipient>${name}</tag:recipient>}" defines :
 * <ul>
 * <li> A tag named 'recipient' starting at {@code <tag:recipient>} and ending at {@code </tag:recipient>}.</li>
 * <li> The position where the content will be inserted is defined by the first character of '{@code <tag:}' ( the '{@code <}').
 * <li> The default template for its content is defined between the starting mark
 *      ( {@code <tag:recipient>} ) and the ending one ( {@code </tag:recipient>} ).</li>
 * </ul>
 * The default content can be omitted. Example :
 * <pre>{@code
 * Subject: ${subject}
 * To:      <tag:recipient/>
 * Body:    ${body}
 * }</pre>
 * In this case, the nature of the content to add must be defined explicitly using the {@link #add(Template) add method}. 
 *
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public final class Tag extends Writable {

    /** valued templates for this place. **/
    final private ArrayList<Writable> content = new ArrayList<>( 10 );
    
    /** factory used to build templates within this tag **/
    final private Engine factory;
  

    /**
     * Creates a new Tag.
     *
     * @param factory factory used to build templates at this tag.
     * 
     * @param interp interpreter to use for expression evaluation.
     */
    Tag( final Engine factory ) {
	this.factory = factory;
    }


    /**
     * Create a new template inside this tag using it's default template.
     * <p>
     * Example :
     * <p>
     * if we use the previous mail template example defined as follow :
     * <pre>{@code
     * Subject:${subject}
     * To:<tag:to>${name};</tag:to>
     * Body:${body}
     * }</pre>
     * To set the recipients, using the default template provided by "to" the tag, the code will look like following one :
     * <pre>{@code
     *
     *	    Template mail;
     *	    ...
     *	    Tag recipients = mail.getTag("to");		        // get a handle on the "to" tag
     *	    recipients.create().set("name", "John Doe");	// Create a new recipient
     *	    recipients.create().set("name", "Peter Smith");	// Create another one
     *
     * }</pre>
     * 
     * With this function, the default content is created in place. It's is a shortcut for:
     * <pre>{@code
     *
     *	    this.add( this.getFactory(). create() );
     *
     * }</pre>
     * 
     * @return the newly created template.
     **/
    public final Template create() {
	return add( factory.create() );
    }
    
    /**
     * Add an existing template content at this tag place.
     *
     * @param tpl An existing template to add at this place. If there is already some content,
     *            then it is added at the end.
     *
     * @return The newly added template.
     **/
    public final Template add( final Template tpl ) {
	content.add( tpl );
	return tpl;
    }
   
    /**
     * Gets the factory used to build default content templates for this tag.
     *
     * @return this tag default factory.
     **/
    public final Factory getFactory() {
	return factory;
    }
    
    
    /**
     * Gets the factory of a nested tag.
     *
     * @param name Nested tag reference. It can be simply a tag name. When the target tag is nested deeply,
     *             then it's multiple tags names separated by dots (like in {@code "customer.address.country"}).
     *
     * @return Corresponding factory or <code>null</code> if no factory was found with this reference.
     **/
    public final Factory getFactory( final String name ) {
	return factory.getEngine( name );
    }
   

    /**
     * Check if this tag is empty.
     * <p>
     * It is a shortcut for : "<code>size() == 0</code>".
     * 
     * @return <code>true</code> if there is nothing at this place, else <code>false</code>.
     */
    public final boolean isEmpty() {
	return content.isEmpty();
    }

    /** Check if there is existing content at this place.
     * <p>
     * It is a shortcut for: "<code>size() != 0</code> ".
     * 
     * @return <code>true</code> if there is content, else <code>false</code>.
     */
    public final boolean isNotEmpty()  {
	return !content.isEmpty();
    }
    
    /**
     * Write current content to a given destination.
     *
     * @param dest Destination where to write current content.
     *
     * @throws IOException if an error occur when writing to final destination.
     **/
    @Override
    final void write( final TypeWriter dest ) throws IOException {
	int cnt  = content.size();
	for (int i = 0; i < cnt; i++) {
	    if ( i > 0 && factory.separator != null) {
		factory.separator.write( dest );
	    }
	    content.get(i).write( dest );
	}
    }
}
