-- Диагностика: выполнять в базе tg_bot через прямое подключение под tg_bot_app.
-- Файл не является миграцией и не меняет данные или права.
-- Ожидаются current_user = tg_bot_app и current_database = tg_bot.
SELECT current_user, current_database();

-- Показывает количество строк, не выводя почту и токены.
-- Для пустой таблицы результат 0 является нормальным.
SELECT COUNT(*) FROM public.google_connections;

-- Все четыре значения должны быть true.
-- Проверяет наличие прав, но не выполняет INSERT, UPDATE или DELETE.
SELECT
    has_table_privilege(current_user, 'public.google_connections', 'SELECT') AS can_select,
    has_table_privilege(current_user, 'public.google_connections', 'INSERT') AS can_insert,
    has_table_privilege(current_user, 'public.google_connections', 'UPDATE') AS can_update,
    has_table_privilege(current_user, 'public.google_connections', 'DELETE') AS can_delete;
