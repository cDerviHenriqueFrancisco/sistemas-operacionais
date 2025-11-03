public import java.util.*;

public class SimuladorLRU {


    static final int NUM_PAGINAS_SWAP = 100;
    static final int NUM_PAGINAS_RAM = 10;

    static class Pagina {
        int N; // Número da página
        int I; // Instrução
        int D; // Dado
        int R; // Bit de acesso
        int M; // Bit de modificação
        int T; // Tempo de envelhecimento

        Pagina(int N, int I, int D, int R, int M, int T) {
            this.N = N;
            this.I = I;
            this.D = D;
            this.R = R;
            this.M = M;
            this.T = T;
        }

        @Override
        public String toString() {
            return String.format("[N=%02d, I=%03d, D=%02d, R=%d, M=%d, T=%d]", 
                N, I, D, R, M, T);
        }
    }

    static List<Pagina> criaMatrizSwap() {
        Random rand = new Random();
        List<Pagina> swap = new ArrayList<>();

        for (int i = 0; i < NUM_PAGINAS_SWAP; i++) {
            int N = i;
            int I = i + 1;
            int D = rand.nextInt(50) + 1;
            int R = 0;
            int M = 0;
            int T = rand.nextInt(9900) + 100;
            swap.add(new Pagina(N, I, D, R, M, T));
        }
        return swap;
    }

    static List<Pagina> criaMatrizRAM(List<Pagina> swap) {
        Random rand = new Random();
        List<Pagina> ram = new ArrayList<>();
        Set<Integer> escolhidos = new HashSet<>();

        while (ram.size() < NUM_PAGINAS_RAM) {
            int index = rand.nextInt(NUM_PAGINAS_SWAP);
            if (!escolhidos.contains(index)) {
                escolhidos.add(index);
                Pagina p = swap.get(index);
                ram.add(new Pagina(p.N, p.I, p.D, p.R, p.M, p.T));
            }
        }
        return ram;
    }

    static void executaInstrucao(
            List<Pagina> ram,
            List<Pagina> swap,
            LinkedList<Integer> historicoLRU,
            int instrucao) {

        System.out.println("\n>>> Instrução sorteada: " + instrucao);

        // Procura instrução na RAM
        int posicao = -1;
        for (int i = 0; i < ram.size(); i++) {
            if (ram.get(i).I == instrucao) {
                posicao = i;
                break;
            }
        }

        if (posicao != -1) {
            
            Pagina p = ram.get(posicao);
            System.out.println("Instrução " + instrucao + " encontrada na RAM (posição " + posicao + ").");

            p.R = 1;

            historicoLRU.remove((Integer) posicao);
            historicoLRU.addLast(posicao);

            if (Math.random() < 0.5) {
                p.D += 1;
                p.M = 1;
                System.out.println("Página modificada -> D=" + p.D + ", M=1");
            }

        } else {
            
            System.out.println("Instrução " + instrucao + " não está na RAM. PAGE FAULT!");

            int posSubstituir = historicoLRU.removeFirst();
            Pagina removida = ram.get(posSubstituir);
            System.out.println("Substituindo página N=" + removida.N + " (posição " + posSubstituir + ") via LRU.");

            Pagina nova = swap.get(instrucao - 1);
            ram.set(posSubstituir, new Pagina(nova.N, nova.I, nova.D, nova.R, nova.M, nova.T));

            historicoLRU.addLast(posSubstituir);
        }
    }

    public static void main(String[] args) {
        Random rand = new Random();

        List<Pagina> swap = criaMatrizSwap();
        List<Pagina> ram = criaMatrizRAM(swap);

        LinkedList<Integer> historicoLRU = new LinkedList<>();
        for (int i = 0; i < NUM_PAGINAS_RAM; i++) {
            historicoLRU.add(i);
        }

        System.out.println("=== MATRIZ SWAP (100x6) ===");
        for (int i = 0; i < 10; i++) {
            System.out.println(swap.get(i));
        }

        System.out.println("\n=== MATRIZ RAM (10x6) ===");
        for (Pagina p : ram) {
            System.out.println(p);
        }

        int pageFaults = 0, hits = 0;

        for (int i = 0; i < 20; i++) {
            int instrucao = rand.nextInt(100) + 1;

            boolean hit = ram.stream().anyMatch(p -> p.I == instrucao);
            if (hit) hits++; else pageFaults++;

            executaInstrucao(ram, swap, historicoLRU, instrucao);
        }

        System.out.println("\n=== MATRIZ RAM FINAL ===");
        for (Pagina p : ram) {
            System.out.println(p);
        }

        System.out.println("\nResumo da execução:");
        System.out.println("Total de Instruções: 20");
        System.out.println("Hits: " + hits);
        System.out.println("Page Faults: " + pageFaults);
    }
}
