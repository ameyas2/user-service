use posts_application

CREATE TABLE public.users (
        id UUID NOT NULL,
        created_at TIMESTAMP(6) NULL,
        updated_at TIMESTAMP(6) NULL,
        first_name VARCHAR(255) NULL,
        last_name VARCHAR(255) NULL,
        username VARCHAR(255) NULL,
        CONSTRAINT users_pkey PRIMARY KEY (id ASC),
        UNIQUE INDEX users_username_key (username ASC)
)

select * from users

ALTER TABLE users
ADD profile_image_location TEXT default null;
