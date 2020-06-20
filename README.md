# Ftp proxy server

Прокси-сервер для FTP протокола в активном режиме. 
При попытке использования пассивного режима будет возвращен код ответа 502 (Command not implemented).
В сервере реализована возможность кэширования скачиваемых файлов.

## Запуск приложения

При запуске приложение читает необходимые параметры, такие как, максимальный размер кэша, путь до директории кэша, порт и адрес прокси сервера из файла *application.properties*.

При запуске создается объект типа [`ProxyDispatcher`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/ProxyDispatcher.kt) прослушивающий сокет, открытый на указанном порту и обрабатывающий подключения к серверу. Выполняет свою работу [`ProxyDispatcher`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/ProxyDispatcher.kt) в отдельном потоке

## Остановка приложения

Для корректной остановки потоков был реализован абстрактный класс [`AbstractProperlyStoppableThread`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/cuncurrent/AbstractProperlyStoppableThread.kt), который расширяет класс `Thread`.

В нем добавлен абстрактный метод `stopProperly`, который каждый класс-наследник реализует по-своему. В результате, вызов метода должен приводить к корректному завершению потока. 

Также есть метод `stoppedProperly`, переводящий переменную `isStoppedProperly` в значени true и возвращающий предыдущее ее значение. Он приватный и используется только внутри класса-наследника для проверки, не были ли действия приводящие к остановке потока выполнены ранее.

Чтобы остановить приложение нужно ввести команду *quit*, котрая правильно завершит выполнение всех потоков, созданных при выполнении программы.

## Обработка подключений

Для обработки подключений клиентов был реализован класс [`ProxyDispatcher`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/ProxyDispatcher.kt). Он прослушивает серверный сокет и при подключении клиента создает экземпляр класса [`ProxySession`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/ProxySession.kt), который реализует дальнейшую логику работы с клиентом. Наследуется от класса [`AbstractProperlyStoppableThread`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/cuncurrent/AbstractProperlyStoppableThread.kt).

## Парсинг команд

Для каждой ftp-команды, которую прокси-сервер должен обработать, был написан класс, хранящий параметры команды и саму команду. Все эти классы наследуются от класса [`Command`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/command/Command.kt). У этого класса есть метод `toCommandLine`, который преобразует команду в ее строчное представление, воспринимаемое сервером. Также в нем объявлены два абстрактных метода `takeToken` и `takeParams`, возвращающие команду и параметры соответственно, преобразованные в строку. Возвращаемые результаты этих методов используются в `toCommandLine`.

За парсинг команд отвечает [`CommandParser`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/command/CommandParser.kt), который для каждой полученной от клиента команды создает соответствующий экземпляр класса, унаследованного от [`Command`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/command/Command.kt). В конструкторе он принимает реализацию интерфейса [`CommandHandler`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/command/CommandHandler.kt), отвечающую за дальнейшую обработку команды. После парсинга у [`CommandHandler`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/command/CommandHandler.kt)-а вызывается метод `handle`, в который в качестве аргумента передается полученный объект.

## Парсинг ответов сервера

Обработка ответов сервера реализована аналогично обработке команд. 

Для каждого типа ответа реализован класс, унаследованный от [`Response`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/response/Response.kt), который хранит код ответа и сообщение.

За парсинг отвечает класс [`ResponseParser`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/response/ResponseParser.kt). В конструкторе он принимает экземпляр класса, унаследованного от [`ResponseHandler`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/response/ResponseHandler.kt)

## Пользовательские сессии

Основная логика приложения реализована в классе [`ProxySession`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/ProxySession.kt). Он реализует интерфейсы [`CommandHandler`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/command/CommandHandler.kt) и [`ResponseHandler`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/response/ResponseHandler.kt) и, следовательно, отвечает за обработку всех команд, полученных от пользователя, и ответов, полученных от сервера.

В этом классе хранится информация о пользователе, о скачиваемом файле, а также есть экземпляры соединения с клиентом [`ClientControlConnection`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/ClientControlConnection.kt), с сервером [`ServerControlConnection`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/ServerControlConnection.kt), запущенные в отдельных потоках, и экземпляр [`DataTransferManager`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/DataTransferManager.kt)-a, отвечающего за открытие соединений данных.

Рассмотрим как реализована обработка команды *RETR* (скачать файл). При получении этой команды прокси-сервер вместо того, чтобы сразу отправить эту команду серверу, отправляет команду *PWD* (получить путь к текущей директории). Это необходимо для того, чтобы построить полный путь к закэшированному файлу. Если этот файл есть в кэше, то он отправляется клиенту, а если нет - прокси-сервер устанавливает соединение с сервером для скачивания этого файла. При скачивании файла с сервера параллельно происходит его сохранение в кэше, если в кэше есть место, если нет - файл не кэшируется.

## Передача данных

За передачу данных отвечает класс [`DataTransfer`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/DataTransfer.kt). Он наследуется от класса [`AbstractProperlyStoppableThread`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/cuncurrent/AbstractProperlyStoppableThread.kt) и производит передачу данных в отдельном потоке. У него есть несколько наследников, реализующие разные сценарии передачи данных. [`DataTransfer.FromCache`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/DataTransfer.kt) - передача файла из кэша, [`DataTransfer.WithServer`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/DataTransfer.kt) - передача данных от сервера клиенту или от клиента серверу без кэширования, [`DataTransfer.WithServer.Caching`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/DataTransfer.kt) - передача данных от сервера клиенту с кэшированием.

Класс [`DataTransferManager`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/DataTransferManager.kt) используется для установки и разрывов соединений данных с одним конкретным сервером. При установке соединения он создает экземпляр [`DataTransfer`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/proxy/DataTransfer.kt), который в новом потоке начинает передачу данных.

## Устройство кэша

За кэширование фалов отвечает класс [`Cache`](https://github.com/KirillKhoroshkov/ftp-proxy-server/blob/master/src/main/kotlin/org/polykek/ftpproxyserver/cache/Cache.kt). В конструкторе он принимает путь до директории, в которую будут кэшироваться файлы и максимальный размер кэша.

Для кэширования файла используется метод `writeCachedFile`, который создает для каждого сервера свою директорию, именем которой является адрес сервера. И далее в этой папке создаются директории с полным путем к файлу и сам файл. В результате эта функция возвращает экземпляр [`OutputStream`](https://docs.oracle.com/javase/7/docs/api/java/io/OutputStream.html), открытый на только что созданном файле.

Для чтения файла из кэша нужно использовать метод `readCachedFile`, который открывает [`InputStream`](https://docs.oracle.com/javase/7/docs/api/java/io/InputStream.html) для уже закэшированного файла.

## Логгирование

Для логгирования использовалась библиотека [Log4j](https://logging.apache.org/log4j/2.x/). В логи выводятся сообщения об ошибках, информация, необходимая для отладки, а также все команды, поступившие от пользователей, и ответы полученные от серверов.
