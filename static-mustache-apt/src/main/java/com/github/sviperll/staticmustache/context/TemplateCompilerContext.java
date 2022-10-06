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

import org.eclipse.jdt.annotation.Nullable;

import com.github.sviperll.staticmustache.context.ContextException.FieldNotFoundContextException;

/**
 * @see RenderingCodeGenerator#createTemplateCompilerContext
 * @author Victor Nazarov <asviraspossible@gmail.com>
 */
public class TemplateCompilerContext {
    private final TemplateStack templateStack;
    private final @Nullable EnclosedRelation enclosedRelation;
    private final RenderingContext context;
    private final RenderingCodeGenerator generator;
    private final VariableContext variables;
    private final ContextType childType;

    TemplateCompilerContext(TemplateStack templateStack, RenderingCodeGenerator processor, VariableContext variables, RenderingContext field,
            ContextType childType) {
        this(templateStack, processor, variables, field, childType, null);
    }

    private TemplateCompilerContext(
            TemplateStack templateStack, 
            RenderingCodeGenerator processor, 
            VariableContext variables, 
            RenderingContext field, 
            ContextType childType,
             @Nullable EnclosedRelation parent) {
        this.templateStack = templateStack;
        this.enclosedRelation = parent;
        this.context = field;
        this.generator = processor;
        this.variables = variables;
        this.childType = childType;
    }

    private String sectionBodyRenderingCode(VariableContext variables) throws ContextException {
        JavaExpression entry = context.currentExpression();
        String path =  enclosedRelation != null ? enclosedRelation.name() : "";
        try {
            return generator.generateRenderingCode(entry, variables, path);
        } catch (TypeException ex) {
            throw new ContextException("Unable to render field", ex);
        }
    }
    
//    private String path() {
//        List<String> segments = new ArrayList<>();
//        var e = enclosedRelation;
//        while (e != null) {
//            String n = e.name();
//            if (! n.isBlank()) {
//                segments.add(e.name());
//            }
//            var pe = parentContext().enclosedRelation;
//            if (pe == e || pe == this.enclosedRelation)
//                break;
//            e = pe;
//        }
//        Collections.reverse(segments);
//        return segments.stream().collect(Collectors.joining("."));
//    }

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
        return "/* RenderingContext: " + context.getClass() + " */\n" + //
                "/* TypeMirror: " + context.currentExpression().type() + " */\n";

    }

    public String endSectionRenderingCode() {
        return context.endSectionRenderingCode();
    }

    public TemplateCompilerContext createForParameterPartial(String template) {
        // No enclosing relation for new partials
        return new TemplateCompilerContext(templateStack.ofPartial(template), generator, variables, context, ContextType.PARENT_PARTIAL);
    }
    
    public TemplateCompilerContext createForPartial(String template) {
        return new TemplateCompilerContext(templateStack.ofPartial(template),generator, variables, context, ContextType.PARTIAL);
    }

    
    public TemplateCompilerContext getChild(String path, ContextType childType) throws ContextException {
        return _getChild(path, childType);
    }

    List<String> splitNames(String name) {
        return List.of(name.split("\\."));
    }

    
    //TODO rename to ContextType
    public enum ContextType {
        ROOT,
        ESCAPED_VAR,
        UNESCAPED_VAR,
        SECTION, // #
        INVERTED { // ^
          @Override
        public ContextType pathType() {
              return INVERTED;
        }  
        },
        PARTIAL, // >
        BLOCK, // $
        PARENT_PARTIAL, // <
        PATH;
        
        public ContextType pathType() {
            return PATH;
        }
        
        public boolean isVar() {
            if (this == ESCAPED_VAR || this == UNESCAPED_VAR) {
                return true;
            }
            return false;
        }
    }
    
    private TemplateCompilerContext _getChild(String name, ContextType childType) throws ContextException {
        if (name.equals(".")) {
            RenderingContext enclosedField = _getChildRender(name, childType, new OwnedRenderingContext(context));
            return new TemplateCompilerContext(templateStack, generator, variables, enclosedField, childType, 
                    new EnclosedRelation(name, this));
        }
        
        switch (childType) {
        case PARTIAL, PARENT_PARTIAL, BLOCK -> {
            RenderingContext enclosedField = _getChildRender(name, childType, new OwnedRenderingContext(context));
            return new TemplateCompilerContext(templateStack, generator, variables, enclosedField, childType,
                    new EnclosedRelation(name, this));
        }
        default -> {
        }
        }
        
        List<String> names = splitNames(name);
        
        if (names.size() == 0) {
            throw new IllegalStateException("names");
        }
        
        RenderingContext enclosing = new OwnedRenderingContext(context);
        //System.out.println(enclosing.printStack());
        var it = names.iterator();
        /*
         * A non dotted name can be resolved against parents
         */
        var start = _getChildRender(it.next(), it.hasNext() ? childType.pathType() : childType, enclosing, false);
        enclosing = start;
        /*
         * Each part of a dotted name should resolve only against its parent.
         * direct = true
         */
        while (it.hasNext()) {
            String n = it.next();
            /*
             * Inverted dotted fields can actually not exist.
             * TODO add compiler flag on whether or not to support this
             */
            if (childType == ContextType.INVERTED) {
                try {
                    enclosing = _getChildRender(n, childType, enclosing, true);
                } catch (FieldNotFoundContextException e) {
                    enclosing = start;
                    break;
                }
            }
            else {
                enclosing = _getChildRender(n, it.hasNext() ? childType.pathType() : childType, enclosing, true);
            }
        }
        return new TemplateCompilerContext(templateStack, 
                generator, variables, enclosing, childType, new EnclosedRelation(name, this));

    }
    
    private RenderingContext _getChildRender(String name, ContextType childType, RenderingContext enclosing) throws ContextException {
        return _getChildRender(name, childType, enclosing, false);
    }
    
    private RenderingContext _getChildRender(String name, ContextType childType, RenderingContext enclosing, boolean direct) 
            throws ContextException {
        try {
            return __getChildRender(name, childType, enclosing, direct);
        }
        catch (TypeException ex) {
            throw new ContextException(MessageFormat.format("Can''t use ''{0}'' field for rendering", name), ex);
        }
    }
        
    private RenderingContext __getChildRender(String name, ContextType childType, RenderingContext enclosing, boolean direct) 
            throws ContextException, TypeException {
        if (name.equals(".")) {
            return switch (childType) {
            case ESCAPED_VAR, UNESCAPED_VAR, PATH -> enclosing;
            case SECTION ->  generator.createRenderingContext(childType, enclosing.currentExpression(), enclosing);
            case INVERTED -> throw new ContextException("Current section can't be inverted");
            case PARENT_PARTIAL, ROOT -> throw new ContextException("Current section can't be parent");
            case PARTIAL -> throw new ContextException("Current section can't be partial");
            case BLOCK -> throw new ContextException("Current section can't be block");
            };
        }
        /*
         * dots are allowed in partials since they are file names
         */
        if (childType == ContextType.PARENT_PARTIAL || childType == ContextType.PARTIAL) {
            return enclosing;
        }
        if (name.contains(".")) {
            throw new IllegalStateException("dotted path not allowed here");
        }
        if (childType == ContextType.BLOCK) {
            return enclosing;
        }
        JavaExpression entry = direct ? enclosing.getDataDirectly(name) :  enclosing.getDataOrDefault(name, null);
        if (entry == null) {
            System.out.println("Field not found." + " field: " + name +  ", template: " +  templateStack.describeTemplateStack() + " context stack: " + enclosing.printStack() + " direct: " + direct + "\n");
            throw new FieldNotFoundContextException(MessageFormat.format("Field not found in current context: ''{0}'' , template: " + templateStack.describeTemplateStack(), name));
        }
        RenderingContext enclosedField;
        enclosedField = switch (childType) {
        case ESCAPED_VAR, UNESCAPED_VAR, SECTION, PATH -> generator.createRenderingContext(childType,entry, enclosing);
        case INVERTED -> new InvertedRenderingContext(generator.createInvertedRenderingContext(entry, enclosing));
        case PARENT_PARTIAL, ROOT -> throw new IllegalStateException("parent not allowed here");
        case PARTIAL -> throw new IllegalStateException("partial not allowed here");
        case BLOCK -> throw new IllegalStateException("block not allowed here");
        };
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
    
    public ContextType getType() {
        return childType;
    }
    
    public TemplateStack getTemplateStack() {
        return templateStack;
    }
}
