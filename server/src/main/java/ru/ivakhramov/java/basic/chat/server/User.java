package ru.ivakhramov.java.basic.chat.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class User {

    private int id;
    private String login;
    private String password;
    private String name;

    private List<Role> roles = new ArrayList<>();

    public User(int id, String login, String password, String name, List<Role> roles) {

        this.id = id;
        this.login = login;
        this.password = password;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public void addRoleToRoles(EnumRole role) {
        this.roles.add(new Role(role == EnumRole.ADMIN ? 1 : 2, role));
    }

    public void removeRoleFromRoles(EnumRole enumRole) {
        Iterator<Role> iterator = roles.iterator();
        while (iterator.hasNext()) {
            Role role = iterator.next();
                if (role.getRole().equals(enumRole)) {
                    iterator.remove();
                    break;
                }
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", roles=" + roles +
                '}';
    }
}
