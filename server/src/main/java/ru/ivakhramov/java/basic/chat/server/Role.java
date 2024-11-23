package ru.ivakhramov.java.basic.chat.server;

public class Role {
    private int id;
    private EnumRole role;

    public Role(int id, EnumRole role) {
        this.id = id;
        this.role = role;
    }

    public EnumRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", role='" + role + '\'' +
                '}';
    }
}
