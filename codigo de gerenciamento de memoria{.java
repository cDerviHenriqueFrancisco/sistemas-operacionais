import java.util.*;

class Processo {
    String id;
    int tamanho;
    int inicio; // índice onde começou a alocação (-1 = não alocado)

    Processo(String id, int tamanho) {
        this.id = id;
        this.tamanho = tamanho;
        this.inicio = -1;
    }
}

public class GerenciamentoMemoria {
    private int[] memoria;
    private int ponteiroNextFit = 0; // usado no Next Fit
    private Map<String, Processo> processosAlocados = new HashMap<>();

    public GerenciamentoMemoria(int tamanho) {
        memoria = new int[tamanho];
        Arrays.fill(memoria, 0);
    }

    // imprime estado atual da memória
    public void imprimirMemoria() {
        System.out.print("Mapa de Memória: ");
        for (int bit : memoria) {
            System.out.print(bit);
        }
        System.out.println();
    }

    // desalocar processo
    public void desalocar(Processo p) {
        if (p.inicio == -1) {
            System.out.println("Processo " + p.id + " não está na memória.");
            return;
        }
        for (int i = p.inicio; i < p.inicio + p.tamanho; i++) {
            memoria[i] = 0;
        }
        System.out.println("Processo " + p.id + " desalocado.");
        p.inicio = -1;
        imprimirMemoria();
    }

    // First Fit
    public boolean firstFit(Processo p) {
        for (int i = 0; i <= memoria.length - p.tamanho; i++) {
            if (livre(i, p.tamanho)) {
                alocar(p, i);
                return true;
            }
        }
        return false;
    }

    // Next Fit
    public boolean nextFit(Processo p) {
        int start = ponteiroNextFit;
        do {
            if (livre(ponteiroNextFit, p.tamanho)) {
                alocar(p, ponteiroNextFit);
                return true;
            }
            ponteiroNextFit = (ponteiroNextFit + 1) % memoria.length;
        } while (ponteiroNextFit != start);
        return false;
    }

    // Best Fit
    public boolean bestFit(Processo p) {
        int melhorIndice = -1;
        int menorEspaco = Integer.MAX_VALUE;

        for (int i = 0; i <= memoria.length - p.tamanho; i++) {
            if (livre(i, p.tamanho)) {
                int espacoLivre = contarEspaco(i);
                if (espacoLivre >= p.tamanho && espacoLivre < menorEspaco) {
                    menorEspaco = espacoLivre;
                    melhorIndice = i;
                }
            }
        }
        if (melhorIndice != -1) {
            alocar(p, melhorIndice);
            return true;
        }
        return false;
    }

    // Worst Fit
    public boolean worstFit(Processo p) {
        int piorIndice = -1;
        int maiorEspaco = -1;

        for (int i = 0; i <= memoria.length - p.tamanho; i++) {
            if (livre(i, p.tamanho)) {
                int espacoLivre = contarEspaco(i);
                if (espacoLivre >= p.tamanho && espacoLivre > maiorEspaco) {
                    maiorEspaco = espacoLivre;
                    piorIndice = i;
                }
            }
        }
        if (piorIndice != -1) {
            alocar(p, piorIndice);
            return true;
        }
        return false;
    }

    // Quick Fit (simples: divide em classes por tamanho)
    private Map<Integer, List<Integer>> listasLivres = new HashMap<>();
    public boolean quickFit(Processo p) {
        atualizarListasLivres();
        if (listasLivres.containsKey(p.tamanho)) {
            List<Integer> lista = listasLivres.get(p.tamanho);
            if (!lista.isEmpty()) {
                int pos = lista.remove(0);
                alocar(p, pos);
                return true;
            }
        }
        return firstFit(p); // fallback
    }

    // funções auxiliares
    private void alocar(Processo p, int inicio) {
        for (int i = inicio; i < inicio + p.tamanho; i++) {
            memoria[i] = 1;
        }
        p.inicio = inicio;
        processosAlocados.put(p.id, p);
        System.out.println("Processo " + p.id + " alocado em " + inicio);
        imprimirMemoria();
    }

    private boolean livre(int inicio, int tamanho) {
        if (inicio + tamanho > memoria.length) return false;
        for (int i = inicio; i < inicio + tamanho; i++) {
            if (memoria[i] == 1) return false;
        }
        return true;
    }

    private int contarEspaco(int inicio) {
        int count = 0;
        for (int i = inicio; i < memoria.length && memoria[i] == 0; i++) {
            count++;
        }
        return count;
    }

    private void atualizarListasLivres() {
        listasLivres.clear();
        int i = 0;
        while (i < memoria.length) {
            if (memoria[i] == 0) {
                int inicio = i;
                while (i < memoria.length && memoria[i] == 0) i++;
                int tamanho = i - inicio;
                listasLivres.putIfAbsent(tamanho, new ArrayList<>());
                listasLivres.get(tamanho).add(inicio);
            }
            i++;
        }
    }

    // estatística de fragmentação externa
    public int calcularFragmentacaoExterna() {
        int fragmentos = 0;
        int i = 0;
        while (i < memoria.length) {
            if (memoria[i] == 0) {
                int inicio = i;
                while (i < memoria.length && memoria[i] == 0) i++;
                int tamanho = i - inicio;
                if (tamanho < 2) fragmentos++; // considera buracos pequenos
            } else {
                i++;
            }
        }
        return fragmentos;
    }

    // exemplo de uso
    public static void main(String[] args) {
        GerenciamentoMemoria gm = new GerenciamentoMemoria(32);

        Processo p1 = new Processo("P1", 5);
        Processo p2 = new Processo("P2", 4);
        Processo p3 = new Processo("P3", 2);

        gm.imprimirMemoria();

        gm.firstFit(p1);
        gm.nextFit(p2);
        gm.bestFit(p3);

        gm.desalocar(p2);

        System.out.println("Fragmentação externa: " + gm.calcularFragmentacaoExterna());
    }
}

