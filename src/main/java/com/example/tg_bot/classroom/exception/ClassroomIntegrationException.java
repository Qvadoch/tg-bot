package com.example.tg_bot.classroom.exception;

/**
 * Ошибка чтения Classroom, доступная обработчикам без зависимости от исключений Google SDK.
 */
public class ClassroomIntegrationException extends RuntimeException {
    /**
     * Создаёт ошибку интеграции с сохранением исходной причины.
     *
     * @param message описание неудавшейся операции
     * @param cause исходное исключение внешнего клиента
     */
    public ClassroomIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
