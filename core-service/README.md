# core-service

Минимальный Kotlin-сервис, сгенерированный по `src/main/resources/openapi-coreService.yml`.

## Что здесь есть

- OpenAPI-first генерация через `kotlin-spring`
- Сгенерированные интерфейсы и DTO в `build/generated/src/main/kotlin`
- Мок-реализация всех ручек в `CoreApiController`
- Swagger UI

## Запуск

```powershell
.\gradlew.bat :core-service:bootRun --no-daemon
```

## Проверка

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Пример ручки: `GET http://localhost:8081/coins`

## Сборка

```powershell
.\gradlew.bat :core-service:build --no-daemon
```

