import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

class Agendamento {

    private int numeroSNS;
    private String local;
    private DataOutputStream out;
    private int hora;

    public Agendamento(int numeroSNS, String local, DataOutputStream out) {
        this.numeroSNS = numeroSNS;
        this.local = local;
        this.out = out;
    }

    public int getNumeroSNS() {
        return this.numeroSNS;
    }

    public String getLocal() {
        return this.local;
    }

    public DataOutputStream getOut() {
        return this.out;
    }

    public int getHora() {
        return this.hora;
    }

    public void setHora(int hora) {
        this.hora = hora;
    }

}

class Local {

    private String nome;
    private HashMap<Integer, Agendamento> agendamentosLocal;
    private int numeroVagas;
    private int numeroAgendamentos;
    private int numeroEspera;
    private ReentrantLock lock;
    private Condition cond;

    public Local(String nome) {
        this.nome = nome;
        this.agendamentosLocal = new HashMap<>();
        this.numeroVagas = new Random().nextInt(5) + 2; //sera colocado um numero aleatório de vagas entre 2 a 7
        this.numeroAgendamentos = 0;
        this.numeroEspera = 0;
        this.lock = new ReentrantLock();
        this.cond = this.lock.newCondition();
    }

    public HashMap<Integer, Agendamento> getAgendamentosLocal() {
        return this.agendamentosLocal;
    }

    public void addAgendamento(Agendamento newAge) throws IOException, InterruptedException {
        this.lock.lock();
        try {
            /*
             * Comparar o numero de agendamentos a numero de vagas existentes para o local:
             * 1 - Se um numero de agendamentos for igual ao numero de vagas significa que o
             * cliente tera de aguardar por alguma desmarcacao de outro utente para o
             * sistema poder marcar a a vacinacao do utente.
             * 
             * 2 - Se Se um numero de agendamentos for difente ao numero de vagas significa
             * que o cliente podera efetuar o agendamento da sua vaciancao, sendo o sistema
             * responsavel pela escolha da hora
             * 
             * NOTA: Para efeitos de simplicacao do projeto reduziu-se o horário em relacao
             * ao mundo rea
             */
            if (this.numeroAgendamentos == this.numeroVagas) {

                newAge.getOut().writeUTF(">>NAO TEM VAGAS - AGUARDE");
                newAge.getOut().flush();

                while (this.numeroAgendamentos == this.numeroVagas) {
                    System.out.println(this.numeroAgendamentos + " - " + this.numeroVagas);
                    numeroEspera++;
                    this.printAgendamento();
                    this.cond.await();
                }
                numeroEspera--;
            }

            /*
             * Ciclo responsavel por encontrar uma hora para a toma da vacina
             */
            int horas = 8 + this.numeroVagas;
            boolean encontrouVaga;
            for (int h = 8; h < horas; h++) {
                encontrouVaga = true;
                for (Agendamento age : this.agendamentosLocal.values()) {
                    if (age.getHora() == h) {
                        encontrouVaga = false;
                    }
                }

                if (encontrouVaga) {
                    newAge.setHora(h);
                    break;
                }
            }

            this.numeroAgendamentos++;
            this.agendamentosLocal.put(newAge.getNumeroSNS(), newAge);
            newAge.getOut().writeUTF(">>FOI AGENDADO PARA AS " + newAge.getHora() + " HORAS EM " + this.nome);
            newAge.getOut().flush();
        } finally {
            this.printAgendamento();
            this.printLocal();
            this.lock.unlock();
        }
    }

    public void removeAgendamento(Agendamento age) throws IOException, InterruptedException {
        this.lock.lock();
        try {
            /*
             * Comapar o numero de agendamentos a numero de vagas existentes para o local: 1
             * - Se um numero de agendamentos for igual ao numero de vagas significa que o
             * cliente tera de aguardar por alguma desmarcacao de outro utente para o
             * sistema poder marcar a a vacinacao do utente.
             * 
             * 2 - Se Se um numero de agendamentos for difente ao numero de vagas significa
             * que o cliente podera efetuar o agendamento da sua vaciancao, sendo o sistema
             * responsavel pela escolha da hora
             * 
             * NOTA: Para efeitos de simplicacao do projeto
             */
            this.agendamentosLocal.remove(age.getNumeroSNS());
            this.numeroAgendamentos--;
            if (this.numeroAgendamentos < this.numeroVagas) {
                this.cond.signalAll();
            }
            age.getOut().writeUTF(">>FOI DESMARCADO");
            age.getOut().flush();
        } finally {
            this.printAgendamento();
            this.printLocal();
            this.lock.unlock();
        }
    }

    private void printAgendamento() {
        System.out.println(">>MARCACOES EM " + this.nome + "<<");
        for (Agendamento age : this.agendamentosLocal.values()) {
            System.out.println(age.getNumeroSNS() + " -" + age.getLocal());
        }
    }

    public void printLocal() {
        System.out.println("\n>> " + this.nome + " <<");
        System.out.println("-> Vagas          :" + this.numeroVagas);
        System.out.println("-> Agendamentos   :" + this.numeroAgendamentos);
        System.out.println(
                "-> Vagas Restantes:" + Integer.valueOf(this.numeroVagas - this.numeroAgendamentos).toString());
        System.out.println("-> Em Espera      :" + this.numeroEspera);
    }
}

public class MapaLocais {
    private HashMap<String, Local> mapaLocais;

    public MapaLocais(String[] nomeLocais) {
        this.mapaLocais = new HashMap<>();
        for (String nomeLoc : nomeLocais) {
            this.mapaLocais.put(nomeLoc, new Local(nomeLoc));
        }
    }

    public void addAgendamento(int numSNS, String nomeLocal, DataOutputStream out)
            throws IOException, InterruptedException {
        Agendamento age = this.getAgendamento(numSNS);
        Local l = this.mapaLocais.get(nomeLocal);

        /*
                Para se realizar um agendamento devem se cumprir os seguintes requesitos:
                1 - O utente nao ter um agendamento
                2 - O local pretendido para a vacinacao existir
        */

        if (age == null && l != null) {
            Agendamento newAge = new Agendamento(numSNS, nomeLocal, out);
            l.addAgendamento(newAge);
            this.printMapaLocais(false, newAge.getLocal());
        } else {
            if (age != null) {
                out.writeUTF(">>ERRO: ESTE NUMERO_SNS JA TEM VACINACAO CONFIRMADA");
                out.flush();
            }
            if (l == null) {
                out.writeUTF(">>ERRO: LOCALIZACAO NAO EXISTE");
                out.flush();
            }
        }
    }

    public void removeAgendamento(int numSNS, DataOutputStream out) throws IOException, InterruptedException {
        Agendamento age = this.getAgendamento(numSNS);
        Local l = this.mapaLocais.get(age.getLocal());

         /*
                Para se realizar um agendamento devem se cumprir os seguintes requesitos:
                1 - O utente ter efetuado anteriormente uma marcacao
        */

        if (l != null && age != null) {
            l.removeAgendamento(age);
            this.printMapaLocais(false, age.getLocal());
        } else {
            if (age == null) {
                out.writeUTF(">> ERRO: ESTE NUMERO_SNS NAO TEM VACINACAO MARCADA");
            }
            if (l == null) {
                out.writeUTF(">> ERRO: LOCALIZACAO NAO EXISTE");
            }
            out.flush();
        }
    }

    public Agendamento getAgendamento(int numSNS) throws IOException {
        for (Local l : mapaLocais.values()) {
            for (Agendamento age : l.getAgendamentosLocal().values()) {
                if (age.getNumeroSNS() == numSNS) {
                    return age;
                }
            }
        }
        return null;
    }

    public void printMapaLocais(boolean all, String name) {
        if (all) {
            for (Local l : this.mapaLocais.values()) {
                l.printLocal();
            }
        } else {
            mapaLocais.get(name).printLocal();
        }
    }

}
