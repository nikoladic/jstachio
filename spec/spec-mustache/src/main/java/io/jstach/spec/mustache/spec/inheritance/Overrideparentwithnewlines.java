package io.jstach.spec.mustache.spec.inheritance;

import io.jstach.annotation.GenerateRenderer;
import io.jstach.annotation.Template;
import io.jstach.annotation.TemplateMapping;
import io.jstach.spec.generator.SpecModel;

@GenerateRenderer(template = "inheritance/Overrideparentwithnewlines.mustache")
@TemplateMapping({
@Template(name="parent", template="{{$ballmer}}peaking{{/ballmer}}"),
})
public class Overrideparentwithnewlines extends SpecModel {
}
