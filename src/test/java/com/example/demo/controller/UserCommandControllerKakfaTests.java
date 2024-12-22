package com.example.demo.controller;

import com.example.demo.command.controller.UserCommandControllerKakfa;
import com.example.demo.command.model.User;
import com.example.demo.command.service.UserCommandServiceKafkaProducer;
import com.example.demo.config.WebSecurityTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCommandControllerKakfa.class)
@Import(WebSecurityTestConfiguration.class)
public class UserCommandControllerKakfaTests {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserCommandServiceKafkaProducer userCommandServiceKafkaProducer;

  private ObjectMapper objectMapper;
  private User mockUser;
  private String USER_JSON;
  private String UPDATED_USER_JSON;

  @BeforeEach
  void setUp() throws Exception {
    mockUser = new User(1L, "John Doe", "john.doe@example.com", 1L);
    objectMapper = new ObjectMapper();
    USER_JSON = new String(Files.readAllBytes(Paths.get("src/test/resources/user.json")));
    User updatedUser = new User(1L, "John Doe Updated", "john.doe.updated@example.com", 2L);
    UPDATED_USER_JSON = objectMapper.writeValueAsString(updatedUser);
  }

  @Nested
  class CreateUserTests {
    @Test
    @WithMockUser(roles = "USER")
    void testCreateUser() throws Exception {
      when(userCommandServiceKafkaProducer.createUser(any(User.class))).thenReturn(mockUser);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/command/users/kafka")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(USER_JSON))
              .andExpect(status().isOk())
              .andReturn();

      String actualResponseContent = result.getResponse().getContentAsString();
      assertEquals(200, result.getResponse().getStatus());
      User expectedUser = objectMapper.readValue(actualResponseContent, User.class);
      assertEquals(expectedUser, objectMapper.readValue(actualResponseContent, User.class));
    }
  }

  @Nested
  class UpdateUserTests {
    @Test
    @WithMockUser(roles = "USER")
    void testUpdateUser() throws Exception {
      User updatedUser = new User(1L, "John Doe Updated", "john.doe.updated@example.com", 2L);
      when(userCommandServiceKafkaProducer.updateUser(updatedUser.getId(), updatedUser))
          .thenReturn(updatedUser);

      MvcResult result =
          mockMvc
              .perform(
                  put("/api/command/users/kafka/{id}", updatedUser.getId())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(UPDATED_USER_JSON))
              .andExpect(status().isOk())
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());
      assertEquals(UPDATED_USER_JSON, result.getResponse().getContentAsString());
    }
  }

  @Nested
  class DeleteUserTests {
    @Test
    @WithMockUser(roles = "USER")
    void testDeleteUser() throws Exception {
      Long userId = 1L;
      doNothing().when(userCommandServiceKafkaProducer).deleteUser(userId);

      MvcResult result =
          mockMvc
              .perform(
                  delete("/api/command/users/kafka")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("1"))
              .andExpect(status().isNoContent())
              .andReturn();

      assertEquals(204, result.getResponse().getStatus());
    }
  }

  @Nested
  class HelloWorldTests {
    @Test
    @WithMockUser(roles = "USER")
    void testHelloWorld() throws Exception {
      MvcResult result =
          mockMvc
              .perform(get("/api/command/users/kafka/hello"))
              .andExpect(status().isOk())
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());
      assertEquals("Hello World", result.getResponse().getContentAsString());
    }
  }
}
