package org.geoserver.web.data.store.cache;

import java.util.logging.Logger;

import org.geotools.data.cache.utils.CacheUtils;
import org.geotools.data.cache.utils.EHCacheUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BeanFactory implements BeanFactoryPostProcessor {

    protected final static transient Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geoserver.web.data.store.cache.BeanFactory");

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        // for (String beanName : dependencies.keySet()) {
        final String beanName = "geoServerLoader";
        final BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
        bd.setDependsOn(StringUtils.mergeStringArrays(bd.getDependsOn(), new String[] {
                CacheUtils.BEAN_NAME, EHCacheUtils.BEAN_NAME }));
        // }

    }

}
