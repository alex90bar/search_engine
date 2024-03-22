# SearchEngine

## Оглавление

- [Описание проекта](#1--)
- [Спецификация API](#2--api)
- [Стек используемых технологий](#3---)
- [Запуск проекта](#4--)

## 1. Описание проекта.
SearchEngine - это учебный проект на Java и SpringBoot.

Он представляет собой поисковый движок для осуществления индексации страниц веб-сайтов и дальнейшего поиска по проиндексированным страницам.

Веб-интерфейс (frontend-составляющая) проекта представляет собой одну веб-страницу с тремя вкладками:

### Dashboard. 
Эта вкладка открывается по умолчанию. На ней отображается общая статистика по всем сайтам, а также детальная статистика и статус по каждому из сайтов (статистика, получаемая по запросу /api/statistics).

<p align="center"><img  src="/readme_assets/dashboard.png" width="80%"></p>


### Management. 
На этой вкладке находятся инструменты управления поисковым движком — запуск и остановка полной индексации (переиндексации), а также возможность добавить (обновить) отдельную страницу по ссылке:

<p align="center"><img  src="/readme_assets/management.png" width="80%"></p>


### Search. 
Эта страница предназначена для тестирования поискового движка. На ней находится поле поиска, выпадающий список с выбором сайта для поиска, а при нажатии на кнопку «Найти» выводятся результаты поиска (по API-запросу /api/search):

<p align="center"><img  src="/readme_assets/search.png" width="100%"></p>


Вся информация на вкладки подгружается путём запросов к API приложения. При нажатии кнопок также отправляются запросы.


## 2. Спецификация API.

### Запуск полной индексации — GET /api/startIndexing

Метод запускает полную индексацию всех сайтов или полную переиндексацию, если они уже проиндексированы.
Если в настоящий момент индексация или переиндексация уже запущена, метод возвращает соответствующее сообщение об ошибке.

Метод без параметров

Формат ответа в случае успеха:

```json
{
"result": true
}
```

Формат ответа в случае ошибки:

```json
{
"result": false,
"error": "Индексация уже запущена"
}
```


### Остановка текущей индексации — GET /api/stopIndexing

Метод останавливает текущий процесс индексации (переиндексации). Если в настоящий момент индексация или переиндексация не происходит, метод возвращает соответствующее сообщение об ошибке.

Метод без параметров.

Формат ответа в случае успеха:

```json
{
"result": true
}
```

Формат ответа в случае ошибки:

```json
{
"result": false,
"error": "Индексация не запущена"
}
```


### Добавление или обновление отдельной страницы — POST /api/indexPage

Метод добавляет в индекс или обновляет отдельную страницу, адрес которой передан в параметре.
Если адрес страницы передан неверно, метод должен вернуть соответствующую ошибку.

Параметры:

`url` — адрес страницы, которую нужно переиндексировать.

Формат ответа в случае успеха:

```json
{
"result": true
}
```

Формат ответа в случае ошибки:

```json
{
"result": false,
"error": "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"
}
```


### Статистика — GET /api/statistics

Метод возвращает статистику и другую служебную информацию о состоянии поисковых индексов и самого движка.

Метод без параметров.

Формат ответа:

```json
{
  "result": true,
  "statistics": {
    "total": {
      "sites": 10,
      "pages": 436423,
      "lemmas": 5127891,
      "indexing": true
    },
    "detailed": [
      {
        "url": "http://www.site.com",
        "name": "Имя сайта",
        "status": "INDEXED",
        "statusTime": 1600160357,
        "error": "Ошибка индексации: главная страница сайта недоступна",
        "pages": 5764,
        "lemmas": 321115
      },
      ...
    ]
  }
}
```

### Получение данных по поисковому запросу — GET /api/search

Метод осуществляет поиск страниц по переданному поисковому запросу (параметр query).

Чтобы выводить результаты порционно, также можно задать параметры offset (сдвиг от начала списка результатов) и limit (количество результатов, которое необходимо вывести).

В ответе выводится общее количество результатов (count), не зависящее от значений параметров offset и limit, и массив data с результатами поиска. Каждый результат — это объект, содержащий свойства результата поиска (см. ниже структуру и описание каждого свойства).

Если поисковый запрос не задан или ещё нет готового индекса (сайт, по которому ищем, или все сайты сразу не проиндексированы), метод возвращает соответствующую ошибку.

Параметры:

`query` — поисковый запрос;

`site` — сайт, по которому осуществлять поиск (если не задан, поиск происходит по всем проиндексированным сайтам); задаётся в формате адреса, например: http://www.site.com (без слэша в конце);

`offset` — сдвиг от 0 для постраничного вывода (параметр необязательный; если не установлен, то значение по умолчанию равно нулю);

`limit` — количество результатов, которое необходимо вывести (параметр необязательный; если не установлен, то значение по умолчанию равно 20).

Формат ответа в случае успеха:

```json
{
  "result": true,
  "count": 574,
  "data": [
    {
      "site": "http://www.site.com",
      "siteName": "Имя сайта",
      "uri": "/path/to/page/6784",
      "title": "Заголовок страницы, которую выводим",
      "snippet": "Фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML",
      "relevance": 0.93362
    },
    ...
  ]
}
```
Формат ответа в случае ошибки:

```json
{
"result": false,
"error": "Задан пустой поисковый запрос"
}
```



## 3. Стек используемых технологий.
`Java 17` `Spring Boot` `REST API` `Spring Data' 'JDBC-JPA-Hibernate` `MySQL` `Multithreading` `Apache Lucene Morphology` `JSOUP` `Lombok` `Maven` `Thymeleaf`



## 4. Запуск проекта.

Для запуска необходимы: 

`JDK 17`
`docker-compose`

Из корня проекта выполняем команды в терминале:

для запуска окружения (БД):

```
docker-compose up -d
```

для запуска проекта:

```
mvn clean install
java -jar target/SearchEngine-1.0-SNAPSHOT.jar
```

Веб-интерфейс:  
http://localhost:8080/