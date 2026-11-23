-- Почта может измениться; постоянным идентификатором аккаунта служит Google sub.
-- У старых записей проверенного sub ещё нет, поэтому столбец пока допускает NULL.
-- NOT NULL добавляется отдельной миграцией после заполнения реальными Google ID.
ALTER TABLE public.google_connections
ADD COLUMN google_subject VARCHAR(255);
