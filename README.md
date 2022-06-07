# Stratic-tpl &middot; [![License: Apache 2](https://img.shields.io/badge/License-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![Latest Github release](https://img.shields.io/github/release/laurent-simon/Stratic-tpl.svg)](https://github.com/laurent-simon/Stratic-tpl/releases/latest)

**Stratic-tpl** is a lightweight and fast Java library that allows a program to generate textual content using templates.

Created in 2003 to generate proprietary software code for more than 250 applications in banking, health insurance and petrol industry, today, it is still in use, as an open-source library. 

## Features

* No dependencies.
* XML like template markup.
* Handles complex indentations levels.
* The program keeps control on logic and data.
* Java 8+ compatible.

# Contents

* [Usage and examples](#usage-and-examples)
* [Templates syntax](#syntax-reference)
* [API documentation](#api-documentation)
* [Building](#building)

# Usage and examples

Given a "hello world" template file named `hello.txt`:

```txt
    Hello world
```

It can be used by a Java program like following:

```java
import com.stratic.tpl.Factory;
import com.stratic.tpl.FactoryLoader;
import com.stratic.tpl.Template;

// ...

final Factory helloFactory = FactoryLoader
    .getDefault()                // Uses the default template factory loader
    .getFactory( "hello.txt" )   // Gets a factory able to produce templates from `hello.txt` file

final Template hello = helloFactory.create();   // Creates an instance of 'hello' template

System.out.printf( hello.toString() );
```

It looks overcomplicated for a "hello world" program! If the template need to be used often, then, `helloFactory` should be kept permanently in memory (It's the "costly" part). Then, it's more simple.

To say hello to somebody that will be known at runtime, in `hello.txt`:

```txt
    Hello ${name}
```

Usage:

```java
final Template hello = helloFactory.create()
    .set( "name", "John" );

System.out.printf( hello.toString() );
```

Result:

```
    Hello John
```

Variables types can be complex objects.

Given a class `Person`:

```java
    final public class Person {
        public final String firstName;
        public final String LastName;

        Person( final String first, final String last ) {
            this.firstName = first;
            this.lastName = last;
        }

        public final String getFullName() {
            return String.format( "%s %s", firstName, lastName );
        }
    }
```

Then you can write the following template:

```txt
    Hello  ${person.firstName} ${person.lastName}
```

or this one :

```txt
    Hello ${person.fullName}
```

With the following code:

```java
    final Template hello = helloFactory.create()
        .set( "person", new Person( "John", "Doe" ) ) 

    System.out.printf( hello.toString() );    
```

Both will produce the same result:

```txt
    Hello John Doe
```

Methods, getters and attributes can be used indifferently, with the following precedence order :

1. Method with exact name (`fuillName` for example`).
2. Getter (`getFullName` or 'isMale` for example).
3. Attribute with exact name.

They don't need necessary to be public (private members can be used).

A template can contain sub-templates, delimited by a named tag:

```txt

<tag:greeting>Hello ${person.fullName}</tag:greeting>
```

The sub-template can be used multiple times in the main one:

```java
    List<Person> persons = ...

    final Template hello = helloFactory.create();
    for ( Person p : persons ) {
        hello.create( "greeting" ).set( "person", p );
    }
```

Result :

```txt

Hello John DoeHello Susy Lee
```

Oops, something is missing! When multiple contents are inserted at the same location, like here, if it's necessary, the tag can define a separator to be used between content instances. So, we can change the `hello.txt` template to:

```txt
<tag:newEntry>,
</tag:newEntry>
<tag:greeting separator="newEntry" >Hello ${person.fullName}</tag:greeting>
```

Using this version with the same program, the result will be:

```txt

Hello John Doe,
Hello Susy Lee
```

A tag encompasses two distinct things:

* A location where some content can be inserted.
* A default content (the sub-template) to be used at this location.

Both concepts can be used separately:

```txt
<tag:greeting>   Hello ${person.fullName}
</tag:greeting>

Greetings:
<tag:grettings/>
```

Here :

* Ther first tag ( `greeting` ) defines the  content of a greeting.
* The second tag ( `greetings` ) defines the place where all the greetings should appear.

Usage example:

```java
    final Template hello = helloFactory.create();
    final Factory greeting = hello.getFactory( "greeting" );  // Gets content's factory of the greeting content template.
    final Tag greetings = hello.getTag( "greetings" );        // Gets the location where to insert greetings


    for ( Person p : persons ) {
        greetings.add( greeting.create().set( "person", p ) );
    }
```

The content can also come from another external source (another file in this example):

`hello.txt` :

```txt
Greetings:
<tag:grettings/>
```

`greeting.txt`:

```txt
   Hello ${person.fullName}
```

```java
    // Done one time some where
    final Factory loader = FactoryLoader.getDefault();
    final Factory helloFactory = loader.getFactory( "hello.txt" );
    final Factory greeting = loader.getFactory( 'greeting.txt' );

    // usage
    final Template hello = helloFactory.create();
    final Tag greetings = hello.getTag( "greetings" );

    for ( Person p : persons ) {
        greetings.add( greeting.create().set( "person", p ) );
    }
```

Content can also be included statically, from the same template. Example (file `page.txt`):

```txt
<tag:line>---------------------------------------</tag:line><tag:header>
         **${page.document.title}**
@{line}</tag:header>
<tag:content/>
<tag:footer>@{line}
</tag:footer>@{line}
            Page ${page.number}/${page.document.pages}
</tag:footer>
```

Here, `@{tag:line>` says to include a copy of the content of the `line` tag (`<tag:line>`). It is equivalent to:

```txt
<tag:line>---------------------------------------</tag:line><tag:header>
            **${page.document.title}**
---------------------------------------</tag:header>
<tag:content/>
<tag:footer>---------------------------------------
            Page ${page.number}/${page.document.pages}
</tag:footer>
```

In the same way, content can also be imported from an external template (All or a specific part of it). Example (file `abstract.txt`):

```txt
@{/page.txt#header}

# Abstract

   ${page.content}

@{/page.txt#footer}
```

So, any template can be reused and, with its content, it can act like a library.

The includes are transparent for the program. The template is used like if everything was written directly  in it:

```java
    // Done one time some where
    final Factory abstractFactory = FactoryLoader.getDefault().getFactory( "abstract.txt" );

    // ...
    Page page = ... // application specific stuff
    page.content= "This is an example\n" + 
                  "of page content."

    // usage
    final Template abstract = abstractFactory.create().set( "page", page );
```

But, in the result, there is a small glitch!

```md
**Example document**
---------------------------------------

# Abstract

   This is an example
of page content

---------------------------------------
            Page 2/3
```

By default, if the automatically included content contains many lines, the next lines start at the beginning, without margin.

To preserve margins, an `<indent>` tag can be added around the section where margins should be preserved:

```md
@{/page.txt#header}

# Abstract

   <indent>${page.content}</indent>

@{/page.txt#footer}
```

The reference margin is the one before the `<indent>` tag. This gives this result:

```md
**Example document**
---------------------------------------

# Abstract

   This is an example
   of page content.

---------------------------------------
            Page 2/3
```

The margin is the sequence of characters preceding the `<indent>` tag from the start of the line. It's not necessarily spaces. It can be anything else.

For example, this :

```md
@{/page.txt#header}

# Abstract

.  | <indent>${page.content}</indent>

@{/page.txt#footer}
```

Gives :

```md
**Example document**
---------------------------------------

# Abstract

.  | This an example
.  | of page content

---------------------------------------
            Page 2/3
```

Remark: These examples demonstrates indentation principles using a simple vraiable but, indentations works for any type of dynamic content, without any limitation.

# Syntax reference

The templates source code is just pure text, where some special character sequences denotes specific features to the templates. The text can be redacted using any character set (UTF-8, ISO-8859-1, etc.).
If the character set is different from the one on the runtime environment, the character set needs to be explicitly specified at load time (see [`FactoryLoader.getDefault()`](https://laurent-simon.github.io/Stratic-tpl/com/stratic/tpl/FactoryLoader.html#getDefault(java.lang.ClassLoader,java.nio.charset.Charset)).

In this syntax specification, some artifacts are not part of the syntax itself but are used to explain the syntax:

* A string delimited by cotes (like in `'example'`) is a placeholder for a syntax element that is already described or that will be described next.
* Square brackets denotes optional parts (like in `Hello[ world]`, where ` world` is noted as optional).

## Variables

A variable usage has the following syntax:

```txt
    ${'variable reference'}
```

`'variable reference'` designates a value known by the program at runtime. It has the following syntax:

```txt
    'name'[.'variable reference']
```

`'name'` is a valid Java variable name. It is case-sensitive.

Example :

```txt
    ${customer.address.city.name}
```

## Escape character

The escape character is the backslash (`\`). When it is used, the next character lose its special meaning.

For example: `${foo}` designates the `foo` variable. But the `\${bar}` or `$\{bar}` expressions doesn't designate any variable. With the escape character in front of them, some characters have lost their magic power, then the sequences are not any more special. Both cases are taken as the `${bar}' static text.

The backslash is itself a special character. To use it as normal text in a template, put another backslash in front of it. For example, to write:

```txt
    C:\\Windows
```

In a template, write instead :

```txt
    C:\\\\Windows
```

Remark: you need to protect special characters, only if they form an ambiguous expression. For example, in the following text:

```txt
    They cost 10 $ each!
```

The `$` sign did not need to be protected by a backslash because it's not ambiguous. The dollar sign has a special meaning only if it is followed by an opening brace (`{`). That's not the case here.

The only special character that should be always escaped to be normally rendered is the backslash itself.

## Tag

A tag has the following syntax when it is just a location mark (like a bookmark):

```txt
    <tag:'tag name'[ separator="'separator'"]/>
```

Or this syntax, when a default content is defined for this location:

```txt
    <tag:'tag name'[ separator="'separator'"]>'default content'</tag:'tag name'>
```

`'tag name'` is the tag identifier. Allowed characters are :

* a letter
* a currency symbol (such as '$', '€', etc.)
* a connecting punctuation character (such as '_')
* a digit
* a numeric letter (such as a Roman numeral character)
* a combining mark
* a non-spacing mark

In practice, all characters allowed in a Java identifier are allowed here.

`'default content'` is the content that will be produced at this location, when no explicit content is specified. It's a nested template.

`'separator'` is a template reference (see after) which designates the content to be used to separate consecutive content occurrences inserted at this location.

## Template reference

A template reference can point to a full template (a root template), or a part of a template (a tag inside the template). The syntax is:

```txt
    [[/'root reference']#]['tag reference']
```

The reference can be:

* Absolute : 'with a `'root reference'`
* Relative to a template : with a pound sign (`#` points to the template head).
  - If there is a `'root reference'` before, the `#` sign point to the head of the corresponding template.
  - If there is nothing before, then the `#` sign points to the head of the current template.
* Relative to a location : with a `'tag reference'`
  - if there is a `#` sign before the `'tag reference`', the starting location  is the head of the  template pointed by the `#` sign.
  - If there is nothing before, the starting location is the current tag.

A `'root reference'` has no specific syntax. It is dependent on the `FactoryLoader` implementation. The default implementation accepts two types of templates source:

* In-memory named templates : In this case, the root reference is the name of the template (example `foo`).
* File templates : In this case, the root reference is a file path name (example `java/class-template.txt`).

A `'tag reference'` has the following syntax:

```txt
    'tag name'[.'tag reference']
```

The dot notation is used when necessary to point to nested tags.

## Includes

The includes have two alternative ways.

To include a copy of the content of an existing template in the current one, the syntax is the following:

```txt
    @{'template reference'}
```

To create a new tag which is identical to an existing one, the syntax is the following:

```txt
    @{tag:'tag name' 'template reference'}
```

Where:

* `'tag name'` : is the name of the new tag to be created at this location.
* `'template reference'` : designates an existing content to be used as the content of the new tag.

In fact, the second form is a shortcut for :

```txt
    <tag:'tag name'>@{'template reference'}</'tag name'>
```

Be careful that a template definition cannot be infinite. So, includes cannot be reentrants and include themselves (directly or indirectly).

## Indentations

Indentations syntax is :

```txt
    <indent>'content'</indent>
```

`'content'` can be any template's content. It is a content where new lines must be indented at the same position, and with the same margin characters, than the `<indent>` tag itself.

Remark: putting an `<indent>`at the start of a line is useless. As expected, it has no effect in this case.

# API documentation

The [Stratic-tpl API documentation](https://laurent-simon.github.io/Stratic-tpl/) is avaible online.

# Building

The project uses [Gradle](https://gradle.org/). To build `Stratic-tpl.jar` from source, go to the project main folder and, on Windows type:

```bat
gradlew build
```

on linux or MacOS, type:

```bash
./gradlew build
```

The `Stratic-tpl.jar` library is generated in the `lib/build/libs` folder.
