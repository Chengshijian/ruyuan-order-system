package com.ruyuan.eshop.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyuan.eshop.common.core.AbstractObject;
import com.ruyuan.eshop.common.core.CloneDirection;
import com.ruyuan.eshop.common.core.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 对象克隆工具类
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public class ObjectUtil {

    /**
     * 转换集合-浅克隆
     *
     * @param sourceList  源集合
     * @param targetClazz 目标集合元素类型
     * @return 转换后的集合
     */
    public static <T> List<T> convertList(
            List<? extends AbstractObject> sourceList, Class<T> targetClazz) {
        if (sourceList == null) {
            return null;
        }

        List<T> targetList = new ArrayList<T>();
        for (AbstractObject sourceObject : sourceList) {
            targetList.add(sourceObject.clone(targetClazz));
        }

        return targetList;
    }

    /**
     * 转换集合-深度克隆
     *
     * @param sourceList     源集合
     * @param targetClazz    目标集合元素类型
     * @param cloneDirection 深度分页参数 @See CloneDirection.java
     * @return 转换后的集合
     */
    public static <T> List<T> convertList(List<? extends AbstractObject> sourceList,
                                          Class<T> targetClazz, CloneDirection cloneDirection) {
        if (sourceList == null) {
            return null;
        }

        List<T> targetList = new ArrayList<T>();
        for (AbstractObject sourceObject : sourceList) {
            targetList.add(sourceObject.clone(targetClazz, cloneDirection));
        }

        return targetList;
    }

    /**
     * 转换集合-浅克隆
     *
     * @param sourceList  源集合
     * @param targetClazz 目标集合元素类型
     * @return 转换后的集合
     */
    public static <T> List<T> convertList(
            List<? extends AbstractObject> sourceList, Class<T> targetClazz, Consumer<T> consumer) {
        if (sourceList == null) {
            return null;
        }

        List<T> targetList = new ArrayList<T>();
        for (AbstractObject sourceObject : sourceList) {
            targetList.add(sourceObject.clone(targetClazz));
        }

        targetList.forEach(consumer);

        return targetList;
    }

    /**
     * 转换集合-深度克隆
     *
     * @param sourceList     源集合
     * @param targetClazz    目标集合元素类型
     * @param cloneDirection 深度分页参数 @See CloneDirection.java
     * @return 转换后的集合
     */
    public static <T> List<T> convertList(List<? extends AbstractObject> sourceList,
                                          Class<T> targetClazz, CloneDirection cloneDirection, Consumer<T> consumer) {
        if (sourceList == null) {
            return null;
        }

        List<T> targetList = new ArrayList<T>();
        for (AbstractObject sourceObject : sourceList) {
            targetList.add(sourceObject.clone(targetClazz, cloneDirection));
        }

        targetList.forEach(consumer);

        return targetList;
    }

    /**
     * 转换集合-浅克隆
     *
     * @param sourcePage  源分页集合
     * @param targetClazz 目标集合元素类型
     * @return 转换后的集合
     */
    public static <T> IPage<T> convertPage(
            IPage<? extends AbstractObject> sourcePage, Class<T> targetClazz) {
        if (sourcePage == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePage.getRecords();
        List<T> targetList = convertList(sourceList, targetClazz);
        IPage<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal(), sourcePage.isSearchCount());
        targetPage.setRecords(targetList);
        return targetPage;
    }

    /**
     * 转换集合-深度克隆
     *
     * @param sourcePage     源分页集合
     * @param targetClazz    目标集合元素类型
     * @param cloneDirection 深度分页参数 @See CloneDirection.java
     * @return 转换后的集合
     */
    public static <T> IPage<T> convertPage(IPage<? extends AbstractObject> sourcePage,
                                           Class<T> targetClazz, CloneDirection cloneDirection) {
        if (sourcePage == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePage.getRecords();
        List<T> targetList = convertList(sourceList, targetClazz, cloneDirection);
        IPage<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal(), sourcePage.isSearchCount());
        targetPage.setRecords(targetList);
        return targetPage;
    }

    /**
     * 转换集合-浅克隆
     *
     * @param sourcePage  源分页集合
     * @param targetClazz 目标集合元素类型
     * @return 转换后的集合
     */
    public static <T> IPage<T> convertPage(
            IPage<? extends AbstractObject> sourcePage, Class<T> targetClazz, Consumer<T> consumer) {
        if (sourcePage == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePage.getRecords();
        List<T> targetList = convertList(sourceList, targetClazz);
        IPage<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal(), sourcePage.isSearchCount());
        targetPage.setRecords(targetList);

        targetPage.getRecords().forEach(consumer);

        return targetPage;
    }

    /**
     * 转换集合-深度克隆
     *
     * @param sourcePage     源分页集合
     * @param targetClazz    目标集合元素类型
     * @param cloneDirection 深度分页参数 @See CloneDirection.java
     * @return 转换后的集合
     */
    public static <T> IPage<T> convertPage(IPage<? extends AbstractObject> sourcePage,
                                           Class<T> targetClazz, CloneDirection cloneDirection, Consumer<T> consumer) {
        if (sourcePage == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePage.getRecords();
        List<T> targetList = convertList(sourceList, targetClazz, cloneDirection);
        IPage<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal(), sourcePage.isSearchCount());
        targetPage.setRecords(targetList);

        targetPage.getRecords().forEach(consumer);

        return targetPage;
    }

    /**
     * 转换PageBean分页对象-浅度克隆
     *
     * @param sourcePageBean 源分页集合
     * @param targetClazz    目标集合元素类型
     * @return
     */
    public static <T> PageResult<T> convertPageResult(PageResult<? extends AbstractObject> sourcePageBean, Class<T> targetClazz) {
        if (sourcePageBean == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePageBean.getContent();

        List<T> targetList = convertList(sourceList, targetClazz);
        PageResult<T> targetPageBean = new PageResult<T>(targetList);
        targetPageBean.setNumber(sourcePageBean.getNumber());
        targetPageBean.setSize(sourcePageBean.getSize());
        targetPageBean.setTotalElements(sourcePageBean.getTotalElements());
        targetPageBean.setTotalPages(sourcePageBean.getTotalPages());
        targetPageBean.setNumberOfElements(sourcePageBean.getNumberOfElements());
        return targetPageBean;
    }

    /**
     * 转换PageBean分页对象-深度克隆
     *
     * @param sourcePageBean 源分页集合
     * @param targetClazz    目标集合元素类型
     * @param cloneDirection 深度分页参数 @See CloneDirection.java
     * @return
     */
    public static <T> PageResult<T> convertPageResult(PageResult<? extends AbstractObject> sourcePageBean, Class<T> targetClazz, CloneDirection cloneDirection) {
        if (sourcePageBean == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePageBean.getContent();
        List<T> targetList = convertList(sourceList, targetClazz, cloneDirection);
        PageResult<T> targetPageBean = new PageResult<T>(targetList);
        targetPageBean.setNumber(sourcePageBean.getNumber());
        targetPageBean.setSize(sourcePageBean.getSize());
        targetPageBean.setTotalElements(sourcePageBean.getTotalElements());
        targetPageBean.setTotalPages(sourcePageBean.getTotalPages());
        targetPageBean.setNumberOfElements(sourcePageBean.getNumberOfElements());
        return targetPageBean;
    }

    /**
     * 转换PageBean分页对象-浅度克隆
     *
     * @param sourcePageBean 源分页集合
     * @param targetClazz    目标集合元素类型
     * @return
     */
    public static <T> PageResult<T> convertPageResult(PageResult<? extends AbstractObject> sourcePageBean, Class<T> targetClazz, Consumer<T> consumer) {
        if (sourcePageBean == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePageBean.getContent();

        List<T> targetList = convertList(sourceList, targetClazz);
        PageResult<T> targetPageBean = new PageResult<T>(targetList);
        targetPageBean.setNumber(sourcePageBean.getNumber());
        targetPageBean.setSize(sourcePageBean.getSize());
        targetPageBean.setTotalElements(sourcePageBean.getTotalElements());
        targetPageBean.setTotalPages(sourcePageBean.getTotalPages());
        targetPageBean.setNumberOfElements(sourcePageBean.getNumberOfElements());

        targetPageBean.getContent().forEach(consumer);

        return targetPageBean;
    }

    /**
     * 转换PageBean分页对象-深度克隆
     *
     * @param sourcePageBean 源分页集合
     * @param targetClazz    目标集合元素类型
     * @param cloneDirection 深度分页参数 @See CloneDirection.java
     * @return
     */
    public static <T> PageResult<T> convertPageResult(PageResult<? extends AbstractObject> sourcePageBean, Class<T> targetClazz, CloneDirection cloneDirection, Consumer<T> consumer) {
        if (sourcePageBean == null) {
            return null;
        }
        List<? extends AbstractObject> sourceList = sourcePageBean.getContent();
        List<T> targetList = convertList(sourceList, targetClazz, cloneDirection);
        PageResult<T> targetPageBean = new PageResult<T>(targetList);
        targetPageBean.setNumber(sourcePageBean.getNumber());
        targetPageBean.setSize(sourcePageBean.getSize());
        targetPageBean.setTotalElements(sourcePageBean.getTotalElements());
        targetPageBean.setTotalPages(sourcePageBean.getTotalPages());
        targetPageBean.setNumberOfElements(sourcePageBean.getNumberOfElements());

        targetPageBean.getContent().forEach(consumer);

        return targetPageBean;
    }

}