package com.github.sviperll.staticmustache.examples;

import io.jstach.annotation.GenerateRenderableAdapter;

@GenerateRenderableAdapter(template="parent2.mustache")
public record Parent2(String message) {
    
}
