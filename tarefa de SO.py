#!/usr/bin/env python3
import random
import os
import time
from typing import List

PROCESS_TABLE_FILE = "process_table.txt"
QUANTUM = 1000

INITIAL_TIMES = {
    0: 10000,
    1: 5000,
    2: 7000,
    3: 3000,
    4: 3000,
    5: 8000,
    6: 2000,
    7: 5000,
    8: 4000,
    9: 10000,
}

class Estado:
    PRONTO = "PRONTO"
    EXECUTANDO = "EXECUTANDO"
    BLOQUEADO = "BLOQUEADO"
    TERMINADO = "TERMINADO"

class Processo:
    def __init__(self, pid: int, tempo_total: int):
        self.pid = pid
        self.tempo_total = tempo_total
        self.cp = 0
        self.estado = Estado.PRONTO
        self.nes = 0
        self.n_cpu = 0
        self.tempo_executado_total = 0

    def to_row(self):
        return f"{self.pid}\t{self.tempo_total}\t{self.cp}\t{self.estado}\t{self.nes}\t{self.n_cpu}\n"

    @staticmethod
    def header():
        return "PID\tTP(restante)\tCP\tEP\tNES\tN_CPU\n"

def salvar_tabela(processos: List[Processo]):
    with open(PROCESS_TABLE_FILE, "w") as f:
        f.write(Processo.header())
        for p in sorted(processos, key=lambda x: x.pid):
            f.write(p.to_row())

def restaurar_processo_de_arquivo(pid: int, processos: List[Processo]):
    if not os.path.exists(PROCESS_TABLE_FILE):
        return
    with open(PROCESS_TABLE_FILE, "r") as f:
        lines = f.readlines()
    for line in lines[1:]:
        parts = line.strip().split("\t")
        if len(parts) >= 6:
            try:
                file_pid = int(parts[0])
            except ValueError:
                continue
            if file_pid == pid:
                proc = next((x for x in processos if x.pid == pid), None)
                if proc:
                    try:
                        proc.tempo_total = int(parts[1])
                        proc.cp = int(parts[2])
                        proc.estado = parts[3]
                        proc.nes = int(parts[4])
                        proc.n_cpu = int(parts[5])
                    except Exception:
                        pass
                return

def imprimir_estado(processos: List[Processo], titulo="Estado do sistema"):
    print("\n" + "="*10 + f" {titulo} " + "="*10)
    print(Processo.header().strip())
    for p in sorted(processos, key=lambda x: x.pid):
        print(f"{p.pid}\t{p.tempo_total}\t{p.cp}\t{p.estado}\t{p.nes}\t{p.n_cpu}")
    print("="*40 + "\n")

def simular():
    processos = [Processo(pid, INITIAL_TIMES[pid]) for pid in range(10)]
    ready_queue = [p.pid for p in processos]
    blocked = []
    terminated_count = 0
    tempo_global = 0
    rodada = 0
    salvar_tabela(processos)
    print("Iniciando simulação...")
    imprimir_estado(processos, "Estado inicial")

    while terminated_count < len(processos):
        rodada += 1
        if blocked:
            desbloqueados = []
            for pid in list(blocked):
                if random.random() < 0.30:
                    bloqueado_proc = next(p for p in processos if p.pid == pid)
                    bloqueado_proc.estado = Estado.PRONTO
                    ready_queue.append(pid)
                    blocked.remove(pid)
                    desbloqueados.append(pid)
            if desbloqueados:
                print(f"[Rodada {rodada}] Desbloqueados (30% chance): {desbloqueados}")
                salvar_tabela(processos)

        if not ready_queue:
            if blocked:
                print(f"[Rodada {rodada}] Nenhum PRONTO. Verificando bloqueados novamente...")
                desbloqueados = []
                for pid in list(blocked):
                    if random.random() < 0.30:
                        proc = next(p for p in processos if p.pid == pid)
                        proc.estado = Estado.PRONTO
                        ready_queue.append(pid)
                        blocked.remove(pid)
                        desbloqueados.append(pid)
                if desbloqueados:
                    print(f"[Rodada {rodada}] Desbloqueados: {desbloqueados}")
                    salvar_tabela(processos)
                    continue
                else:
                    time.sleep(0.01)
                    continue
            else:
                break

        current_pid = ready_queue.pop(0)
        proc = next(p for p in processos if p.pid == current_pid)
        restaurar_processo_de_arquivo(proc.pid, processos)
        if proc.estado == Estado.TERMINADO:
            continue
        proc.estado = Estado.EXECUTANDO
        proc.n_cpu += 1
        print(f"[Rodada {rodada}] Escalando PID {proc.pid} para EXECUTANDO (N_CPU agora {proc.n_cpu})")
        salvar_tabela(processos)

        ciclos_a_executar = min(QUANTUM, proc.tempo_total)
        ciclos_executados = 0
        ocorreu_io = False

        for ciclo in range(ciclos_a_executar):
            tempo_global += 1
            ciclos_executados += 1
            proc.cp += 1
            proc.tempo_total -= 1
            proc.tempo_executado_total += 1
            if proc.tempo_total <= 0:
                proc.estado = Estado.TERMINADO
                terminated_count += 1
                print(f"  -> PID {proc.pid} finalizou após {proc.tempo_executado_total} ciclos totais.")
                salvar_tabela(processos)
                break
            if random.random() < 0.01:
                proc.estado = Estado.BLOQUEADO
                proc.nes += 1
                ocorreu_io = True
                blocked.append(proc.pid)
                print(f"  -> PID {proc.pid} realizou E/S no ciclo {ciclos_executados} do seu quantum (NES agora {proc.nes}).")
                salvar_tabela(processos)
                break

        if proc.estado == Estado.EXECUTANDO:
            proc.estado = Estado.PRONTO
            ready_queue.append(proc.pid)
            print(f"  -> Quantum do PID {proc.pid} terminou (executou {ciclos_executados} ciclos). Troca de contexto: EXECUTANDO -> PRONTO")
            salvar_tabela(processos)

        if rodada % 5 == 0:
            imprimir_estado(processos, titulo=f"Estado após rodada {rodada}")

    print("Simulação finalizada.")
    imprimir_estado(processos, "Estado final (todos os processos terminados ou sem mais trabalho)")

if __name__ == "__main__":
    random.seed()
    simular()
