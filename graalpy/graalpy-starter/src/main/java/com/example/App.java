/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://opensource.org/license/UPL.
 */

package com.example;

import org.graalvm.polyglot.Context;

public class App {

    public static void main(String[] args) {
        try (Context context = Context.newBuilder("python")
                         /* Enabling some of these is needed for various standard library modules */
                        .allowAllAccess(true)
                        .build()) {
            context.eval("python", """
import java
from java.util import ArrayList
l = ArrayList()
l.add(1)
print("!!!! " + str(l.get(0)))
Foo = java.type("com.example.Foo")
foo = Foo()
foo.print()
            """);
        }
    }
}
