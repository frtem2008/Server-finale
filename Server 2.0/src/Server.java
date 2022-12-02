//если этот код работает то его писал LiveFish, если нет, то example

//сервер для удалённого выполнения команд админами на компах клиентов
//просто демонстрационная версия
//клиенты уже переписываются на с++


//формат данных которые получает сервер от админа и клиента
//data = A$id на кого$command$args - админ
//data = C$id команды$id свой$результат - клиент

//формат данных которые отправляет сервер админу и клиенту
//data = A$id клиента$command$args$success - админ
//data = C$id команды$command$args - клиент

//библиотеки для работы с сокетами, файлами, списками данных

import java.io.*;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.*;

//основной класс сервера
public class Server {
    //для цветной консоли
    public static final String CONNECTIONCOLOR = "Green",
            DISCONNECTIONCOLOR = "Cyan",
            LOGCOLOR = "Normal",
            REGISTRATIONCOLOR = "Yellow",
            FILECREATINGCOLOR = "Blue",
            ERRORCOLOR = "Red",
            INVALIDDATACOLOR = "Purple";
    //ANSI для цвета
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final int SERVER_PORT = 26780;//порт сервера

    public static Scanner input = new Scanner(System.in);//для консоли сервера
    public static Set<Phone> phones = new HashSet<>();//все сокеты
    public static Set<Phone> adminPhones = new HashSet<>();//админские сокеты
    public static Set<Phone> clientPhones = new HashSet<>();//клиентские сокеты
    public static Set<Integer> allIds = new HashSet<>();//все уникальные id
    public static Set<Integer> activeIds = new HashSet<>();//все пользователи, активные сейчас
    public static Set<Integer> adminIds = new HashSet<>();//уникальные id админов
    public static Set<Integer> clientIds = new HashSet<>();//уникальные id клиентов
    //промежуточный список запросов, которые ещё выполняются
    public static Set<Request> tempRequests = new HashSet<>();
    //папка с логами для хранения логов
    public static File logFolder;
    //файл для определения пути к проекту
    public static File pathFile = new File("pathFile");
    //файлы для хранения запросов
    public static File mainRequestFile = new File("logFolder/fin req.txt");
    public static File commandIDsFile = new File("logFolder/commandIDs.txt");
    //файлы для хранения информации о пользователях
    public static File connectionsFile = new File("logFolder/connectionsFile.txt");
    public static File poweringFile = new File("logFolder/on or off.txt");
    //файл для запоминания информации о последнем id сообщения, выполненном каким-либо клиентом
    public static File idFile = new File("logFolder/id.txt");
    public static boolean COLOREDTEXT; //будет ли использоватся цветной вывод данных

    //печать цветного текста
    public static void printColored(String str, String color) {
        if (COLOREDTEXT)
            switch (color.toLowerCase(Locale.ROOT)) {
                case "red" -> System.out.println(ANSI_RED + str + ANSI_RESET);
                case "black" -> System.out.println(ANSI_BLACK + str + ANSI_RESET);
                case "green" -> System.out.println(ANSI_GREEN + str + ANSI_RESET);
                case "yellow" -> System.out.println(ANSI_YELLOW + str + ANSI_RESET);
                case "blue" -> System.out.println(ANSI_BLUE + str + ANSI_RESET);
                case "purple" -> System.out.println(ANSI_PURPLE + str + ANSI_RESET);
                case "cyan" -> System.out.println(ANSI_CYAN + str + ANSI_RESET);
                case "white" -> System.out.println(ANSI_WHITE + str + ANSI_RESET);
                case "normal" -> System.out.println(str);
            }
        else
            System.out.println(str);
    }

    //спрашивает пользователя о необходимости цветного вывода
    public static void initColors() {
        System.out.println("Do you want to use colored console? (true/false)");
        COLOREDTEXT = input.nextBoolean();
        input.nextLine();
    }

    //создание всех файлов сервера
    public static void createFiles() {
        try {
            //получение пути к файлам
            printColored("Attempting to create files\n", LOGCOLOR);
            printColored("Creating pathFile: " + pathFile.createNewFile(), FILECREATINGCOLOR);
            String testPath = pathFile.getAbsolutePath().replaceAll("pathFile", "");

            //создание log файла
            logFolder = new File(testPath + "/logFolder/");
            printColored("Creating logFolder folder: " + logFolder.mkdir() + "\n", FILECREATINGCOLOR);
            printColored("Logs folder path: " + logFolder.getAbsolutePath() + "\n", LOGCOLOR);

            //массив всех необходимых файлов (возможность для расширения в будующем)
            File[] files = {
                    mainRequestFile,
                    commandIDsFile,
                    connectionsFile,
                    poweringFile,
                    idFile
            };
            for (File cur : files) {
                if (!cur.exists())
                    if (cur.createNewFile())
                        printColored("Successfully created file " + cur.getName(), FILECREATINGCOLOR);
                    else
                        printColored("!Failed to create file " + cur.getName(), FILECREATINGCOLOR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //обновление всех активных id
    public static void refreshActiveIDs() {
        activeIds.clear();
        for (Phone phone : phones)
            activeIds.add(phone.id);
    }

    //чтение всех зарегистрированных до этого id из файла
    public static void fillArrays() {
        String ids = readFile(idFile);
        String[] idSplit = ids.split("\n");
        if (idSplit[0].equals("")) {
            printColored("No id input to parse", INVALIDDATACOLOR);
            return;
        }
        printColored("Ids read from file: ", LOGCOLOR);
        for (int i = 0; i < idSplit.length; i++) {
            allIds.add(Integer.parseInt(idSplit[i].trim()));
            //красивый вывод без запятой в конце
            if (i != idSplit.length - 1) {
                System.out.print(Integer.parseInt(idSplit[i].trim()) + ", ");
            } else {
                System.out.print(Integer.parseInt(idSplit[i].trim()));
            }
        }
        printColored("\n", LOGCOLOR);
    }

    //необходимо для уникального итендификатора каждого сообщения,
    //даже если сервер был перезагружен или выключен
    //читает последнее число из файла
    public static void setIdCount() {
        String ids = readFile(commandIDsFile);
        ArrayList<Integer> commandIds = new ArrayList<>();
        String[] idSplit = ids.split("\n");
        if (ids.trim().equals(""))
            Request.setRequestCount(1);
        else {
            for (String value : idSplit) commandIds.add(Integer.parseInt(value.trim()));
            if (commandIds.size() > 0)
                Request.setRequestCount(commandIds.get(commandIds.size() - 1));
            else
                Request.setRequestCount(1);
        }
    }

    public static void main(String[] args) {
        initColors(); //вопрос об использовании цвета в консоли
        createFiles(); //создание необходимых файлов
        setIdCount(); //задание уникального Id новым сообщениям
        fillArrays(); //заполнение списков информацией из файлов

        new Thread(Server::server).start();//запуск сервера

        new Thread(Server::serverConsole).start();//запуск консоли сервера
    }

    //консоль сервера
    public static void serverConsole() {
        String action; //пользовательский ввод

        while (true) {
            Phone failedPhone = null;
            try {
                action = input.nextLine(); //ввод данных

                switch (action) {
                    case "/shutdown" -> { //безопасное выключение сервера
                        printColored("Shutting down...", DISCONNECTIONCOLOR);
                        writeOnOff("Off");//запись в логи
                        for (Phone phone : phones) {
                            phone.writeLine("SYS$SHUTDOWN");
                            failedPhone = phone;
                        }
                        System.exit(0);
                    }
                    case "/connections" -> { //вывод списка всех активных подключений
                        if (phones.size() > 0) {
                            printColored("All active connections: ", LOGCOLOR);
                            phones.forEach(phone -> printColored(phone.connection, LOGCOLOR));
                        } else
                            printColored("No active connections", LOGCOLOR);
                    }
                    case "/idlist" -> {//вывод всех зарегистрированных id
                        printColored("All registrated IDs: ", LOGCOLOR);
                        phones.forEach(phone -> printColored(String.valueOf(phone), LOGCOLOR));
                    }
                    case "/help" -> { //вывод справки
                        printColored("___________________________________", "Cyan");
                        printColored("Help: \n", "Cyan");
                        printColored("""
                                /help to show this
                                /shutdown to shut the server down
                                /disconnect <int id> to disconnect a client from server
                                /connections to show all active connections
                                /idlist to show all registrated ids
                                /msg <int id> <String message> to send a message to the client
                                ___________________________________\040
                                """, "Cyan");
                    }
                    default -> {
                        //обработка остальных команд при помощи регулярных выражений
                        if (action.matches("/disconnect[ ]*\\d*[ ]*")) { // - /disconnect [int id]
                            if (action.split("/disconnect").length > 0) {
                                int idToDisconnect = Integer.parseInt(action.split("/disconnect ")[1]);
                                refreshActiveIDs();
                                if (activeIds.contains(idToDisconnect)) {
                                    getPhoneById(phones, idToDisconnect).writeLine("SYS$DISCONNECT");
                                    printColored("Disconnected client with id " + idToDisconnect + "\n", DISCONNECTIONCOLOR);
                                    writeConnection(Math.abs(idToDisconnect), 'd');
                                    phones.remove(getPhoneById(phones, idToDisconnect));
                                } else
                                    printColored("Client with id " + idToDisconnect + " isn't connected", INVALIDDATACOLOR);
                            } else {
                                for (Phone phone : phones) {
                                    phone.writeLine("SYS$DISCONNECT");
                                    writeConnection(Math.abs(phone.id), 'd');
                                }
                                printColored("Disconnected " + phones.size() + " clients (all)", DISCONNECTIONCOLOR);
                                phones.clear();
                            }
                        } else if (action.matches("/msg[ ]+[\\d]+[ ]+([\\w][ \\-=*$#]*)+")) { // - /msg <id> <text>
                            if (action.split("/msg").length > 0) {
                                int idToSend = Integer.parseInt(action.split(" ")[1]);
                                StringBuilder messageText = new StringBuilder();
                                for (int i = 2; i < action.split(" ").length; i++)
                                    messageText.append(action.split(" ")[i]);

                                refreshActiveIDs(); //обновление итендификаторов перед отправкой, иначе возможна отправка несуществующему клиенту
                                if (activeIds.contains(idToSend)) {
                                    getPhoneById(phones, idToSend).writeLine("SYS$MSG$" + messageText);
                                    printColored("Sent message " + messageText + " to client with id: " + idToSend, LOGCOLOR);
                                } else
                                    printColored("Client with id: " + idToSend + " isn't connected", INVALIDDATACOLOR);
                            }
                        } else {
                            //сообщение о неправильном вводе в консоль
                            printColored("Invalid command", INVALIDDATACOLOR);
                            printColored("Type /help to show all available commands", "Cyan");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                disconnectIfInactive(failedPhone, null);
            }
        }
    }

    //функция для выявления неактивных клиентов (нет таймаута, тк в данном случае отключение должно происходить мнгновенно)
    public static void disconnectIfInactive(Phone phone, Thread current) {
        if (phone == null || current == null) {
            printColored("Phone to disconnect: " + phone + ", current thread: " + current, INVALIDDATACOLOR);
            return;
        }
        try {
            printColored("\nClient with id " + phone.id + " disconnected\n", DISCONNECTIONCOLOR);
            writeConnection(Math.abs(phone.id), 'd'); //запись в логи
            phones.remove(phone);//удаление сокета из списка активных
            printColored("Interrupting client working thread", LOGCOLOR);
            phone.close();
            current.interrupt(); //остановка потока, обрабатывавшего этот сокет
            refreshActiveIDs();//обновление базы активных id при отключении
        } catch (IOException e) {
            e.printStackTrace();
            printColored("FAILED TO CLOSE PHONE: " + phone, ERRORCOLOR);
        }
    }

    //запись включения/выключения сервера в логи
    public static void writeOnOff(String onOff) {
        LocalDateTime now = LocalDateTime.now();
        String normalDate = dateNormalize(now);
        String toAppend = normalDate + "$" + onOff;
        appendStrToFile(poweringFile, toAppend);
    }

    //основной поток сервера
    public static void server() {
        try (ServerSocket server = new ServerSocket(SERVER_PORT)) { //запуск сервера
            printColored("Server started\n", "Yellow");
            writeOnOff("On");//запись в логи
            boolean run = true;
            while (run) {
                Phone phone = new Phone(server);//создание сокета сервера и ожидание присоединения клиентов
                new Thread(() -> {//каждый клиент в отдельном потоке
                    try {
                        //переменные для вывода и отправки информации
                        String data, root = "", command, args, success;

                        //итендификатор сообщения (отслеживание выполнения)
                        //uniId для регистрации и авторизации
                        //aUniId для отправки данных на id администратора
                        //cUniId для отправки данных на id клиента
                        int connectedClientId, adminId, cUniId;

                        boolean isActive;
                        phones.add(phone); //запись о сокетах
                        connectedClientId = login(phone);
                        //если логин прошёл успешно
                        if (connectedClientId != 0) {
                            activeIds.add(phone.id);
                            phone.connection = "Ip: " + phone.getIp() + " id: " + connectedClientId + " root: " + root;
                            printColored("Client connected: ip address is " + phone.getIp() + ", root is " + root + ", unique id is " + Math.abs(connectedClientId), CONNECTIONCOLOR);
                            phone.writeLine("Connected to server as " + root + " with unique id " + Math.abs(connectedClientId));
                            writeConnection(Math.abs(connectedClientId), 'c');
                        }
                        isActive = connectedClientId != 0;
                        //в бесконечном цикле обрабатываем данные клиента
                        while (isActive) {
                            //формат данных которые получает сервер от админа и клиента
                            //data = A$id на кого$command$args - админ
                            //data = C$id команды$id свой$результат - клиент

                            //формат данных которые отправляет сервер админу и клиенту
                            //data = A$id клиента$command$args$success - админ
                            //data = C$id команды$command$args - клиент

                            data = phone.readLine(); //считывание данных


                            if (data != null) {
                                String[] split = data.split("\\$");//чтобы каждый раз не сплитить строку

                                //сообщение о неверной команде
                                if (split.length == 0) {
                                    printColored("Received invalid data from client with id " + connectedClientId, INVALIDDATACOLOR);
                                    phone.writeLine("INVALID SYNTAX");
                                    continue;
                                }
                                root = split[0]; //информация об отправителе(админ/клиент)

                                if (root.trim().equals("A")) {
                                    //добавление информации об админе
                                    adminId = connectedClientId;

                                    //запись в списки о новом/старом вернувшимся юзере
                                    adminPhones.add(phone);
                                    adminIds.add(connectedClientId);

                                    if (!data.matches("A\\$[\\d]+\\$[\\w]+\\$[\\w]+([ ]+[\\w]+)*")) { //регулярка для обработки
                                        printColored("Received invalid data from client with id " + connectedClientId, INVALIDDATACOLOR);
                                        phone.writeLine("INVALID SYNTAX");
                                        continue;
                                    }
                                    //получение информации
                                    //TODO НОРМАЛЬНОЕ ПОЛНОЕ ПОЛУЧЕНИЕ ИНФОРМАЦИИ
                                    if (split[1].equals("getinfo"))
                                        getInfo(split[2], phone);
                                    else {
                                        printColored("___________________________________", LOGCOLOR);
                                        printColored("Admin data read: " + data, LOGCOLOR);
                                        //получение информации о клиенте
                                        cUniId = Integer.parseInt(split[1]);//уникальный id клиента
                                        if (cUniId == phone.id) { //проверка на отправку запроса самому себе
                                            printColored("Attempt to send request on itself on id: " + phone.id, INVALIDDATACOLOR);
                                            phone.writeLine("INVALID ID SENT");
                                            continue;
                                        }
                                        if (adminIds.contains(cUniId)) {//проверка на отправку запроса другому админу
                                            printColored("Attempt to send request to admin with id: " + cUniId, INVALIDDATACOLOR);
                                            phone.writeLine("INVALID ID SENT");
                                            continue;
                                        }

                                        printColored("Id to send: " + cUniId, LOGCOLOR);
                                        printColored("Id who sent: " + connectedClientId, LOGCOLOR);

                                        command = split[2];//сама команда
                                        printColored("Command to send: " + command, LOGCOLOR);

                                        args = split[3];//аргументы команды
                                        printColored("Args to send: " + args, LOGCOLOR);

                                        //id запроса выставляется автоматически прямо в конструкторе
                                        Request thisReq = new Request(adminId, cUniId, command, args);

                                        //добавление запроса в список запросов и в файл
                                        tempRequests.add(thisReq);

                                        //отправка информации нужному клиенту
                                        refreshActiveIDs();
                                        Phone toSend = getPhoneById(phones, cUniId);
                                        if (toSend != null) {
                                            if (allIds.contains(cUniId))
                                                toSend.writeLine(thisReq.id + "$" + command + "$" + args);
                                            else //ошибка отправки данных незарегистрированному клиенту
                                                printColored("Invalid command: this id is free", INVALIDDATACOLOR);
                                        } else { //ошибка отправки оффлайн клиенту
                                            printColored("Sending error: system didn't find an online client with id " + cUniId, ERRORCOLOR);
                                            phone.writeLine("Sending error: system didn't find an online client with id " + cUniId);
                                        }
                                    }
                                } else if (root.trim().equals("C")) { //добавление информации о клиенте
                                    printColored("___________________________________", LOGCOLOR);
                                    printColored("Client data read: " + data, LOGCOLOR);
                                    if (!data.matches("C\\$[\\d]+\\$[\\d]+\\$[\\w]+[ ]*[\\w]*")) {//регулярка для проверки данных, которые прислал клиент
                                        printColored("Received invalid data from client with id " + connectedClientId, INVALIDDATACOLOR);
                                        phone.writeLine("INVALID SYNTAX");
                                        continue;
                                    }

                                    int commandId = Integer.parseInt(split[1]); //id выполненной команды
                                    printColored("Command id: " + commandId, LOGCOLOR);

                                    //получение команды, которую выполнял клиент, по её id
                                    Request clientReq = getReqById(tempRequests, commandId);
                                    clientPhones.add(phone);
                                    //получение уникального итендификатора клиента
                                    cUniId = Integer.parseInt(split[2]);
                                    clientIds.add(connectedClientId);
                                    printColored("Client id to send: " + connectedClientId, LOGCOLOR);

                                    //получение id админа, отправившего команду
                                    adminId = clientReq.idA;
                                    adminIds.add(adminId);
                                    printColored("Admin id to send: " + adminId, LOGCOLOR);

                                    command = clientReq.cmd;//команда, которая была выполнена
                                    printColored("Command to send: " + command, LOGCOLOR);

                                    args = clientReq.args;//аргументы команды
                                    printColored("Args to send: " + args, LOGCOLOR);

                                    success = split[3];//успех выполнения (success/no success)
                                    printColored("Success to send: " + success, LOGCOLOR);

                                    //формирование ответа
                                    String response = cUniId + "$" + command + "$" + args + "$" + success;

                                    //обработка команды по id
                                    Request done = getReqById(tempRequests, commandId);
                                    if (done.equals(Request.ZEROREQUEST))
                                        printColored("Client " + cUniId + " wanted to write a zeroRequest", INVALIDDATACOLOR);

                                    //запись запроса в файл
                                    Request mainReq = new Request(done, success);
                                    writeRequest(mainReq);

                                    //удаление запроса из промежуточного списка
                                    tempRequests.remove(done);
                                    //отправка данных о клиенте админу с id aUniId
                                    Phone toSend = getPhoneById(phones, adminId);
                                    if (toSend != null) {
                                        if (allIds.contains(adminId))
                                            toSend.writeLine(response);
                                        else //ошибка отправки данных незарегистрированному администратору
                                            printColored("Invalid command: this id is free", INVALIDDATACOLOR);
                                    } else { //ошибка отправки данных оффлайн администратору
                                        printColored("Sending error: system didn't find an online admin with id " + adminId, ERRORCOLOR);
                                        phone.writeLine("\nSending error: system didn't find an online admin with id " + adminId);
                                    }
                                }
                                refreshActiveIDs(); //обновление итендификаторов
                                try {
                                    Thread.sleep(10);//пауза в запросах
                                } catch (InterruptedException e) {
                                    printColored("Failed to do Thread.sleep: thread " + Thread.currentThread().getName() + " is interrupted", ERRORCOLOR);
                                }
                            }
                        }
                    } catch (IOException e) {
                        disconnectIfInactive(phone, Thread.currentThread());
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (NullPointerException | IOException e) {
            printColored("Failed to start a server:\n_________________________", ERRORCOLOR);
            e.printStackTrace();
        }
    }

    //обработка запроса на получение информации админом
    //TODO доделать парсер по id
    public static void getInfo(String command, Phone phone) {
        //command - сообщение после getinfo
        new Thread(() -> {
            String toSend = "INFO$ERROR";
            switch (command.toLowerCase(Locale.ROOT)) {
                case "idall" -> toSend = "INFO$allIds" + activeIds.toString();
                case "idreg" -> toSend = "INFO$idReg" + allIds.toString();
                case "adminIds" -> toSend = "INFO$adminIds" + adminIds.toString();
                case "clientIds" -> toSend = "INFO$clientIds" + clientIds.toString();
                default -> {
                    if (command.matches("[\\d]+")) { //получение ip по id
                        int idToSend = Integer.parseInt(command);
                        Phone cur = getPhoneById(phones, idToSend);
                        if (cur != null)
                            toSend = "INFO$ID" + cur.getIp();
                        else
                            toSend = "INFO$ERROR$INVALIDID";
                    } else if (command.matches("[\\d]+[ ]+[\\w]+([ ]+[\\d]{2}.[\\d]{2}.[\\d]{4} [\\d]{2}.[\\d]{2}.[\\d]{4})*")) {
                        //TODO cmd information
                        //полученние информации о команде
                        ArrayList<String> commands = new ArrayList<>();
                        ArrayList<String> args = new ArrayList<>();
                        ArrayList<String> successes = new ArrayList<>();

                        int id = Integer.parseInt(command.split(" ")[0]);
                        String cmd = command.split(" ")[1];
                        String allCmd = readFile(mainRequestFile);
                        String[] lines = allCmd.split("\n");
                        for (String line : lines) {
                            commands.add(line.split("\\$")[3]);
                            args.add(line.split("\\$")[4]);
                            successes.add(line.split("\\$")[5]);
                        }
                    } else { //сообщение о неправильной команде
                        toSend = "INFO$ERROR$INVALIDSYNTAX";
                    }
                }

            }

            //отправка сообщения админу
            try {
                phone.writeLine(toSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    //получение Request из списка по уникальному итендификатору
    public static Request getReqById(Set<Request> reqList, long id) {
        List<Request> res = reqList.stream().filter(req -> req.id == id).toList();
        return res.size() == 0 ? Request.ZEROREQUEST : res.get(0);
    }

    //получение Phone из списка по уникальному итендификатору
    public static Phone getPhoneById(Set<Phone> phoneList, long id) {
        List<Phone> res = phoneList.stream().filter(phone -> phone.id == id).toList();
        return res.size() == 0 ? null : res.get(0);
    }

    //приведение LocalDateTime в формат дд-мм[час:мин:сек]
    public static String dateNormalize(LocalDateTime date) {
        String res;
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        int hours = date.getHour();
        int min = date.getMinute();
        int sec = date.getSecond();
        res = day + "-" + month + "[" + hours + ":" + min + ":" + sec + "]";
        return res;
    }

    //запись запросов в файл в зависимости от содержания
    public static void writeRequest(Request req) {
        String writeReq;
        LocalDateTime now = LocalDateTime.now();

        String dateToWrite = dateNormalize(now);
        if (req.equals(Request.ZEROREQUEST)) {
            printColored("A try to write a zero request into file", INVALIDDATACOLOR);
        } else { //запись в файл с завершёнными запросами
            writeReq = dateToWrite + "$" + req.idA + "$" + req.idC + "$" + req.cmd + "$" + req.args + "$" + req.success;
            appendStrToFile(mainRequestFile, writeReq);
        }
    }

    //запись в файл о подключении пользователя
    public static void writeConnection(int id, char cd) {
        LocalDateTime now = LocalDateTime.now();
        String normalDate = dateNormalize(now);
        String toAppend = normalDate + "$" + id + "$" + cd;
        appendStrToFile(connectionsFile, toAppend);
    }

    //функции для работы с файлами
    public static void appendStrToFile(File file, String str) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
            out.write(str + "\n");
            out.close();
        } catch (IOException e) {
            printColored("exception occurred" + e, ERRORCOLOR);
        }
    }

    public static void clearFile(String fileName) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, false));
            out.write("");
            out.close();
        } catch (IOException e) {
            printColored("exception occurred" + e, ERRORCOLOR);
        }
    }

    public static String readFile(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file.getPath()))) {
            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append(System.lineSeparator());
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    //обновление id последнего сообщения (для уникальности id всех сообщений)
    public static void updateIdCommandsFile(Request req) {
        clearFile(commandIDsFile.getPath());
        appendStrToFile(commandIDsFile, String.valueOf(req.id));
    }

    private static Object find(Object source, HashSet<Object> set) {
        if (set.contains(source))
            for (Object obj : set)
                if (obj.equals(source))
                    return obj;

        return null;
    }

    public static int login(Phone phone) {
        //отправка инфы о подключении клиенту
        boolean loginFailed = true;
        //пароли и тд будут позже
        String dataSent, root;
        int uniId = 0;
        //цикл входа / регистрации
        do {//пока клиент не зарегается или не войдёт
            try {
                refreshActiveIDs();
                dataSent = phone.readLine(); //чтение id клиента
                root = dataSent.split("\\$")[0]; //Admin or Client + id
                uniId = Integer.parseInt(dataSent.split("\\$")[1]);

                //если id отрицательный, то регистрируем пользователя
                if (uniId <= 0) {
                    if (allIds.contains(-uniId)) {
                        printColored("The user with id " + (-uniId) + " already exists", INVALIDDATACOLOR);
                        phone.writeLine("Invalid id: user with id " + (-uniId) + " already exists");
                        continue;
                    }

                    //лол Idea не знает слова registrated
                    String register = "Successfully registrated new user with root " + root + " and id: " + (-uniId);

                    appendStrToFile(idFile, String.valueOf(-uniId));//добавление id в список зарегистрированных
                    printColored(register, REGISTRATIONCOLOR);

                    phone.writeLine("Registration success");
                    phone.id = -uniId;
                    allIds.add(Math.abs(uniId)); //добавление нашего id в список
                    break; //окончание цикла входа/регистрации
                } else { //иначе пытаемся войти
                    if (allIds.contains(uniId)) {
                        if (!activeIds.contains(uniId)) { //id есть в списке, но нет онлайн
                            loginFailed = false;
                            phone.writeLine("Login success");
                            phone.id = uniId;
                        } else { //id есть в списке и есть онлайн
                            printColored("Failed to login a user with id " + uniId + ": user with this id has already logged in", INVALIDDATACOLOR);
                            phone.writeLine("Login failed: user with id " + uniId + " has already logged in");
                        }
                    } else { //ошибка логина: такого логина пока нет
                        printColored("Failed to login a user with id " + uniId + ": this id is free", INVALIDDATACOLOR);
                        phone.writeLine("Login failed");
                        loginFailed = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                disconnectIfInactive(phone, Thread.currentThread());
            }
        } while (loginFailed);

        return Math.abs(uniId);
    }
}


//класс для хранения запросов
//нужен для хранения всех команд на сервере и обращения к ним по id

class Request {
    //специальный "нулевой" запрос
    public static final Request ZEROREQUEST = new Request(0, 0, "0", "0");

    static long requestCount = 1;//количество запросов

    public String cmd, args, success;
    public int idA, idC;
    long id;//уникальный итендификатор

    //конструктор для временного хранения
    public Request(int idA, int idC, String cmd, String args) {
        this.cmd = cmd;
        this.args = args;
        this.success = "NaN";
        this.idC = idC;
        this.idA = idA;
        requestCount++;
        this.id = requestCount;
    }

    //конструктор для постоянного хранения
    public Request(Request what, String success) {
        this.idA = what.idA;
        this.idC = what.idC;
        this.cmd = what.cmd;
        this.args = what.args;
        this.id = what.id;

        this.success = success;
        Server.updateIdCommandsFile(this);
    }

    public static void setRequestCount(int c) {
        Server.printColored("Request count set to " + c + "\n", Server.LOGCOLOR);
        requestCount = c;
    }
}