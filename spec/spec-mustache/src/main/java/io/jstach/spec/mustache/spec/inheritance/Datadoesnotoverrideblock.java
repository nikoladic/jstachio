package io.jstach.spec.mustache.spec.inheritance;

import io.jstach.annotation.GenerateRenderableAdapter;
import io.jstach.annotation.Template;
import io.jstach.annotation.TemplateMapping;
import io.jstach.spec.generator.SpecModel;

@GenerateRenderableAdapter(template = "inheritance/Datadoesnotoverrideblock.mustache")
@TemplateMapping({
@Template(name="include", template="{{$var}}var in include{{/var}}"),
})
public class Datadoesnotoverrideblock extends SpecModel {
}
