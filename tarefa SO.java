import java.io.*;
import java.util.*;

public class SimuladorSO {

    // ========================
    // CONSTANTES
    // ========================
    private static final int QUANTUM = 1000;
    private static final String ARQUIVO_TABELA = "process_table.txt";

    private static final Map<Integer, Integer> TEMPOS_INICIAIS = Map.ofEntries(
        Map.entry(0, 10000),
        Map.entry(1, 5000),
        Map.entry(2, 7000),
        Map.entry(3, 3000),
        Map.entry(4, 3000),
        Map.entry(5, 8000),
        Map.entry(6, 2000),
        Map.entry(7, 5000),
        Map.entry(8, 4000),
        Map.entry(9, 10000)
    );

    enum Estado {
        PRONTO, EXECUTANDO, BLOQUEADO, TERMINADO
    }

    // ========================
    // CLASSE PROCESSO
    // ========================
    static class Processo {
        int pid;
        int tempoRestante;
        int cp;      // contador de programa
        Estado estado;
        int nes;     // nº de operações de E/S
        int ncpu;    // nº de vezes que usou a CPU

        public Processo(int pid, int tempoRestante) {
            this.pid = pid;
            this.tempoRestante = tempoRestante;
            this.cp = 0;
            this.estado = Estado.PRONTO;
            this.nes = 0;
            this.ncpu = 0;
        }

        @Override
        public String toString() {
            return pid + "\t" + tempoRestante + "\t" + cp + "\t" +
                   estado + "\t" + nes + "\t" + ncpu;
        }

        public static String cabecalho() {
            return "PID\tTP(restante)\tCP\tEP\tNES\tN_CPU";
        }
    }

    // ========================
    // SALVAR / RESTAURAR
    // ========================
    static void salvarTabela(List<Processo> processos) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARQUIVO_TABELA))) {
            pw.println(Processo.cabecalho());
            for (Processo p : processos) {
                pw.println(p.toString());
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar tabela: " + e.getMessage());
        }
    }

    static void restaurarProcessoDeArquivo(int pid, List<Processo> processos) {
        File f = new File(ARQUIVO_TABELA);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            br.readLine(); // cabeçalho
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] parts = linha.split("\t");
                if (parts.length < 6) continue;
                int filePid = Integer.parseInt(parts[0]);
                if (filePid == pid) {
                    Processo p = processos.get(pid);
                    p.tempoRestante = Integer.parseInt(parts[1]);
                    p.cp = Integer.parseInt(parts[2]);
                    p.estado = Estado.valueOf(parts[3]);
                    p.nes = Integer.parseInt(parts[4]);
                    p.ncpu = Integer.parseInt(parts[5]);
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao restaurar processo: " + e.getMessage());
        }
    }

    // ========================
    // FUNÇÃO PRINCIPAL
    // ========================
    public static void main(String[] args) {
        Random rand = new Random();
        List<Processo> processos = new ArrayList<>();
        Queue<Integer> prontos = new LinkedList<>();
        List<Integer> bloqueados = new ArrayList<>();

        // cria os 10 processos
        for (int i = 0; i < 10; i++) {
            Processo p = new Processo(i, TEMPOS_INICIAIS.get(i));
            processos.add(p);
            prontos.add(i);
        }

        salvarTabela(processos);
        System.out.println("=== Simulação iniciada ===");
        imprimirEstado(processos, "Estado inicial");

        int terminados = 0;
        int rodada = 0;

        while (terminados < processos.size()) {
            rodada++;

            // 1️⃣ Verifica bloqueados: 30% de chance de voltar a PRONTO
            List<Integer> desbloqueados = new ArrayList<>();
            for (Iterator<Integer> it = bloqueados.iterator(); it.hasNext();) {
                int pid = it.next();
                if (rand.nextDouble() < 0.30) {
                    Processo p = processos.get(pid);
                    p.estado = Estado.PRONTO;
                    prontos.add(pid);
                    it.remove();
                    desbloqueados.add(pid);
                }
            }
            if (!desbloqueados.isEmpty()) {
                System.out.println("[Rodada " + rodada + "] Desbloqueados: " + desbloqueados);
                salvarTabela(processos);
            }

            if (prontos.isEmpty()) {
                // nenhum pronto -> aguarda desbloqueio
                if (bloqueados.isEmpty()) break; // todos terminaram
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                continue;
            }

            // 2️⃣ Escolhe próximo processo pronto (Round Robin)
            int pid = prontos.poll();
            Processo p = processos.get(pid);

            // Restaurar dados (PRONTO -> EXECUTANDO)
            restaurarProcessoDeArquivo(pid, processos);

            if (p.estado == Estado.TERMINADO) continue;

            p.estado = Estado.EXECUTANDO;
            p.ncpu++;
            System.out.println("[Rodada " + rodada + "] Executando PID " + p.pid);
            salvarTabela(processos);

            boolean fezIO = false;

            for (int ciclo = 1; ciclo <= QUANTUM; ciclo++) {
                p.cp++;
                p.tempoRestante--;

                if (p.tempoRestante <= 0) {
                    p.estado = Estado.TERMINADO;
                    terminados++;
                    System.out.println("  -> PID " + p.pid + " terminou.");
                    salvarTabela(processos);
                    break;
                }

                // 1% de chance de E/S
                if (rand.nextDouble() < 0.01) {
                    p.estado = Estado.BLOQUEADO;
                    p.nes++;
                    bloqueados.add(p.pid);
                    fezIO = true;
                    System.out.println("  -> PID " + p.pid + " fez E/S (NES=" + p.nes + ")");
                    salvarTabela(processos);
                    break;
                }
            }

            // 3️⃣ Se não terminou nem fez IO → quantum acabou → PRONTO
            if (p.estado == Estado.EXECUTANDO) {
                p.estado = Estado.PRONTO;
                prontos.add(p.pid);
                System.out.println("  -> PID " + p.pid + " terminou o quantum → PRONTO");
                salvarTabela(processos);
            }

            if (rodada % 5 == 0)
                imprimirEstado(processos, "Após rodada " + rodada);
        }

        System.out.println("=== Simulação finalizada ===");
        imprimirEstado(processos, "Estado final");
    }

    // ========================
    // IMPRESSÃO
    // ========================
    static void imprimirEstado(List<Processo> processos, String titulo) {
        System.out.println("\n==== " + titulo + " ====");
        System.out.println(Processo.cabecalho());
        for (Processo p : processos) {
            System.out.println(p);
        }
        System.out.println("===========================\n");
    }
}

