package ru.ivakhramov.java.basic.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private Connection connection;

    public DatabaseManager(Connection connection) {

        this.connection = connection;
    }

    private static final String USERS_QUERY = "SELECT * FROM users";
    private static final String USER_ROLES_QUERY = "SELECT r.id AS \"id\", r.role AS \"role\" \n" +
            "FROM roles r\n" +
            "JOIN users_to_roles utr\n" +
            "ON r.id = utr.role_id\n" +
            "WHERE utr.user_id = ?";
    private static final String INSERT_USER_QUERY = "INSERT INTO users (login, password, name) VALUES (?, ?, ?)";
    private static final String INSERT_USER_TO_ROLE_QUERY = "INSERT INTO users_to_roles (user_id, role_id) VALUES (?, ?)";
    private static final String UPDATE_USER_NAME_QUERY = "UPDATE users SET name = ? WHERE id = ?";
    private static final String DELETE_USER_TO_ROLE_QUERY = "DELETE FROM users_to_roles WHERE user_id = ? AND role_id = ?";

    List<User> users = new ArrayList<>();

    private List<Role> getRolesToUser(User user) {

        List<Role> roles = new ArrayList<>();

        try (PreparedStatement getRolesToUser = connection.prepareStatement(USER_ROLES_QUERY)) {
            getRolesToUser.setInt(1, user.getId());
            try (ResultSet resultSet = getRolesToUser.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    EnumRole enumRole = EnumRole.valueOf(resultSet.getString(2).toUpperCase());
                    Role role = new Role(id, enumRole);
                    roles.add(role);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return roles;
    }

    public List<User> getUsers() {

        try (Statement getUsers = connection.createStatement()) {
            try (ResultSet resultSet = getUsers.executeQuery(USERS_QUERY)) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String login = resultSet.getString(2);
                    String password = resultSet.getString(3);
                    String name = resultSet.getString(4);
                    User user = new User(id, login, password, name, new ArrayList<>());
                    List<Role> roles = getRolesToUser(user);
                    user.setRoles(roles);
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    public void addUser(String login, String password, String name, EnumRole role) {

        try (PreparedStatement addUser = connection.prepareStatement(INSERT_USER_QUERY, Statement.RETURN_GENERATED_KEYS)) {

            addUser.setString(1, login);
            addUser.setString(2, password);
            addUser.setString(3, name);
            addUser.executeUpdate();

            ResultSet generatedKeys = addUser.getGeneratedKeys();
            if (generatedKeys.next()) {
                int userId = generatedKeys.getInt(1);
                try (PreparedStatement insertUserToRole = connection.prepareStatement(INSERT_USER_TO_ROLE_QUERY)) {
                    insertUserToRole.setInt(1, userId);
                    insertUserToRole.setInt(2, role == EnumRole.ADMIN ? 1 : 2);
                    insertUserToRole.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateName(int id, String name) {

        try (PreparedStatement updateName = connection.prepareStatement(UPDATE_USER_NAME_QUERY)) {
            updateName.setString(1, name);
            updateName.setInt(2, id);
            updateName.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addRole(int user_id, EnumRole enumRole) {

        try (PreparedStatement addRole = connection.prepareStatement(INSERT_USER_TO_ROLE_QUERY)) {
            addRole.setInt(1, user_id);
            addRole.setInt(2, enumRole == EnumRole.ADMIN ? 1 : 2);
            addRole.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRole(int user_id, EnumRole enumRole) {

        try (PreparedStatement deleteRole = connection.prepareStatement(DELETE_USER_TO_ROLE_QUERY)) {
            deleteRole.setInt(1, user_id);
            deleteRole.setInt(2, enumRole == EnumRole.ADMIN ? 1 : 2);
            deleteRole.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
