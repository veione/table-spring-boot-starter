package com.think.table.reader.csv.bean;

import com.think.table.reader.csv.bean.function.AccessorInvoker;
import com.think.table.reader.csv.bean.function.AssignmentInvoker;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Encapsulates the logic for accessing member variables of classes.
 * <p>The logic in opencsv is always:<ol>
 *     <li>Use an accessor method first, if available, and this always has the
 *     form "get"/"set" + member name with initial capital.</li>
 *     <li>If this accessor method is available but deals in
 *     {@link Optional}, wrap or unwrap as necessary. Empty
 *     {@link Optional}s lead to {@code null} return values, and
 *     {@code null} values lead to empty {@link Optional}s.</li>
 *     <li>Use reflection bypassing all access control restrictions.</li>
 * </ol>These are considered separately for reading and writing.</p>
 *
 * @param <T> The type of the member variable being accessed
 * @author Andrew Rucker Jones
 * @since 5.0
 */
public class FieldAccess<T> {

    /** The field being accessed. */
    private final Field field;

    /** A functional interface to read the field. */
    private final AccessorInvoker<Object, T> accessor;

    /** A functional interface to write the field. */
    private final AssignmentInvoker<Object, T> assignment;

    /**
     * Constructs this instance by determining what mode of access will work
     * for this field.
     *
     * @param field The field to be accessed.
     */
    public FieldAccess(Field field) {
        this.field = field;
        accessor = determineAccessorMethod();
        assignment = determineAssignmentMethod();
    }

    @SuppressWarnings("unchecked")
    private AccessorInvoker<Object, T> determineAccessorMethod() {
        AccessorInvoker<Object, T> localAccessor;
        String getterName = "get" + Character.toUpperCase(field.getName().charAt(0))
                + field.getName().substring(1);
        try {
            Method getterMethod = field.getDeclaringClass().getMethod(getterName);
            if(getterMethod.getReturnType().equals(Optional.class)) {
                localAccessor = bean -> {
                    Optional<T> opt = (Optional<T>) getterMethod.invoke(bean);
                    return opt.orElse(null);
                };
            }
            else {
                localAccessor = bean -> (T) getterMethod.invoke(bean);
            }
        } catch (NoSuchMethodException e) {
            localAccessor = bean -> (T)FieldUtils.readField(this.field, bean, true);
        }
        return localAccessor;
    }

    private AssignmentInvoker<Object, T> determineAssignmentMethod() {
        AssignmentInvoker<Object, T> localAssignment;
        String setterName = "set" + Character.toUpperCase(field.getName().charAt(0))
                + field.getName().substring(1);
        try {
            Method setterMethod = field.getDeclaringClass().getMethod(setterName, field.getType());
            localAssignment = setterMethod::invoke;
        } catch (NoSuchMethodException e1) {
            try {
                Method setterMethod = field.getDeclaringClass().getMethod(setterName, Optional.class);
                localAssignment = (bean, value) -> setterMethod.invoke(bean, Optional.ofNullable(value));
            }
            catch(NoSuchMethodException e2) {
                localAssignment = (bean, value) -> FieldUtils.writeField(this.field, bean, value, true);
            }
        }
        return localAssignment;
    }

    /**
     * Returns the value of the field in the given bean.
     * @param bean The bean from which the value of this field should be returned
     * @return The value of this member variable
     * @throws IllegalAccessException If there is a problem accessing the
     * member variable
     * @throws InvocationTargetException If there is a problem accessing the
     * member variable
     */
    public T getField(Object bean) throws IllegalAccessException, InvocationTargetException {
        return accessor.invoke(bean);
    }

    /**
     * Sets the value of the field in the given bean.
     * @param bean The bean in which the value of the field should be set
     * @param value The value to be written into the member variable of the bean
     * @throws IllegalAccessException If there is a problem accessing the
     * member variable
     * @throws InvocationTargetException If there is a problem accessing the
     * member variable
     */
    public void setField(Object bean, T value) throws IllegalAccessException, InvocationTargetException{
        assignment.invoke(bean, value);
    }

    /**
     * Creates a hash code for this object.
     * This override delegates hash code creation to the field passed in
     * through the constructor and does not includes any of its own state
     * information.
     */
    @Override
    public int hashCode() {
        return field.hashCode();
    }

    /**
     * Determines equality between this object and another.
     * This override delegates equality determination to the field passed in
     * through the constructor and does not includes any of its own state
     * information.
     */
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FieldAccess)) {
            return false;
        }
        return field.equals(((FieldAccess)obj).field);
    }

    /**
     * Returns a string representation of this object.
     * This override delegates the string representation to the field passed in
     * through the constructor and does not includes any of its own state
     * information.
     */
    @Override
    public String toString() {
        return field.toString();
    }
}
