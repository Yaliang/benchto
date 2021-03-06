/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.benchto.driver.utils;

import javax.measure.unit.Unit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static javax.measure.unit.NonSI.BYTE;
import static javax.measure.unit.NonSI.DAY;
import static javax.measure.unit.NonSI.HOUR;
import static javax.measure.unit.NonSI.MINUTE;
import static javax.measure.unit.SI.GIGA;
import static javax.measure.unit.SI.KILO;
import static javax.measure.unit.SI.MEGA;
import static javax.measure.unit.SI.MICRO;
import static javax.measure.unit.SI.MILLI;
import static javax.measure.unit.SI.NANO;
import static javax.measure.unit.SI.SECOND;
import static javax.measure.unit.SI.TERA;

/**
 * Helps converting between units.
 */
// TODO: use JSR 363 when available
public final class UnitConverter
{
    private static final Pattern VALUE_WITH_UNIT_PATTERN = Pattern.compile("^([+-]?(?:\\d+|\\d*\\.\\d+))([a-zA-Z]*)$");
    private static final int VALUE_GROUP_INDEX = 1;
    private static final int UNIT_GROUP_INDEX = 2;

    private UnitConverter()
    {
    }

    public static double parseValueAsUnit(String string, Unit<?> unit)
    {
        double parsedValue = parseValue(string);
        Unit<?> parsedUnit = parseUnit(string);
        return parsedUnit.getConverterTo(unit).convert(parsedValue);
    }

    public static double parseValue(String string)
    {
        Matcher matcher = matchValueWithUnit(string);
        return Double.parseDouble(matcher.group(VALUE_GROUP_INDEX));
    }

    public static Unit<?> parseUnit(String string)
    {
        Matcher matcher = matchValueWithUnit(string);
        switch (matcher.group(UNIT_GROUP_INDEX)) {
            case "d":
                return DAY;
            case "h":
                return HOUR;
            case "m":
                return MINUTE;
            case "s":
                return SECOND;
            case "ms":
                return MILLI(SECOND);
            case "ns":
                return NANO(SECOND);
            case "us":
                return MICRO(SECOND);
            case "B":
                return BYTE;
            case "kB":
            case "KB":
                return KILO(BYTE);
            case "MB":
                return MEGA(BYTE);
            case "GB":
                return GIGA(BYTE);
            case "TB":
                return TERA(BYTE);
            default:
                throw new IllegalArgumentException(string + " unit is not supported");
        }
    }

    public static String format(Unit<?> unit)
    {
        if (unit.equals(BYTE)) {
            return "BYTES";
        }
        else if (unit.equals(MILLI(SECOND))) {
            return "MILLISECONDS";
        }
        else {
            throw new IllegalArgumentException(unit + " is not supported");
        }
    }

    private static Matcher matchValueWithUnit(String string)
    {
        Matcher matcher = VALUE_WITH_UNIT_PATTERN.matcher(string);
        checkState(matcher.matches(), "String %s does not match value with unit pattern", string);
        return matcher;
    }
}
