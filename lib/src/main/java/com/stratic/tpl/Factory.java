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

import java.util.List;
import java.util.Set;

/**
 * An engine able to create new templates instances.
 * Each factory produce a specific kind of template.
 * <p>
 * By analogy, a factory is to a template what a class is to an object.
 * <p>
 * You can get the factories from:
 * <ul>
 * <li>{@link Template Templates}: The factory used to create the template.</li>
 * <li>{@link Tag Tags}: The factory used to create default content at the location materialized by the tag.</li>
 * <li>{@link FactoryLoader factories loaders}: The root factories.</li>
 * </ul>
 * 
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
public interface Factory {
    
    /**
     * Create a new template from this factory.
     *
     * @return A new template created by this factory.
     **/
    public Template create();
    
    
    /**
     * Gets tags names used in this factory.
     *
     * @return a set of strings where each entry is a valid tag name for produced templates;
     **/
    public Set<String> getTagsNames();
    
    
    /**
     * Gets an ordered list of tags names used in this factory.
     *
     * @return a list of strings where each entry is a valid tag name for produced templates;
     **/
    public List<String> getTagsList();


    /**
     * Check if a given tag name is defined by this factory
     *
     * @param tagName tag name to be checked for existency.
     *
     * @return <code>true</code> if given tagname is defined for this factory templates, else <code>false</code>.
     **/
    public boolean tagExists( final String tagName );


    /**
     * Get factory for an embedded tag.
     *
     * @param tagName Embedded tag name.
     *
     * @return corresponding factory.
     **/
    public Factory getFactory( final String tagName );
    
}
