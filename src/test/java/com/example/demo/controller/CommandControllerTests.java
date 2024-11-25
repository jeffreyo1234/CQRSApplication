package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static util.ObjectMapperUtil.mapObject;

import com.example.demo.command.controller.UserCommandController;
import com.example.demo.command.model.User;
import com.example.demo.command.service.UserCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(UserCommandController.class)
public class CommandControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserCommandService userCommandService;

  private User mockUser;
  private String USER_JSON;
  private String UPDATED_USER_JSON;

  @BeforeEach
  void setUp() throws Exception {
    mockUser = new User(1L, "John Doe", "", 1L);
    ObjectMapper objectMapper = new ObjectMapper();
    USER_JSON = mapObject(Paths.get("src/test/resources/json/user.json").toString(), String.class);
    User updatedUser = new User(1L, "John Doe", "john.doe@example.com", 2L);
    UPDATED_USER_JSON = objectMapper.writeValueAsString(updatedUser);
  }

  @Nested
  public class GivenAValidRequest {

    @Nested
    class WhenTheRequestIsReceived {

      @Test
      void thenTheUserIsCreated() throws Exception {
        when(userCommandService.createUser(mockUser)).thenReturn(mockUser);

        MvcResult perform =
            mockMvc
                .perform(
                    post("/api/command/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(200, perform.getResponse().getStatus());
        assertEquals(USER_JSON, perform.getResponse().getContentAsString());
      }

      @Test
      void thenTheUserIsUpdated() throws Exception {
        User updatedUser = new User(1L, "John Doe", "john.doe@example.com", 2L);
        when(userCommandService.updateUser(updatedUser)).thenReturn(updatedUser);

        MvcResult perform =
            mockMvc
                .perform(
                    put("/api/command/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_USER_JSON))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(200, perform.getResponse().getStatus());
        assertEquals(UPDATED_USER_JSON, perform.getResponse().getContentAsString());
      }

      @Test
      void thenTheUserIsDeleted() throws Exception {
        Long userId = 1L;
        doNothing().when(userCommandService).deleteUser(userId);

        MvcResult perform =
            mockMvc
                .perform(
                    delete("/api/command/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1"))
                .andExpect(status().isNoContent())
                .andReturn();
        assertEquals(204, perform.getResponse().getStatus());
      }
    }
  }
}
