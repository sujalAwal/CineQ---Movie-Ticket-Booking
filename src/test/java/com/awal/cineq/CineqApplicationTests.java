package com.awal.cineq;

import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CineqApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void createNewUser() {
		User user = new User();
		user.setEmail("super@admin.com");
		user.setName("Super Admin");
		user.setPassword("Super@213");
		// Use roleId (reference to roles collection) instead of enum
		// Replace with actual roleId from your roles collection
		user.setRoleId("69765bae092751d1429dcbf0"); // Example: ADMIN role ObjectId
		System.out.println(userRepository.insert(user));
	}

}
