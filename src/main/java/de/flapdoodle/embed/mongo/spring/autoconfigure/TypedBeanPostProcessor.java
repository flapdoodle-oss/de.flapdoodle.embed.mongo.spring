/*
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	...
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.embed.mongo.spring.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.function.Function;

public class TypedBeanPostProcessor<T> implements BeanPostProcessor {

	private final Class<T> type;
	private final Function<T, T> beforeInit;
	private final Function<T, T> afterInit;

	public TypedBeanPostProcessor(Class<T> type, Function<T, T> beforeInit, Function<T, T> afterInit) {
		this.type = type;
		this.beforeInit = beforeInit;
		this.afterInit = afterInit;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (type.isInstance(bean)) {
			return beforeInit.apply((T) bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (type.isInstance(bean)) {
			return afterInit.apply((T) bean);
		}
		return bean;
	}

	public static <T> TypedBeanPostProcessor<T> applyBeforeInitialization(Class<T> type, Function<T, T> change) {
		return new TypedBeanPostProcessor<>(type, change, Function.identity());
	}
}
