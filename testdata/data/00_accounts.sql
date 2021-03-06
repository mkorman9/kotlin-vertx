TRUNCATE TABLE public.accounts CASCADE;

COPY public.accounts_credentials (id, email, password_bcrypt, last_change_at, last_change_ip) FROM stdin;
1	admin@gogin.online	$2a$10$CYoenxLkI.J6uzP3oH2Iceh9Zm1n4XO51ngkSwm3Rk7BfmXawkWW2	2021-06-05 18:25:21.313229	127.0.0.1
\.

COPY public.accounts (id, username, roles, active, deleted, preferred_language, banned_until, created_at, credentials_id) FROM stdin;
d9e40989-4892-49bf-a247-bf6f0c82378a	admin	{CLIENTS_EDITOR,PERMISSIONS_ADMIN}	t	f	en-US	1970-01-01 00:00:00.00000	2021-06-05 18:25:20.42222	1
\.

SELECT setval('accounts_credentials_id_seq', (SELECT max(id) FROM public.accounts_credentials));
