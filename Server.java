import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

public class Server {
    public static void main(String[] args) throws IOException {

        boolean on = true;
        ServerSocket ss = new ServerSocket(1234);
        System.out.println(">>SERVIDOR LIGADO");

        String[] nomeLocais = { "PORTO", "LISBOA", "FARO" };
        MapaLocais mapaLocais = new MapaLocais(nomeLocais);
        MapaUtentes mapaUtentes = new MapaUtentes();
        mapaLocais.printMapaLocais(true, null);

        while (on) {
            Socket s = ss.accept();
            ClientHandler ch = new ClientHandler(s, mapaLocais, mapaUtentes);
            ch.start();
        }
        ss.close();
    }

}

class MapaUtentes {

    HashMap<Integer, Boolean> mapaUtentes;

    public MapaUtentes() {
        this.mapaUtentes = new HashMap<Integer, Boolean>();
        for (int i = 1; i < 21; i++) {
            mapaUtentes.put(i, false);
        }
    }

    public boolean utenteEntrou(int numeroSNS, DataOutputStream out) throws IOException {
        if (this.mapaUtentes.containsKey(numeroSNS)) {
            this.mapaUtentes.put(numeroSNS, true);
            return true;
        } else {
            out.writeUTF(">> ERRO: NUMERO SNS INVALIDO");
            out.flush();
            return false;
        }
    }
}

class ClientHandler extends Thread {

    private Socket clientSocket;
    private int numeroSNS;
    private DataInputStream in;
    private DataOutputStream out;
    private MapaLocais mapaLocais;
    private MapaUtentes mapaUtentes;

    public ClientHandler(Socket socket, MapaLocais mapaLocais, MapaUtentes mapaUtentes) throws IOException {
        this.clientSocket = socket;
        this.numeroSNS = -1;
        this.mapaLocais = mapaLocais;
        this.mapaUtentes = mapaUtentes;
        this.in = new DataInputStream(clientSocket.getInputStream());
        this.out = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            String cliResponse = null;
            String message;

            //INDICA AO CLIENTE QUE ESTA CONECTADO AO SERVIDOR E PEDE O CODIGO SNS 
            boolean valido = false;
            while (numeroSNS == -1 || !valido) {
                message = "Connectado ao Servidor\n>>Indique o seu número SNS:";
                sendMessage(message, this.out);
                cliResponse = in.readUTF(); // Le a mensagem do cliente
                System.out.println("SERVIDOR RECEBEU:" + cliResponse);
                try {
                    // Tenta transfomar a mensagem do cliente para inteiro
                    numeroSNS = Integer.parseInt(cliResponse);
                    valido = mapaUtentes.utenteEntrou(numeroSNS, out);
                    System.out.println("NUMERO SNS VALIDO? "+valido);
                } catch (NumberFormatException e) {
                    message = "ERRO - Indique novamente o seu número SNS:";
                    sendMessage(message, this.out);
                }
            }

            message = ">> OPCOES:\n-> Saber Locais: LOCAIS\n-> Marcar: MARCAR [LOCAL]\n-> Desmarcar: DESMARCAR\n-> Sair : SAIR";
            sendMessage(message, this.out);

            String[] tokens = { "" };
            String[] nomeLocais = { "PORTO", "LISBOA", "FARO" };

            //Ciclo que irá ler repetidamente as mensagens do cliente
            while ((cliResponse = in.readUTF()) != null) {
                message = "";
                System.out.println("SERVIDOR RECEBEU:" + cliResponse);
                try {
                    tokens = cliResponse.split(" ");
                } catch (Exception e) {
                }
                System.out.println(Arrays.toString(tokens) + " " + tokens.length);
                if (tokens.length == 1 && tokens[0].equals("LOCAIS")) {
                    message = "-> LOCAIS <-";
                    int i = 0;
                    for (String l : nomeLocais) {
                        message = message + "\n" + i + "-" + l;
                        i++;
                    }
                    sendMessage(message, out);
                } else if (tokens.length == 2 && tokens[0].equals("MARCAR")) {
                    try {
                        sendMessage(">> A MARCAR PARA " + tokens[1], out);
                        mapaLocais.addAgendamento(this.numeroSNS, tokens[1], this.out);
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
                } else if (tokens.length == 1 && tokens[0].equals("DESMARCAR")) {
                    try {
                        sendMessage(">> A DESMARCAR", out);
                        mapaLocais.removeAgendamento(this.numeroSNS, this.out);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    sendMessage(">> OPCAO INVALIDA", out);
                }
            }
            this.in.close();
            this.out.close();
            this.clientSocket.close();

        } catch (IOException e) {
            System.out.println(">> UTENTE " + this.numeroSNS + " SAIU DO SERVIDOR");
        }
    }

    private void sendMessage(String message, DataOutputStream out) throws IOException {
        out.writeUTF(message);
        out.flush();
    }
}