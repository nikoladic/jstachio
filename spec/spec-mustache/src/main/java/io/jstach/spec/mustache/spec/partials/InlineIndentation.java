package io.jstach.spec.mustache.spec.partials;

import io.jstach.annotation.JStach;
import io.jstach.annotation.JStachPartial;
import io.jstach.annotation.JStachPartialMapping;
import io.jstach.spec.generator.SpecModel;

@JStach(path = "partials/InlineIndentation.mustache")
@JStachPartialMapping({
@JStachPartial(name="partial", template=">\n>"),
})
public class InlineIndentation extends SpecModel {
}
