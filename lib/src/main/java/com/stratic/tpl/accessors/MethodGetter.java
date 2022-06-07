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
 * A {@link Getter} to read values using a method.
 * 
 * @author <a href="https://github.com/laurent-simon">Laurent Simon</a>
 */
final class MethodGetter implements Getter {
   
   private final static Object[] ARGS = new Object[0];
   
   /** Method used to read values. **/
   private final Method mth;
   
   /** Creates a new instance of MethodSetter. */
   MethodGetter( final Method mth ) {
      this.mth = mth;
   }

   @Override
   public Object get(final Object target) {
      try {
          return mth.invoke( target, ARGS );
      }
      catch ( final Exception e ) {
          throw new AccessorException(
            e,
            mth.getName(),
            "Unable to read property value with method '%s'",  mth
           );
      }
   }

   @Override
   public Class<?> getType() {
      return mth.getReturnType();
   }
   
}
