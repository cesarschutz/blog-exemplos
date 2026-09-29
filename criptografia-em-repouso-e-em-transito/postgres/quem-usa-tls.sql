-- A consulta do artigo: quem está conectado com TLS
-- (com o usuário mestre ou um usuário com pg_monitor; um usuário comum só vê as próprias sessões).
SELECT a.usename, a.client_addr, s.ssl, s.version
FROM pg_stat_ssl s
JOIN pg_stat_activity a ON a.pid = s.pid
WHERE a.backend_type = 'client backend';
