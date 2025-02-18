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
package net.kaczmarzyk;

import net.kaczmarzyk.spring.data.jpa.Customer;
import net.kaczmarzyk.spring.data.jpa.CustomerDto;
import net.kaczmarzyk.spring.data.jpa.CustomerRepository;
import net.kaczmarzyk.spring.data.jpa.ItemTag;
import net.kaczmarzyk.spring.data.jpa.domain.LikeIgnoreCase;
import net.kaczmarzyk.spring.data.jpa.utils.ThrowableAssertions;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Join;
import net.kaczmarzyk.spring.data.jpa.web.annotation.JoinFetch;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import net.kaczmarzyk.utils.interceptor.HibernateStatementInterceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.NestedServletException;

import static java.util.stream.Collectors.toList;
import static javax.persistence.criteria.JoinType.LEFT;
import static net.kaczmarzyk.spring.data.jpa.CustomerBuilder.customer;
import static net.kaczmarzyk.spring.data.jpa.ItemTagBuilder.itemTag;
import static net.kaczmarzyk.spring.data.jpa.OrderBuilder.order;
import static net.kaczmarzyk.spring.data.jpa.utils.ThrowableAssertions.assertThrows;
import static net.kaczmarzyk.utils.interceptor.InterceptedStatementsAssert.assertThatInterceptedStatements;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Jakub Radlica
 */
public class JoinAndJoinFetchReferringToTheSameTableTest extends E2eTestBase {

	@Controller
	public static class TestController {

		@Autowired
		CustomerRepository customerRepository;

		@RequestMapping(value = "/join-and-join-fetch/customers_join")
		@ResponseBody
		public Object findByOrderedItemTagName_join(
				@Join(path = "orders", alias = "o", type = LEFT)
				@Join(path = "o.tags", alias = "t", type = LEFT)
				@Spec(path = "t.name", params = "tagName", spec = LikeIgnoreCase.class)
						Specification<Customer> spec) {

			return customerRepository.findAll(spec, Sort.by("id")).stream()
					.map(CustomerDto::from)
					.collect(toList());
		}

		@RequestMapping(value = "/join-and-join-fetch/customers_join_fetch")
		@ResponseBody
		public Object findByOrderedItemTagName_join_fetch(
				@JoinFetch(paths = "orders", alias = "o")
				@JoinFetch(paths = "o.tags", alias = "t")
				@Spec(path = "t.name", params = "tagName", spec = LikeIgnoreCase.class)
						Specification<Customer> spec) {

			return customerRepository.findAll(spec, Sort.by("id")).stream()
					.map(CustomerDto::from)
					.collect(toList());
		}

		@RequestMapping(value = "/join-and-join-fetch/customers_join_and_join_fetch")
		@ResponseBody
		public Object findByOrderedItemTagName_join_and_join_fetch(
				@JoinFetch(paths = "orders", alias = "o")
				@JoinFetch(paths = "o.tags", alias = "t")
				@Join(path = "orders", alias = "o", type = LEFT)
				@Join(path = "o.tags", alias = "t", type = LEFT)
				@Spec(path = "t.name", params = "tagName", spec = LikeIgnoreCase.class)
						Specification<Customer> spec) {

			return customerRepository.findAll(spec, Sort.by("id")).stream()
					.map(CustomerDto::from)
					.collect(toList());
		}

		@RequestMapping(value = "/join-and-join-fetch/customers_joinPathContainingJoinFetchAlias")
		@ResponseBody
		public Object findByOrderedItemTagName_joinPathContainingJoinFetchAlias(
				@JoinFetch(paths = "orders", alias = "o")
				@Join(path = "o.tags", alias = "t", type = LEFT)
				@Spec(path = "t.name", params = "tagName", spec = LikeIgnoreCase.class)
						Specification<Customer> spec) {

			return customerRepository.findAll(spec, Sort.by("id")).stream()
					.map(CustomerDto::from)
					.collect(toList());
		}

		@RequestMapping(value = "/join-and-join-fetch/customers_joinFetchPathContainingJoinAlias")
		@ResponseBody
		public Object findByOrderedItemTagName_joinFetchPathContainingJoinAlias(
				@Join(path = "orders", alias = "o")
				@JoinFetch(paths = "o.tags", alias = "t")
				@Spec(path = "t.name", params = "tagName", spec = LikeIgnoreCase.class)
						Specification<Customer> spec) {

			return customerRepository.findAll(spec, Sort.by("id")).stream()
					.map(CustomerDto::from)
					.collect(toList());
		}
	}

	@Before
	public void initializeTestData() {
		doInNewTransaction(() -> {
			ItemTag snacksTag = itemTag("#snacks").build(em);
			ItemTag fruitsTag = itemTag("#fruits").build(em);

			customer("Homer", "Simpson")
					.orders(
							order("Duff Beer").withTags(snacksTag),
							order("Donuts").withTags(snacksTag)).build(em);

			customer("Marge", "Simpson")
					.orders(
							order("Apple").withTags(fruitsTag)).build(em);

			customer("Bart", "Simpson")
					.orders(
							order("Pizza").withTags(snacksTag)).build(em);

			customer("Lisa", "Simpson")
					.orders(
							order("Jazz music disc")).build(em);

			customer("Maggie", "Simpson").build(em);
		});
	}

	@After
	public void cleanupDb() {
		doInNewTransaction(() -> {
			customerRepo.deleteAll();
			itemTagRepository.deleteAll();
		});
	}

	@Test
	public void findsByOrdersAndName_join() throws Exception {
		HibernateStatementInterceptor.clearInterceptedStatements();

		performRequestAndAssertResponseContent("/join-and-join-fetch/customers_join");

		assertThatInterceptedStatements()
				// N+1 SELECT problem
				.hasSelects(5)
				.hasJoins(5)
				.hasOneClause(" left outer join orders ")
				.hasOneClause(" left outer join orders_tags ")
				.hasOneClause(" left outer join item_tags ")
				.hasClause(" inner join item_tags ", 2);
	}

	@Test
	public void findsByOrdersAndName_join_fetch() throws Exception {
		HibernateStatementInterceptor.clearInterceptedStatements();

		performRequestAndAssertResponseContent("/join-and-join-fetch/customers_join_fetch");

		assertThatInterceptedStatements()
				.hasSelects(1)
				// Verifying that all lazy collection is initialized due to join fetch usage.
				.hasJoins(3)
				.hasOneClause(" left outer join orders ")
				.hasOneClause(" left outer join orders_tags ")
				.hasOneClause(" left outer join item_tags ");
	}

	@Test
	public void findsByOrdersAndName_join_and_join_fetch() throws Exception {
		HibernateStatementInterceptor.clearInterceptedStatements();

		performRequestAndAssertResponseContent("/join-and-join-fetch/customers_join_and_join_fetch");

		assertThatInterceptedStatements()
				.hasSelects(1)
				// Example of redundant joins due to usage @Join and @JoinFetch with the same aliases
				.hasJoins(6)
				.hasClause(" left outer join orders ", 2)
				.hasClause(" left outer join orders_tags ", 2)
				.hasClause(" left outer join item_tags ", 2);
	}

	@Test
	public void throwsNestedServletExceptionWhenJoinPathContainsJoinFetchAlias() throws Exception {
		HibernateStatementInterceptor.clearInterceptedStatements();

		assertThrows(
				NestedServletException.class,
				() -> {
					mockMvc.perform(get("/join-and-join-fetch/customers_joinPathContainingJoinFetchAlias")
							.param("tagName", "#snacks")
							.accept(MediaType.APPLICATION_JSON))
							.andExpect(status().isOk());
				}
		);

	}

	@Test
	public void throwsNestedServletExceptionWhenJoinFetchPathContainsJoinAlias() throws Exception {
		HibernateStatementInterceptor.clearInterceptedStatements();

		assertThrows(
				NestedServletException.class,
				() -> {
					mockMvc.perform(get("/join-and-join-fetch/customers_joinFetchPathContainingJoinAlias")
							.param("tagName", "#snacks")
							.accept(MediaType.APPLICATION_JSON))
							.andExpect(status().isOk());
				}
		);

	}

	private void performRequestAndAssertResponseContent(String url) throws Exception {
		mockMvc.perform(get(url)
				.param("tagName", "#snacks")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].firstName").value("Homer"))
			.andExpect(jsonPath("$[0].tagOfFirstCustomerItem").value("#snacks"))
			.andExpect(jsonPath("$[1].firstName").value("Bart"))
			.andExpect(jsonPath("$[1].tagOfFirstCustomerItem").value("#snacks"))
			.andExpect(jsonPath("$[2]").doesNotExist());
	}

}
