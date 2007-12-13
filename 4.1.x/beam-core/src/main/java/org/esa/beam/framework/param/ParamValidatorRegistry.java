/*
 * $Id: ParamValidatorRegistry.java,v 1.1.1.1 2006/09/11 08:16:46 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.param;

import java.util.Map;

import org.esa.beam.framework.param.validators.BooleanValidator;
import org.esa.beam.framework.param.validators.ColorValidator;
import org.esa.beam.framework.param.validators.FileValidator;
import org.esa.beam.framework.param.validators.NumberValidator;
import org.esa.beam.framework.param.validators.StringArrayValidator;
import org.esa.beam.framework.param.validators.StringValidator;

/**
 * A <code>ParamValidatorRegistry</code> stores the different validators for each of the different parameter types.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $  $Date: 2006/09/11 08:16:46 $
 */
public class ParamValidatorRegistry {

    private static Map _validators = new java.util.Hashtable();

    static {
        registerValidator(java.lang.Short.class, new NumberValidator());
        registerValidator(java.lang.Integer.class, new NumberValidator());
        registerValidator(java.lang.Long.class, new NumberValidator());
        registerValidator(java.lang.Float.class, new NumberValidator());
        registerValidator(java.lang.Double.class, new NumberValidator());
        registerValidator(java.lang.String.class, new StringValidator());
        registerValidator(java.lang.String[].class, new StringArrayValidator());
        registerValidator(java.lang.Boolean.class, new BooleanValidator());
        registerValidator(java.awt.Color.class, new ColorValidator());
        registerValidator(java.io.File.class, new FileValidator());
    }


    /**
     * Returns the default validator, which is guaranteed to be different from <code>null</code>. The method first look
     * for a validator registred for the <code>String</code> class, if it is not found then a new instance of
     * <code>StringValidator</code> is returned.
     *
     * @see #getValidator(Class)
     */
    public static ParamValidator getDefaultValidator() {
        ParamValidator validator = getValidator(java.lang.String.class);
        if (validator == null) {
            validator = new StringValidator();
        }
        return validator;
    }

    /**
     * Returns a validator for the given value type, which is guaranteed to be different from <code>null</code>. <p> If
     * given value type is <code>null</code>, the method returns the value of <code>getDefaultValidator()</code>.
     *
     * @see #getDefaultValidator()
     */
    public static ParamValidator getValidator(Class valueType) {
        return valueType != null
               ? (ParamValidator) _validators.get(valueType)
               : getDefaultValidator();
    }

    public static void registerValidator(Class valueType, ParamValidator validator) {
        _validators.put(valueType, validator);
    }

    public static boolean deregisterValidator(Class valueType) {
        return _validators.remove(valueType) != null;
    }

    /**
     * Private constructor. Used to prevent instantiation of this class.
     */
    private ParamValidatorRegistry() {
    }
}




