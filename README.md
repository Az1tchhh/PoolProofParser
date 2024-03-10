# Introduction
Привет, меня зовут Азамат. Это моё тестовое задание для настройки и работы с сервисами Django и Spring.

# Обязательные вещи:
1. Django
2. Spring
3. Python dependencies

Вы можете установить необходимые зависимости Python, выполнив следующую команду:

```bash
Copy code
pip install -r requirements.txt
```
# Как запускать сервисы:
## 1) Docker Compose
Настоятельно рекомендую использовать Docker Compose, так как это упрощает запуск сервисов.
Я написал файл docker-compose.yml в корневой папке репозитория. Запустив его с помощью docker-compose up, вы сможете запустить оба сервера.
p.s. Я загрузил образы в Docker Hub, чтобы у вас не было проблем с сборкой проекта. Также я включил в образ мой файл сессии от Telegram. Я знаю, что это не самый хороший подход, но не нашёл другого способа, кроме как отдельно запускать оба сервера (без Docker).

## 2) Отдельный запуск сервисов
Для запуска Spring-сервиса вам нужно изменить файл application.properties, указав свою локальную базу данных. Также измените fetcherUrl=http://django:8000/api/fetch/ на fetcherUrl=http://localhost:8000/api/fetch/.

# Эндпоинты и коллекция Postman:
Сервис на Django используется для подключения к сессии Telegram, чтобы в дальнейшем следить за обновлениями в группе PoolProof. Сервис на Spring отправляет запрос на API http://localhost:8000/api/fetch/ каждую минуту, чтобы получать обновления (новые сообщения). Вы можете использовать эндпоинт http://localhost:8080/api/fetch_channel, чтобы отправить запрос на получение новых сообщений, но это не требуется, так как в Spring-приложении есть Scheduler, который делает это автоматически.
В коллекции Postman также есть три дополнительных запроса для просмотра данных:

**Get Messages by Date**

**Get all Messages**

**Get last n Messages**

Остальные два запроса - Fetch Messages (Auto, Periodic) и Subscribe for updates - автоматизированы.

```  
    @Async
    public CompletableFuture<ResponseEntity<?>> fetchChannel() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResponseEntity<List<Message>> response;
                try {
                    response = HttpUtils.sendGetRequest(fectherUrl);
                    if(response.getStatusCode().is2xxSuccessful()){
                        Long allRows = messageRepository.countAllMessages();
                        if(allRows == 0){
                            messageRepository.saveAll(Objects.requireNonNull(response.getBody()));
                            SuccessMessage successMessage = new SuccessMessage("Messages were saved into database");
                            return new ResponseEntity<>(successMessage, HttpStatus.OK);
                        }
                        else if(Objects.requireNonNull(response.getBody()).size() == allRows){
                            SuccessMessage successMessage = new SuccessMessage("No new messages yet");
                            return new ResponseEntity<>(successMessage, HttpStatus.OK);
                        }
                        else {
                            Message message = response.getBody().get((int) (allRows-1));
                            messageRepository.save(message);
                            SuccessMessage successMessage = new SuccessMessage("New message save into database");
                            return new ResponseEntity<>(successMessage, HttpStatus.OK);
                        }
                    }
                    else{
                        ErrorMessage errorMessageResponse = new ErrorMessage("Could bot request python server");
                        return new ResponseEntity<>(errorMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } catch (Exception e) {
                    ErrorMessage errorMessageResponse = new ErrorMessage("Connection between services corrupted");
                    return new ResponseEntity<>(errorMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } catch (Exception e) {
                ErrorMessage errorMessage = new ErrorMessage("Operation failed");
                return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }
    ...
    @Override
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void fetchChannelPeriodically() {
        CompletableFuture<ResponseEntity<?>> future = fetchChannel();
        future.thenAccept(responseEntity -> {
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                System.out.println("Fetch and save operation successful: " + responseEntity.getBody());
            } else {
                System.err.println("Error during fetch and save operation: " + responseEntity.getBody());
            }
        });
    }
...
LOGS (spring-container):
2024-03-11 00:55:18 Fetch and save operation successful: SuccessMessage(successMessage=Messages were saved into database)
2024-03-11 00:56:17 Fetch and save operation successful: SuccessMessage(successMessage=No new messages yet)
...
LOGS (django-container):
2024-03-11 00:55:18 [10/Mar/2024 19:55:18] "GET /api/fetch/ HTTP/1.1" 200 109648
2024-03-11 00:56:17 [10/Mar/2024 19:56:17] "GET /api/fetch/ HTTP/1.1" 200 109648
```

# Как это работает:
## Есть два сервиса: один следит за обновлениями в Telegram-канале, а второй прослушивает эти обновления. Если что-то изменится, второй сервис сохраняет эти изменения в базу данных. Вот и всё :)

# Где смотреть логи:
Если вы запустили проект через докер, то можете смотреть логи в самом контейнере. А если в ручную, то в терминале.
