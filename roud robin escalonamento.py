from collections import deque

def round_robin(processos, burst_time, quantum):
    """
    Implementação do algoritmo Round Robin
    
    processos   -> lista com os nomes dos processos (ex: ["P1", "P2", "P3"])
    burst_time  -> lista com o tempo de execução de cada processo
    quantum     -> fatia de tempo atribuída a cada processo
    
    Retorna: tempo de espera médio e tempo de retorno médioG
    """
    
    n = len(processos)
    fila = deque(range(n))  # fila de índices dos processos
    tempo_restante = burst_time[:]  # copia do burst time
    tempo = 0  # tempo atual
    tempo_espera = [0] * n
    tempo_retorno = [0] * n
    
    # enquanto houver processo na fila
    while fila:
        i = fila.popleft()  # pega o primeiro da fila
        if tempo_restante[i] > quantum:
            tempo += quantum
            tempo_restante[i] -= quantum
            fila.append(i)  # volta para o fim da fila
        else:
            tempo += tempo_restante[i]
            tempo_restante[i] = 0
            tempo_retorno[i] = tempo
            tempo_espera[i] = tempo - burst_time[i]
    
    # cálculos finais
    tempo_espera_medio = sum(tempo_espera) / n
    tempo_retorno_medio = sum(tempo_retorno) / n
    
    # exibição
    print("Processo | Burst Time | Tempo de Espera | Tempo de Retorno")
    for i in range(n):
        print(f"{processos[i]:8} | {burst_time[i]:10} | {tempo_espera[i]:14} | {tempo_retorno[i]:15}")
    
    print(f"\nTempo médio de espera: {tempo_espera_medio:.2f}")
    print(f"Tempo médio de retorno: {tempo_retorno_medio:.2f}")
    
    return tempo_espera_medio, tempo_retorno_medio


# Exemplo de uso
processos = ["P1", "P2", "P3", "P4"]
burst_time = [10, 5, 8, 6]  # tempos de execução
quantum = 3

round_robin(processos, burst_time, quantum)
