/*
 * (c) 2024 Koordinates Limited
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.geotools.util.logging.Logging;

/** */
public class GSRFunctionFactory implements FunctionFactory {
    // https://doc.arcgis.com/en/arcgis-online/reference/sql-agol.htm
    // String Functions
    static final Name STRING_LOWER = new NameImpl("LOWER");

    static final Name STRING_UPPER = new NameImpl("UPPER");

    static final Name STRING_CHARLENGTH = new NameImpl("CHAR_LENGTH");

    static final Name STRING_CONCAT = new NameImpl("CONCAT");

    static final Name STRING_POSITION = new NameImpl("POSITION");

    static final Name STRING_SUBSTIRNG = new NameImpl("SUBSTRING");

    // Date Functions
    static final Name DATE_CURRENT_TIME = new NameImpl("CURRENT_TIME");

    // Numeric Functions
    static final Name NUMERIC_ABS = new NameImpl("ABS");

    static final Name NUMERIC_CEILING = new NameImpl("CEILING");

    static final Name NUMERIC_COS = new NameImpl("COS");

    static final Name NUMERIC_FLOOR = new NameImpl("FLOOR");

    static final Name NUMERIC_LOG = new NameImpl("LOG");

    static final Name NUMERIC_LOG10 = new NameImpl("LOG10");

    static final Name NUMERIC_MOD = new NameImpl("MOD");

    static final Name NUMERIC_POWER = new NameImpl("POWER");

    static final Name NUMERIC_SIN = new NameImpl("SIN");

    static final Name NUMERIC_TAN = new NameImpl("TAN");

    FilterFactory ff;

    static final Logger LOGGER = Logging.getLogger(GSRFunctionFactory.class);

    List<FunctionName> functionNames;

    public GSRFunctionFactory() {
        ff = CommonFactoryFinder.getFilterFactory(null);
        List<FunctionName> names = new ArrayList<>();

        // register string functions
        names.add(ff.functionName(STRING_LOWER, 1));
        names.add(ff.functionName(STRING_UPPER, 1));
        names.add(ff.functionName(STRING_CHARLENGTH, 1));
        names.add(ff.functionName(STRING_CONCAT, 2));
        names.add(ff.functionName(STRING_POSITION, 2));
        names.add(ff.functionName(STRING_SUBSTIRNG, 3));

        // register date functions
        names.add(ff.functionName(DATE_CURRENT_TIME, 0));

        // register numeric functions
        names.add(ff.functionName(NUMERIC_ABS, 1));
        names.add(ff.functionName(NUMERIC_CEILING, 1));
        names.add(ff.functionName(NUMERIC_COS, 1));
        names.add(ff.functionName(NUMERIC_FLOOR, 1));
        names.add(ff.functionName(NUMERIC_LOG, 1));
        names.add(ff.functionName(NUMERIC_LOG10, 1));
        names.add(ff.functionName(NUMERIC_MOD, 2));
        names.add(ff.functionName(NUMERIC_POWER, 2));
        names.add(ff.functionName(NUMERIC_SIN, 1));
        names.add(ff.functionName(NUMERIC_TAN, 1));

        functionNames = Collections.unmodifiableList(names);
    }

    public Function function(String name, List<Expression> args, Literal fallback) {
        return function(new NameImpl(name), args, fallback);
    }

    @Override
    public Function function(Name name, List<Expression> args, Literal fallback) {
        // Return custom functions first
        if (STRING_POSITION.equals(name)) {
            return new GSRStrIndexOf(name, args, fallback);
        } else if (NUMERIC_LOG10.equals(name)) {
            return new GSRLog10(name, args, fallback);
        }

        // return the ECQL equivalent function
        Expression[] argsArray = args.toArray(new Expression[0]);
        try {
            // Get the method by name and parameter type
            Method method =
                    this.getClass()
                            .getDeclaredMethod(name.toString().toUpperCase(), Expression[].class);
            return (Function) method.invoke(this, new Object[] {argsArray});
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<FunctionName> getFunctionNames() {
        return (functionNames != null && !functionNames.isEmpty())
                ? functionNames
                : Collections.emptyList();
    }

    private Function LOWER(Expression[] argsArray) {
        // LOWER(<string>) -> strToLowerCase(<string>)
        return ff.function("strToLowerCase", argsArray);
    }

    private Function UPPER(Expression[] argsArray) {
        // UPPER(<string>) -> strToUpperCase(<string>)
        return ff.function("strToUpperCase", argsArray);
    }

    private Function CHAR_LENGTH(Expression[] argsArray) {
        // CHAR_LENGTH(<string>) -> strLength(<string>)
        return ff.function("strLength", argsArray);
    }

    private Function CONCAT(Expression[] argsArray) {
        // CONCAT(<string1>, <string2>) -> strConcat(<string1>, <string2>)
        return ff.function("strConcat", argsArray);
    }

    private Function SUBSTRING(Expression[] argsArray) {
        // SUBSTRING(<string>, <start>, <length>) -> strSubstring(<string>, <start>, <end>)
        // NOTE: SUBSTRING is 1-based, strSubstring is 0-based index.
        // Need to convert the length to an end index
        Expression startIndex = ff.subtract(argsArray[1], ff.literal(1));
        Expression endIndex = ff.add(startIndex, argsArray[2]);
        Expression[] newArgs = new Expression[] {argsArray[0], startIndex, endIndex};
        return ff.function("strSubstring", newArgs);
    }

    private Function CURRENT_TIME(Expression[] argsArray) {
        // CURRENT_TIME() -> now()
        return ff.function("now", argsArray);
    }

    private Function ABS(Expression[] argsArray) {
        // Check the type of the first argument in argsArray
        if (argsArray.length > 0 && argsArray[0] != null) {
            Object value = argsArray[0].evaluate(null);

            if (value instanceof Integer) {
                return ff.function("abs", argsArray);
            } else if (value instanceof Long) {
                return ff.function("abs_2", argsArray);
            } else if (value instanceof Float) {
                return ff.function("abs_3", argsArray);
            } else if (value instanceof Double) {
                return ff.function("abs_4", argsArray);
            }
        }

        return ff.function("abs", argsArray);
    }

    private Function CEILING(Expression[] argsArray) {
        // CEILING(<number>) -> ceil(<x>: double)
        return ff.function("ceil", argsArray);
    }

    private Function COS(Expression[] argsArray) {
        // COS(<number>) -> cos(<angle>: double)
        return ff.function("cos", argsArray);
    }

    private Function FLOOR(Expression[] argsArray) {
        // FLOOR(<number>) -> floor(<x>: double)
        return ff.function("floor", argsArray);
    }

    private Function LOG(Expression[] argsArray) {
        // LOG(<number>) -> log(<x>: double)
        return ff.function("log", argsArray);
    }

    private Function MOD(Expression[] argsArray) {
        // MOD(<number>, <n>) -> modulo(<x>: int, <y>: int) | IEEEremainder(<x>: double, <y>:
        // double)
        if (argsArray.length >= 2 && argsArray[0] != null && argsArray[1] != null) {
            Object value1 = argsArray[0].evaluate(null);
            Object value2 = argsArray[1].evaluate(null);
            if (value1 instanceof Integer && value2 instanceof Integer) {
                return ff.function("modulo", argsArray);
            } else if (value1 instanceof Double || value2 instanceof Double) {
                // If either value is a double, use IEEEremainder
                return ff.function("IEEEremainder", argsArray);
            }
        }

        return ff.function("modulo", argsArray);
    }

    private Function POWER(Expression[] argsArray) {
        // POWER(<number>, <y>) -> pow(<base>: double, <exponent>: double)
        return ff.function("pow", argsArray);
    }

    private Function SIN(Expression[] argsArray) {
        // SIN(<number>) -> sin(<angle>: double)
        return ff.function("sin", argsArray);
    }

    private Function TAN(Expression[] argsArray) {
        // TAN(<number>) -> tan(<angle>: double)
        return ff.function("tan", argsArray);
    }
}
