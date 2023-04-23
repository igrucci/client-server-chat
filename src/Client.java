import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// тут всего 2 потока работы. который получает все сообщения с сервера и который получает консольный линейный ввод данных
public class Client implements Runnable {

    private Socket client;
    private BufferedReader in; // для получиня инфы из входных данных
    private PrintWriter out; //
    private boolean done;

    public void run() {
        try {
            client = new Socket("127.0.0.1", 8000); // локальный хост
            out = new PrintWriter(client.getOutputStream(), true); // на выходе новый обьект принтвритера который получит выходной поток и значение тру
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler();
            // cоздаем поток, а не пул потоков тк это обработчик ввода
            Thread t = new Thread(inHandler);
            t.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null);
            System.out.println(inMessage);
        } catch (IOException e) {
            // ignore
        }
    }

    public void shutdown() {
        done = true;
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

    //обработчик ввода
    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    if (message.equals("/quit")) {
                        inReader.close();
                        shutdown();
                    } else{
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}


