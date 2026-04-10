# document-turnover

Desktop-приложение на JavaFX + Spring Boot.

## Требования

- JDK `25` (в `pom.xml` задано `<java.version>25</java.version>`)
- Доступ в интернет для скачивания Maven-зависимостей при первой сборке
- Для сборки `.exe`: Windows + `jpackage` (входит в JDK 25)
- Опционально для некоторых конфигураций `jpackage` на Windows может понадобиться WiX Toolset

## Быстрый запуск (dev)

macOS/Linux:

```bash
./mvnw clean javafx:run
```

Windows:

```bat
mvnw.cmd clean javafx:run
```

## Сборка JAR

Сборка "толстого" JAR (со всеми зависимостями):

macOS/Linux:

```bash
./mvnw -Pwindows-exe -Dexec.skip=true -DskipTests clean package
```

Windows:

```bat
mvnw.cmd -Pwindows-exe -Dexec.skip=true -DskipTests clean package
```

Результат:

- `target/document-turnover-1.0-SNAPSHOT-all.jar` — исполняемый JAR
- `target/document-turnover-1.0-SNAPSHOT.jar` — обычный JAR без зависимостей

Запуск исполняемого JAR:

```bash
java -jar target/document-turnover-1.0-SNAPSHOT-all.jar
```

## Сборка Windows EXE

Профиль `windows-exe` уже настроен в `pom.xml` и вызывает `jpackage` с типом `exe`.

Запускать на Windows:

```bat
mvnw.cmd -Pwindows-exe -DskipTests clean package
```

Где искать результат:

- `target\installer\` — каталог с готовым `.exe`-инсталлятором

Имя приложения берётся из `pom.xml`:

- `<app.name>DocumentTurnover</app.name>`

## Полезные команды

Проверить версии инструментов:

```bash
java -version
jpackage --version
./mvnw -v
```

Собрать с запуском тестов:

```bash
./mvnw -Pwindows-exe clean package
```

## Возможные проблемы

- `release version 25 not supported`  
  Используется JDK ниже 25. Установите JDK 25 и проверьте `java -version`.

- `jpackage: command not found`  
  Используется JRE или неполный JDK. Нужен полноценный JDK 25 с `jpackage`.

- Ошибки при создании `.exe` на Windows  
  Проверьте, что сборка запускается именно на Windows и при необходимости установите WiX Toolset.
