package io.jstach.spec.mustache.spec.inheritance;

import io.jstach.jstache.JStache;
import io.jstach.jstache.JStachePartial;
import io.jstach.jstache.JStachePartials;
import io.jstach.spec.generator.SpecModel;

@JStache(path = "inheritance/Datadoesnotoverrideblock.mustache")
@JStachePartials({ @JStachePartial(name = "include", template = "{{$var}}var in include{{/var}}"), })
public class Datadoesnotoverrideblock extends SpecModel {

}
