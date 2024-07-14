package org.geoserver.gsr.function;

import java.util.List;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.filter.function.StaticGeometry;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

/**
 * POSITION(<substring>, <string>) -> strIndexOf(<string>, <substring>) GSR's strIndexOf returns
 * 0-based index, but POSITION is 1-based index This custom class is needed to reverse the order of
 * the arguments, and add the extra +1 to the result to match the 1-based index behavior of POSITION
 * }
 */
public class GSRStrIndexOf extends FunctionImpl {

    public GSRStrIndexOf(Name name, List<Expression> args, Literal fallback) {
        functionName = new FunctionNameImpl(name, args.size());
        setName(name.getLocalPart());
        setFallbackValue(fallback);
        setParameters(args);
    }

    @Override
    public Object evaluate(Object object) {

        String arg0;
        String arg1;

        try {
            arg0 = getParameters().get(0).evaluate(object, String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Filter Function problem for function strIndexOf argument #0 - expected type String");
        }

        try {
            arg1 = getParameters().get(1).evaluate(object, String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Filter Function problem for function strIndexOf argument #1 - expected type String");
        }

        return Integer.valueOf(StaticGeometry.strIndexOf(arg1, arg0) + 1);
    }
}
