package com.example.demo;

import com.example.demo.controllers.CartController;
import com.example.demo.controllers.OrderController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SareetaApplicationTests {
	private UserController userController;
	private CartController cartController;
	private OrderController orderController;
	private final UserRepository userRepositoryMock = mock(UserRepository.class);
	private final CartRepository cartRepositoryMock = mock(CartRepository.class);
	private final ItemRepository itemRepositoryMock = mock(ItemRepository.class);
	private final OrderRepository orderRepositoryMock = mock(OrderRepository.class);
	private final BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);

	@Before
	public void init() {
		userController = new UserController();
		cartController = new CartController();
		orderController = new OrderController();
		// Inject Mocks into controller instance
		TestUtils.injectObjects(userController,"userRepository", userRepositoryMock);
		TestUtils.injectObjects(userController,"cartRepository", cartRepositoryMock);
		TestUtils.injectObjects(userController,"bCryptPasswordEncoder", encoder);

		TestUtils.injectObjects(cartController,"userRepository",userRepositoryMock);
		TestUtils.injectObjects(cartController,"cartRepository",cartRepositoryMock);
		TestUtils.injectObjects(cartController,"itemRepository",itemRepositoryMock);

		TestUtils.injectObjects(orderController,"userRepository",userRepositoryMock);
		TestUtils.injectObjects(orderController,"orderRepository",orderRepositoryMock);
	}

	@Test
	public void testUserSignUpFlow() {
		// Prepare User Information Request
		when(encoder.encode("password@123")).thenReturn("abc123abc123");
		CreateUserRequest createUserRequest = new CreateUserRequest();
		createUserRequest.setUsername("testUser");
		createUserRequest.setPassword("password@123");
		createUserRequest.setConfirmPassword("password@123");

		// Perform Request
		final ResponseEntity<User> response = userController.createUser(createUserRequest);
		assert response != null;
		Assert.assertEquals(200,response.getStatusCodeValue());

		// Verify Request Data
		User user = response.getBody();
        assert user != null;
        Assert.assertEquals(0, user.getId());
		Assert.assertEquals("testUser",user.getUsername());
		Assert.assertEquals("abc123abc123",user.getPassword());

		// Verify User Exists in MOCK DB
		when(userRepositoryMock.findByUsername("testUser")).thenReturn(user);
		ResponseEntity<User> oldUser = userController.findByUserName("testUser");
		Assert.assertEquals(200, oldUser.getStatusCodeValue());

		User requestUser = oldUser.getBody();
        assert requestUser != null;
        Assert.assertEquals("testUser", requestUser.getUsername());
		Assert.assertEquals(0, requestUser.getId());
	}

	@Test
	public void testCartSubmitOrderFlow() {
		// Prepare test data
		User user = new User();
		user.setUsername("testUser");

		Item item = new Item();
		item.setId(0L);
		item.setName("Apple");
		item.setDescription("Delicious Red Apple");
		item.setPrice(new BigDecimal("2.99"));

		// Add Item to Cart
		List<Item> itemList = new ArrayList<>();
		itemList.add(item);
		Cart cart = new Cart();
		cart.setItems(itemList);
		cart.setUser(user);

		// Assign Cart to User
		user.setCart(cart);

		// Prepare User's Cart Information Request
		ModifyCartRequest modifyCartRequest = new ModifyCartRequest();
		modifyCartRequest.setUsername("testUser");
		modifyCartRequest.setQuantity(1);
		modifyCartRequest.setItemId(0L);

		// Prepare Order
		UserOrder testOrder = new UserOrder();
		testOrder.setId(0L);
		testOrder.setItems(itemList);
		testOrder.setUser(user);
		testOrder.setTotal(new BigDecimal("2.99"));

		List<UserOrder> listOrders = new ArrayList<>();
		listOrders.add(testOrder);

		// Add Test User & Test Order To Mock DB
		when(userRepositoryMock.findByUsername("testUser")).thenReturn(user);
		when(itemRepositoryMock.findById(0L)).thenReturn(Optional.of(item));
		when(orderRepositoryMock.save(any())).thenReturn(testOrder);
		when(orderRepositoryMock.findByUser(user)).thenReturn(listOrders);

		// Perform Add To Cart Request
		ResponseEntity<Cart> response = cartController.addTocart(modifyCartRequest);
		assert response != null;
		Assert.assertEquals(200, response.getStatusCodeValue());

		// Verify Request Data
		Cart requestCart = response.getBody();
        assert requestCart != null;
        Item requestCartItem = requestCart.getItems().get(0);

		Assert.assertEquals(user, requestCart.getUser());
		Assert.assertEquals(new BigDecimal("2.99"), requestCart.getTotal());
		Assert.assertEquals("Apple", requestCartItem.getName());

		// Perform Submit Order Request
		ResponseEntity<UserOrder> orderResponse = orderController.submit("testUser");

		// Verify Request Data
		assert orderResponse != null;
		Assert.assertEquals(200, orderResponse.getStatusCodeValue());

		UserOrder requestOrder = orderResponse.getBody();
		assert requestOrder != null;
		Assert.assertEquals(new BigDecimal("2.99"), requestOrder.getTotal());

		// Perform Remove From Cart Request
		ResponseEntity<Cart> deleteItemCartResponse = cartController.removeFromcart(modifyCartRequest);
		Assert.assertEquals(200, deleteItemCartResponse.getStatusCodeValue());

		// Verify Request Data
		Cart updatedCart = deleteItemCartResponse.getBody();
		assert updatedCart != null;
		Assert.assertEquals(new BigDecimal("0.00"), updatedCart.getTotal());
	}

}
