//модуль для облегчения работы с сокетами

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Phone implements Closeable {
    private final Socket socket; //сам сокет
    //ридер и райтер
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public int id; //уникальный итендификатор (для сервера)

    //информация о соединении (для сервера)
    public String connection;

    //закрыт ли сокет (для сервера)
    public boolean closed = false;

    //конструктор для клиента
    public Phone(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            this.reader = createReader();
            this.writer = createWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //конструктор для сервера
    public Phone(ServerSocket server) {
        try {
            this.socket = server.accept();//ожидание клиентов
            this.reader = createReader();//создание ридера
            this.writer = createWriter();//создание райтера
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //получение ip адреса в виде строки через сокет
    public String getIp() {
        return socket.getInetAddress().getHostAddress();
    }

    //отправка сообщения
    public void writeLine(String msg) throws IOException {
        if (!closed) {
            writer.write(msg);
            writer.newLine();
            writer.flush();
            System.out.println("Client with id " + id + " disconnected");
        }
    }

    //считывание сообщения
    public String readLine() throws IOException {
        if (!closed)
            return reader.readLine();
        return null;
    }

    //создание потока ввода
    private BufferedReader createReader() throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    //создание потока вывода
    private BufferedWriter createWriter() throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    //чтобы можно было использовать try-catch with resources
    @Override
    public void close() throws IOException {
        closed = true;
        writer.close();
        reader.close();
        socket.close();
    }

    //собственный equals (для addReplace и не только на сервере)
    public boolean equals(Object x) {
        if (x == null || x.getClass() != this.getClass())
            return false;
        if (x == this)
            return true;
        Phone cur = (Phone) x;
        return cur.socket == this.socket &&
                cur.id == this.id &&
                cur.getIp().equals(this.getIp());
    }
}
