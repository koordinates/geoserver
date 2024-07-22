package org.geoserver.gsr.function;

import java.util.List;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Converters;
import org.geotools.util.factory.Hints;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;

/** LOG10(<number>): The base-10 logarithm of the specified number. */
public class GSRLog10 extends FunctionImpl {

    public GSRLog10(Name name, List<Expression> args, Literal fallback) {
        functionName = new FunctionNameImpl(name, args.size());
        setName(name.getLocalPart());
        setFallbackValue(fallback);
        setParameters(args);
    }

    @Override
    public Object evaluate(Object object) {

        Object arg0;

        arg0 = getParameters().get(0).evaluate(object);

        if (arg0 == null) {
            return null;
        }

        arg0 = Converters.convert(arg0, Double.class, new Hints());
        if (arg0 == null) {
            throw new IllegalArgumentException(
                    "Filter Function problem for function log argument #0 - expected type double");
        }

        return Math.log10((Double) arg0);
    }
}
