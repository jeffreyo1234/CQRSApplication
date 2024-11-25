package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;

public class ObjectMapperUtil {
  // this is a class for mapping objects

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static <T> T mapObject(String filePath, Class<T> clazz) throws IOException {
    return objectMapper.readValue(Paths.get(filePath).toFile(), clazz);
  }
}
