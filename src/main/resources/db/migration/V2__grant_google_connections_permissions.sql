-- Роль tg_bot_app должна быть создана при первоначальной настройке окружения.
-- Повторение ранее выполненного вручную GRANT не создаёт конфликт прав.
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLE public.google_connections
TO tg_bot_app;
