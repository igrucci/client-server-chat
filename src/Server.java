import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

public class Server implements Runnable {

    // cписок подключений
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {

            server = new ServerSocket(8000);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client); // новый обработчик
                connections.add(handler);
                pool.execute(handler);
            }
            } catch(IOException e){
                shutdown();
            }
        }
// передача в эфир определенного сообщения/ транслирование его всем подключенным клиентам
        public void SendingToAllClients (String message){
            for (ConnectionHandler ch : connections) {
                if (ch != null) {
                    ch.sendMessage(message);
                }
            }
        }

    public void SendingToClientId(String message, String clientId, String nickname) {
        String formattedMessage = nickname + ": " + message;
        for (ConnectionHandler ch : connections) {
            if (ch != null && ch.getId().equals(clientId)) {
                ch.sendMessage(formattedMessage);
            }
        }
    }
        public void shutdown () {
            try {
                done = true;
                if (!server.isClosed()) {
                    server.close();
                }
                for (ConnectionHandler ch : connections) {
                    ch.shutdown();
                }
            } catch (IOException e) {
                //ignore

            }
        }

        //внутренний класс/ управляет подключением клиента/обрабатывает отдельные подключения
        class ConnectionHandler implements Runnable {
            private String id; // уникальный ид для каждого клиента
            private Socket client;
            // потоки
            private BufferedReader in; //устройсто считывания с буферизации; (от клиента)
            private PrintWriter out; // (клиенту)
            private String nickname;

            //обработчик общедоступных подключений
            public ConnectionHandler(Socket client) {
                this.client = client;
                Random random = new Random();
                int randomNumber = random.nextInt(1000); //
                this.id = String.valueOf(randomNumber);
             //   this.id = UUID.randomUUID().toString(); // генерация уникального id через uuid
            }

            public String getId() {
                return id;
            }
            @Override
            public void run() {
                try {
                    out = new PrintWriter(client.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));// буферизованное устр-во считывания

                    out.println(" /all (/all <message>)");
                    out.println(" /one (/one <id> <message>)");
                    out.println(" /nick (/nick <new nick>) ");
                    out.println(" /quit");
                    out.println("=======================================================================");

                    out.println("enter your name");
                    nickname = in.readLine();
                    //запись ника в файл
                    try {
                        File file = new File("clients.txt");
                        FileWriter writer = new FileWriter(file, true);

                        if (file.length() > 0) {
                            writer.write("\n");
                        }

                        writer.write(nickname);
                        writer.close();

                        System.out.println("nickname written to  file.");
                    } catch (IOException e) {
                        System.out.println("error.");
                        e.printStackTrace();
                    }


                    System.out.println(nickname + " connected with id " + id);

                    // сооющение в эфир что то сообщение появилось прямо сейчас
                    SendingToAllClients(nickname + " joined the chat, id ( " + id + " )");

                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("/nick")) {
                            String[] messageSplit = message.split(" ", 2);
                            if (messageSplit.length == 2) {
                                SendingToAllClients(nickname + "rename to" + messageSplit[1]);// cообщаем о смене имени
                                System.out.println(nickname + "rename to" + messageSplit[1]);
                                try {
                                    File file = new File("clients.txt");
                                    FileWriter writer = new FileWriter(file, true);

                                    if (file.length() > 0) {
                                        writer.write("\n");
                                    }

                                    writer.write( messageSplit[1] + "(old " + nickname+")");
                                    writer.close();

                                    System.out.println("new nickname written to  file.");
                                } catch (IOException e) {
                                    System.out.println("error.");
                                    e.printStackTrace();
                                }

                                nickname = messageSplit[1];
                                out.println("successfully changed nickname to " + nickname);

                            } else {
                                out.println("No nickname provided");
                            }
                        } else if (message.startsWith("/quit")) {
                            SendingToAllClients(nickname + " left the chat");
                            shutdown();
                        } else {

                            if(message.startsWith("/one")){


                                String[] messageSplit = message.split(" ", 3);
                                if (messageSplit.length == 3) {

                                   String clientId =messageSplit[1];
                                   message = messageSplit[2];
                                    SendingToClientId(message, clientId, nickname);;

                                } else {
                                    out.println("No id provided");
                                }

                            }
                            else if(message.startsWith("/all")){
                                String[] messageSplit = message.split(" ", 2);
                                if (messageSplit.length == 2) {
                                    message = messageSplit[1];
                                    SendingToAllClients(nickname + " : " + message);
                                } else {
                                    out.println("error");
                                }

                            }
                            else { // по дефолту отправляет всем
                                SendingToAllClients(nickname + " : " + message);
                            }

                        }
                    }
                } catch (IOException e) {
                    shutdown();
                }
            }

            public void sendMessage(String message) {
                out.println(message);
            }

            public void shutdown() {
                try {
                    in.close();
                    out.close();
                    if (!client.isClosed()) {
                        client.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    public static void main(String[] args) {
        Server server = new Server();
        server.run(); // запускает обработчик соединений
    }
    }
