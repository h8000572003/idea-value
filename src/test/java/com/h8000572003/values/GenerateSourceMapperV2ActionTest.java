package com.h8000572003.values;

public class GenerateSourceMapperV2ActionTest extends JavaTestCase {

    private static final String TITLE = "Generated set/get based on parameter 1 return value";

    public void testCreatesReturnValueFromParameter() {
        launch(TITLE, """
                class UserDto {
                    public void setName(String name) {}
                    public String getName() { return null; }
                    public void setAge(int age) {}
                }
                class User {
                    public String getName() { return null; }
                    public int getAge() { return 0; }
                }
                class Mapper {
                    UserDto toDto(User user) {
                        <caret>
                    }
                }
                """);

        assertCode("""
                class UserDto {
                    public void setName(String name) {}
                    public String getName() { return null; }
                    public void setAge(int age) {}
                }
                class User {
                    public String getName() { return null; }
                    public int getAge() { return 0; }
                }
                class Mapper {
                    UserDto toDto(User user) {
                        UserDto userDto = new UserDto();
                        userDto.setName(user.getName());
                        userDto.setAge(user.getAge());
                        return userDto;
                    }
                }
                """);
    }

    public void testNotAvailableForPrimitiveReturn() {
        assertNotAvailable(TITLE, """
                class User {
                    public int getAge() { return 0; }
                }
                class Mapper {
                    int age(User user) {
                        <caret>
                    }
                }
                """);
    }

    public void testNotAvailableForVoid() {
        assertNotAvailable(TITLE, """
                class User {
                    public int getAge() { return 0; }
                }
                class Mapper {
                    void age(User user) {
                        <caret>
                    }
                }
                """);
    }
}
