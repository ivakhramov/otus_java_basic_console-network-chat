--Набор DDL скриптов, создающих БД

CREATE DATABASE "otus_java_basic_console-network-chat";

CREATE TABLE public.users
(
    id       serial,
    login    varchar(100) UNIQUE NOT NULL,
    password varchar(100) NOT NULL,
    name     varchar(100) UNIQUE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE public.roles
(
    id   serial,
    role varchar(100) UNIQUE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE public.users_to_roles
(
    user_id integer,
    role_id integer,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES public.users (id),
    CONSTRAINT role_fk FOREIGN KEY (role_id) REFERENCES public.roles (id)
);

INSERT INTO public.users (login, password, name)
VALUES ('admin', 'admin', 'default_admin'),
       ('user', 'user', 'default_user');

INSERT INTO public.roles (role)
VALUES ('ADMIN'),
       ('USER');

INSERT INTO public.users_to_roles (user_id, role_id)
VALUES (1, 1),
       (1, 2),
       (2, 2);

SELECT login, password, name
FROM users u
         JOIN users_to_roles utr on u.id = utr.user_id
         JOIN roles r on r.id = utr.role_id
WHERE r.role = 'ADMIN';