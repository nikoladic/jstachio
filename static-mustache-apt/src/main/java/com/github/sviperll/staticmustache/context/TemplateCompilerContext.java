/*
 * Copyright (c) 2014, Victor Nazarov <asviraspossible@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *
 *  3. Neither the name of the copyright holder nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.sviperll.staticmustache.context;

import java.text.MessageFormat;
import java.util.List;

import org.jspecify.nullness.Nullable;

/**
 * @see RenderingCodeGenerator#createTemplateCompilerContext
 * @author Victor Nazarov <asviraspossible@gmail.com>
 */
public class TemplateCompilerContext {
    private final @Nullable EnclosedRelation enclosedRelation;
    private final RenderingContext context;
    private final RenderingCodeGenerator generator;
    private final VariableContext variables;

    TemplateCompilerContext(RenderingCodeGenerator processor, VariableContext variables, RenderingContext field) {
        this(processor, variables, field, null);
    }

    private TemplateCompilerContext(RenderingCodeGenerator processor, VariableContext variables, RenderingContext field, 
             @Nullable EnclosedRelation parent) {
        this.enclosedRelation = parent;
        this.context = field;
        this.generator = processor;
        this.variables = variables;
    }

    private String sectionBodyRenderingCode(VariableContext variables) throws ContextException {
        JavaExpression entry = context.currentExpression();
        try {
            return generator.generateRenderingCode(entry, variables);
        } catch (TypeException ex) {
            throw new ContextException("Unable to render field", ex);
        }
    }

    public String renderingCode() throws ContextException {
        return beginSectionRenderingCode() + sectionBodyRenderingCode(variables) + endSectionRenderingCode();
    }

    public String unescapedRenderingCode() throws ContextException {
        return beginSectionRenderingCode() + sectionBodyRenderingCode(variables.unescaped()) + endSectionRenderingCode();
    }

    public String beginSectionRenderingCode() {
        return  debugComment() +  context.beginSectionRenderingCode();
    }
    
    private String debugComment() {
        return "/* RenderingContext: " + context.getClass() + " */\n";
    }

    public String endSectionRenderingCode() {
        return context.endSectionRenderingCode();
    }

    
    public TemplateCompilerContext getChild(String name, ChildType childType) throws ContextException {
        return _getChild(name, childType);
    }

    List<String> splitNames(String name) {
        return List.of(name.split("\\."));
    }

    
    public enum ChildType {
        NORMAL,
        INVERTED;
    }
    
    private TemplateCompilerContext _getChild(String name, ChildType childType) throws ContextException {
        if (name.equals(".")) {
            RenderingContext enclosedField = _getChildRender(name, childType, new OwnedRenderingContext(context));
            return new TemplateCompilerContext(generator, variables, enclosedField, new EnclosedRelation(name, this));
        }
        
        List<String> names = splitNames(name);
        
        if (names.size() == 0) {
            throw new IllegalStateException("names");
        }
        
        RenderingContext enclosing = new OwnedRenderingContext(context);
        
        for (String n : names) {
            enclosing = _getChildRender(n, childType, enclosing);
        }
        return new TemplateCompilerContext(generator, variables, enclosing, new EnclosedRelation(name, this));

    }
    
    private RenderingContext _getChildRender(String name, ChildType childType, RenderingContext enclosing) throws ContextException {
        if (name.equals(".")) {
            return switch (childType) {
            case NORMAL ->  enclosing;
            case INVERTED -> throw new ContextException("Current section can't be inverted");
            };
        }
        if (name.contains(".")) {
            throw new IllegalStateException("dotted path not allowed here");
        }
        JavaExpression entry = enclosing.getDataOrDefault(name, null);
        if (entry == null)
            throw new ContextException(MessageFormat.format("Field not found in current context: ''{0}''", name));
        RenderingContext enclosedField;
        try {
            enclosedField = switch (childType) {
            case NORMAL -> generator.createRenderingContext(entry, enclosing);
            case INVERTED -> new InvertedRenderingContext(generator.createInvertedRenderingContext(entry, enclosing));
            };
        } catch (TypeException ex) {
            throw new ContextException(MessageFormat.format("Can''t use ''{0}'' field for rendering", name), ex);
        }
        return enclosedField;
    }

    public boolean isEnclosed() {
        return enclosedRelation != null;
    }

    public String currentEnclosedContextName() {
        return enclosedRelation.name();
    }

    public TemplateCompilerContext parentContext() {
        return enclosedRelation.parentContext();
    }

    public String unescapedWriterExpression() {
        return variables.unescapedWriter();
    }
}
