/**
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kaczmarzyk.spring.data.jpa.domain;

import net.kaczmarzyk.spring.data.jpa.utils.Converter;
import net.kaczmarzyk.spring.data.jpa.utils.QueryContext;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Objects;


/**
 * <p>Filers with "is null" or "is not null" where clause (e.g. {@code where nickName is null}).</p>
 *
 * <p>Requires boolean parameter.</p>
 *
 * @author Tomasz Kaczmarzyk
 * @author Gerald Humphries
 */
public class Null<T> extends PathSpecification<T> {

	private static final long serialVersionUID = 1L;
	
	protected String expectedValue;
	private Converter converter;

	public Null(QueryContext queryContext, String path, String[] httpParamValues, Converter converter) {
		super(queryContext, path);
		if (httpParamValues == null || httpParamValues.length != 1) {
			throw new IllegalArgumentException();
		}
		this.expectedValue = httpParamValues[0];
		this.converter = converter;
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
		if (converter.convert(expectedValue, Boolean.class)) {
			return cb.isNull(path(root));
		} else {
			return cb.isNotNull(path(root));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		Null<?> aNull = (Null<?>) o;
		return Objects.equals(expectedValue, aNull.expectedValue) &&
				Objects.equals(converter, aNull.converter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), expectedValue, converter);
	}

	@Override
	public String toString() {
		return "Null[" +
				"expectedValue='" + expectedValue + '\'' +
				", converter=" + converter +
				", path=" + path +
				']';
	}
}
