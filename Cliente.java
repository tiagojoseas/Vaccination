import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Cliente {

    public static void main(String[] args) {
        try {

            Socket socket = new Socket("127.0.0.1", 1234);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            /*
             * A Thread listenServer é uma thread repsonsavel por ler e mostar ao cliente
             * todas mensagens/respostas vindas do servidor
             */
            Thread listenServer = new Thread(new Runnable() {
                String serverResponse;

                @Override
                public void run() {
                    // Variavel isOn serve para indicar qd a conexao a servidor continua
                    // estabelecida
                    boolean isOn = true;
                    while (isOn) {
                        try {
                            // Lemos a reposta do servidor e só imprimimos caso seja diferente de null
                            if ((serverResponse = in.readUTF()) != null) {
                                System.out.println(serverResponse);
                            }
                        } catch (IOException e) {
                            isOn = false;
                            System.out.println(">> ERRO: LIGACAO AO SERVIDOR FOI INTERROMPIDA");
                        }
                    }
                }
            });

            // Colocamos a Thread a "correr"
            listenServer.start();

            String userWrote = "";
            // Ciclo responsável por ler os inputs do cliente e envia-los para o servidor
            while (!userWrote.equals("SAIR")) {
                userWrote = userInput.readLine();
                out.writeUTF(userWrote);
                out.flush();
            }

            out.close();
            in.close();
            socket.close();

        } catch (Exception e) {
        }

    }

}