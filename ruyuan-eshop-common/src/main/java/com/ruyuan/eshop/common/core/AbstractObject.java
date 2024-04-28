package com.ruyuan.eshop.common.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 基础POJO类
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractObject {

    /**
     * 浅度克隆
     *
     * @param targetClazz 目标对象的Class类型
     * @return 目标对象实例
     */
    public <T> T clone(Class<T> targetClazz) {
        try {
            T target = targetClazz.newInstance();
            BeanCopierUtil.copyProperties(this, target);

            return getTarget(target);
        } catch (Exception e) {
            throw new RuntimeException("error", e);
        }

    }

    /**
     * 浅度克隆
     *
     * @param target 目标对象实例
     * @return 目标对象实例
     */
    public <T> T clone(T target) {
        try {
            BeanCopierUtil.copyProperties(this, target);
            return getTarget(target);
        } catch (Exception e) {
            throw new RuntimeException("error", e);
        }
    }

    /**
     * 深度克隆
     *
     * @param targetClazz    目标对象的Class类型
     * @param cloneDirection 深入克隆的方向，具体赋值参见CloneDirection.java常量类
     * @return 目标对象实例
     */
    public <T> T clone(Class<T> targetClazz, CloneDirection cloneDirection) {
        try {

            // 先完成基本字段的浅克隆
            T target = targetClazz.newInstance();
            BeanCopierUtil.copyProperties(this, target);

            // 完成内部的AbstractObject、List<AbstractObject>类型字段的深度克隆
            Class<?> thisClazz = this.getClass();
            List<Field> thisFields = listThisField(null, thisClazz);

            for (Field thisField : thisFields) {
                thisField.setAccessible(true);

                // 如果判断某个字段是List类型的
                if (!Collection.class.isAssignableFrom(thisField.getType())) {
                    Class<?> sourceFieldClazz = thisField.getType();
                    if (sourceFieldClazz == String.class || sourceFieldClazz == BigDecimal.class
                            || thisField.getType() == Integer.class || "int".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Long.class || "long".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Short.class || "short".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Double.class || "double".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Float.class || "float".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Boolean.class || "boolean".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Character.class || "char".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Byte.class || "byte".equals(sourceFieldClazz.toString())
                            || sourceFieldClazz == Date.class || sourceFieldClazz == java.sql.Date.class
                            || sourceFieldClazz == LocalDateTime.class || sourceFieldClazz == LocalDate.class) {
                        continue;
                    }
                    // 判断某个字段是否AbstractObject类型的
                    try {
                        if (!(thisField.getType().newInstance() instanceof AbstractObject)) {
                            continue;
                        }
                    } catch (Exception e) {
                        if (e instanceof InstantiationException) {
                            continue;
                        }
                        throw new RuntimeException("error", e);
                    }
                    AbstractObject sourceObj = (AbstractObject) (thisField.get(this));
                    if (sourceObj == null) {
                        continue;
                    }

                    // 获取要克隆的目标类型
                    //Class<?> cloneTargetClazz = getCloneTargetClazz(field.getType(), cloneDirection);
                    Field targetField = null;
                    try {
                        targetField = getTargetClazzField(thisField, targetClazz);
                    } catch (NoSuchFieldException e) {
                        continue;
                    }
                    if (targetField != null) {
                        Class<?> cloneTargetClazz = targetField.getType();
                        AbstractObject clonedObj = (AbstractObject) sourceObj.clone(cloneTargetClazz, cloneDirection);
                        // 获取设置克隆好的对象的方法名称
                        String name = thisField.getName();
                        String setMethodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
                        // getMethod()方法可以获取当前类与父类中所有的public方法
                        Method setFieldMethod = targetClazz.getMethod(setMethodName, targetField.getType());
                        setFieldMethod.invoke(target, clonedObj);
                    }
                } else {
                    Collection<?> list = (Collection<?>) thisField.get(this);
                    if (list == null || list.size() == 0) {
                        continue;
                    }

                    // 获取List集合中的泛型类型
                    Field targetField = null;
                    try {
                        targetField = getTargetClazzField(thisField, targetClazz);
                    } catch (NoSuchFieldException e) {
                        continue;
                    }
                    if (targetField != null) {
                        Class<?> cloneTargetClazz = getTargetListGenericType(targetField);
                        // 获取要克隆的目标类型
                        //Class<?> cloneTargetClazz = getCloneTargetClazz(listGenericClazz, cloneDirection);
                        // 将list集合克隆到目标list集合中去
                        Collection clonedList = (Collection) thisField.get(this).getClass().newInstance();
                        cloneList(list, clonedList, cloneTargetClazz, cloneDirection);

                        // 获取设置克隆好的list的方法名称
                        String name = thisField.getName();
                        String setMethodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
                        // getMethod()方法可以获取当前类与父类中所有的public方法
                        Method setFieldMethod = targetClazz.getMethod(setMethodName, targetField.getType());
                        setFieldMethod.invoke(target, clonedList);
                    }
                }

            }

            return target;
        } catch (Exception e) {
            throw new RuntimeException("error", e);
        }
    }

    /**
     * 递归获取当前类以及父类中的字段
     *
     * @param thisFields 当前类以及父类中的所有字段
     * @param thisClazz  原始类Class对象
     */
    public List<Field> listThisField(List<Field> thisFields, Class<?> thisClazz) {
        if (thisFields == null) {
            thisFields = new ArrayList<>(Arrays.asList(thisClazz.getDeclaredFields()));
        } else {
            thisFields.addAll(Arrays.asList(thisClazz.getDeclaredFields()));
        }

        if (!thisClazz.getSuperclass().getTypeName().equals(AbstractObject.class.getTypeName())) {
            listThisField(thisFields, thisClazz.getSuperclass());
        }
        return thisFields;
    }

    /**
     * 如果目标有继承父类需要递归获取目标字段
     *
     * @param thisField   源对象中的某个字段
     * @param targetClazz 目标对象的class类型
     */
    private Field getTargetClazzField(Field thisField, Class<?> targetClazz) throws NoSuchFieldException {
        Field targetField = null;
        try {
            targetField = targetClazz.getDeclaredField(thisField.getName());
        } catch (NoSuchFieldException e) {
            // 目标类有可能没有继承AbstractObject类
            if (targetClazz.getSuperclass() != null) {
                String targetSuperClazzTypeName = targetClazz.getSuperclass().getTypeName();
                if (!targetSuperClazzTypeName.equals(Object.class.getTypeName()) && !targetSuperClazzTypeName.equals(AbstractObject.class.getTypeName())) {
                    // 递归
                    targetField = getTargetClazzField(thisField, targetClazz.getSuperclass());
                }
            }

            if (targetField == null) {
                throw e;
            }

        }
        return targetField;

    }

    /**
     * 将一个List克隆到另外一个List
     *
     * @param sourceList       原集合对象
     * @param targetList       目标集合对象
     * @param cloneTargetClazz 目标集合对象中元素类型
     * @param cloneDirection   深度克隆
     */
    private void cloneList(Collection sourceList, Collection targetList, Class cloneTargetClazz, CloneDirection cloneDirection) {
        for (Object object : sourceList) {
            if (object instanceof AbstractObject) {
                AbstractObject targetObject = (AbstractObject) object;
                AbstractObject clonedObject = (AbstractObject) targetObject.clone(cloneTargetClazz, cloneDirection);
                targetList.add(clonedObject);
            } else {
                // 非List<? extends AbstractObject>类型的集合字段，直接复用原对象的字段值
                targetList.add(object);
            }
        }
    }

    /**
     * 获取List集合的泛型类型
     *
     * @param targetField 目标字段
     */
    private Class<?> getTargetListGenericType(Field targetField) {
        Type genericType = targetField.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            return (Class<?>) parameterizedType.getActualTypeArguments()[0];
        }
        return null;
    }

    /**
     * 浅度克隆时原对象List属性的处理
     *
     * @param target 目标对象
     */
    private <T> T getTarget(T target) throws Exception {
        Class<?> targetClazz = target.getClass();
        Field[] fields = targetClazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);

            // 如果判断某个字段是List类型的
            if (field.getType() != List.class) {
                continue;
            }
            List<?> list = (List<?>) field.get(target);
            if (list == null || list.size() == 0) {
                continue;
            }

            Class<?> targetListGenericTypeClazz = getTargetListGenericType(field);
            if (targetListGenericTypeClazz != null && !isAbstractObjectClass(targetListGenericTypeClazz)) {
                continue;
            }

            String name = field.getName();
            String setMethodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);

            Method setFieldMethod = targetClazz.getMethod(setMethodName, field.getType());
            setFieldMethod.invoke(target, new ArrayList());
        }

        return target;
    }

    /**
     * 判断某个Class的对象类是否继承了AbstractObject
     *
     * @param clazz class对象
     */
    private boolean isAbstractObjectClass(Class clazz) {
        // 目标类有可能没有继承AbstractObject类
        if (clazz.getSuperclass() != null) {
            String superClazzTypeName = clazz.getSuperclass().getTypeName();
            if (superClazzTypeName.equals(Object.class.getTypeName())) {
                return false;
            }
            if (superClazzTypeName.equals(AbstractObject.class.getTypeName())) {
                return true;
            } else {
                return isAbstractObjectClass(clazz.getSuperclass());
            }
        }
        return false;
    }

}