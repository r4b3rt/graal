/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.runtime.options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.oracle.truffle.llvm.runtime.LLVMLogger;

public class LLVMOptions {

    public static void main(String[] args) {
        for (PropertyCategory category : PropertyCategory.values()) {
            List<Property> properties = category.getProperties();
            if (!properties.isEmpty()) {
                LLVMLogger.unconditionalInfo(category + ":");
                for (Property prop : properties) {
                    LLVMLogger.unconditionalInfo(prop.toString());
                }
                LLVMLogger.unconditionalInfo("");
            }

        }
    }

    static final String PATH_DELIMITER = ":";
    static final String OPTION_PREFIX = "sulong.";
    private static final String OBSOLETE_OPTION_PREFIX = "llvm.";

    @FunctionalInterface
    interface OptionParser {
        Object parse(Property property);
    }

    static boolean parseBoolean(Property prop) {
        return Boolean.parseBoolean(System.getProperty(prop.getKey(), prop.getDefaultValue()));
    }

    static String parseString(Property prop) {
        return System.getProperty(prop.getKey(), prop.getDefaultValue());
    }

    static int parseInteger(Property prop) {
        return Integer.parseInt(System.getProperty(prop.getKey(), prop.getDefaultValue()));
    }

    static String[] parseDynamicLibraryPath(Property prop) {
        String property = System.getProperty(prop.getKey(), prop.getDefaultValue());
        if (property == null) {
            return new String[0];
        } else {
            return property.split(PATH_DELIMITER);
        }
    }

    public enum PropertyCategory {
        GENERAL,
        DEBUG,
        PERFORMANCE,
        TESTS,
        MX;

        public List<Property> getProperties() {
            List<Property> properties = new ArrayList<>();
            for (Property p : Property.values()) {
                if (this == p.getCategory()) {
                    properties.add(p);
                }
            }
            return properties;
        }
    }

    private static Map<Property, Object> parsedProperties = new HashMap<>();
    private static final List<LLVMOption> registeredProperties = new ArrayList<>();

    static {
        registeredProperties.addAll(Arrays.asList(Property.values()));
        parseOptions();
        checkForInvalidOptionNames();
        checkForObsoleteOptionPrefix();
    }

    private static void checkForInvalidOptionNames() {
        boolean wrongOptionName = false;
        Properties allProperties = System.getProperties();
        for (String key : allProperties.stringPropertyNames()) {
            if (key.startsWith(OPTION_PREFIX)) {
                if (Property.fromKey(key) == null) {
                    wrongOptionName = true;
                    LLVMLogger.error(key + " is an invalid option!");
                }
            }
        }
        if (wrongOptionName) {
            LLVMLogger.error("\nvalid options:");
            printOptions();
            System.exit(-1);
        }
    }

    private static void printOptions() {
        LLVMOptions.main(new String[0]);
    }

    private static void checkForObsoleteOptionPrefix() {
        Properties allProperties = System.getProperties();
        for (String key : allProperties.stringPropertyNames()) {
            if (key.startsWith(OBSOLETE_OPTION_PREFIX)) {
                LLVMLogger.error(
                                "The prefix '" + OBSOLETE_OPTION_PREFIX + "' in option '" + key + "' is an obsolete option prefix and has been replaced by the prefix '" + OPTION_PREFIX + "':");
                printOptions();
                System.exit(-1);
            }
        }
    }

    private static void parseOptions() {
        Property[] properties = Property.values();
        for (Property prop : properties) {
            parsedProperties.put(prop, prop.parse());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getParsedProperty(Property property) {
        return (T) parsedProperties.get(property);
    }

}
