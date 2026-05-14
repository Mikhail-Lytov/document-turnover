# document-turnover

Desktop-приложение на JavaFX + Spring Boot.

## Требования

- JDK или JRE `25+` для запуска готового `.exe`.
- JDK `25` для сборки проекта.
- Доступ в интернет для скачивания Maven-зависимостей при первой сборке.

## Быстрый запуск для разработки

macOS/Linux:

```bash
./mvnw clean javafx:run
```

Windows:

```bat
mvnw.cmd clean javafx:run
```

## Сборка Windows EXE

Профиль `windows-exe` собирает "толстый" JAR со всеми зависимостями и заворачивает его в обычный `.exe` через Launch4j. Это не установщик: файл запускается напрямую, но на компьютере должна быть установлена Java `25+`.

Windows:

```bat
mvnw.cmd -Pwindows-exe -DskipTests clean package
```

Результат:

- `target\windows-exe\DocumentTurnover.exe` — готовый запускаемый файл.
- `target\document-turnover-1.0-SNAPSHOT-all.jar` — исполняемый JAR, который можно запустить командой ниже.

Запуск JAR напрямую:

```bat
java -jar target\document-turnover-1.0-SNAPSHOT-all.jar
```

## Полезные команды

Проверить версии инструментов:

```bash
java -version
./mvnw -v
```

Собрать с запуском тестов:

```bash
./mvnw -Pwindows-exe clean package
```

## Возможные проблемы

- `release version 25 not supported`  
  Используется JDK ниже 25. Установите JDK 25 и проверьте `java -version`.

- При запуске `.exe` появляется сообщение о Java  
  На компьютере нет подходящей Java. Установите JDK/JRE 25 или новее и убедитесь, что `java` доступна из `PATH`.
