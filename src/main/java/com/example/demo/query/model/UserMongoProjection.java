package com.example.demo.query.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_projections")
public class UserMongoProjection {
  @Id private Long id;

  private String name;
  private String email;
  private Long version;
}
