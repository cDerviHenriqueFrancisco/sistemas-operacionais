import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Fork {
    private final Lock lock = new ReentrantLock();
    private final int id;

    public Fork(int id) {
        this.id = id;
    }

    public boolean pickUp(String philosopherName) {
        if (lock.tryLock()) {
            System.out.println(philosopherName + " pegou o garfo " + id);
            return true;
        }
        return false;
    }

    public void putDown(String philosopherName) {
        lock.unlock();
        System.out.println(philosopherName + " colocou o garfo " + id);
    }

    public int getId() {
        return id;
    }
}

class Philosopher extends Thread {
    private final Fork leftFork;
    private final Fork rightFork;
    private final int id;

    public Philosopher(int id, Fork left, Fork right) {
        this.id = id;
        this.leftFork = left;
        this.rightFork = right;
    }

    private void pensar() throws InterruptedException {
        System.out.println("Filósofo " + id + " está pensando...");
        Thread.sleep((long) (Math.random() * 2000 + 500));
    }

    private void comer() throws InterruptedException {
        System.out.println("Filósofo " + id + " está COMENDO 🍝...");
        Thread.sleep((long) (Math.random() * 1500 + 500));
    }

    @Override
    public void run() {
        try {
            while (true) {
                pensar();

                Fork primeiro = leftFork.getId() < rightFork.getId() ? leftFork : rightFork;
                Fork segundo = leftFork.getId() < rightFork.getId() ? rightFork : leftFork;

                if (primeiro.pickUp("Filósofo " + id)) {
                    if (segundo.pickUp("Filósofo " + id)) {
                        comer();
                        segundo.putDown("Filósofo " + id);
                    }
                    primeiro.putDown("Filósofo " + id);
                }

            }
        } catch (InterruptedException e) {
            System.out.println("Filósofo " + id + " foi interrompido.");
        }
    }
}

public class DiningPhilosophers {
    public static void main(String[] args) {
        Fork[] forks = new Fork[5];
        Philosopher[] philosophers = new Philosopher[5];

        for (int i = 0; i < 5; i++) {
            forks[i] = new Fork(i);
        }

        for (int i = 0; i < 5; i++) {
            Fork left = forks[i];
            Fork right = forks[(i + 1) % 5];

            philosophers[i] = new Philosopher(i, left, right);
            philosophers[i].start();
        }
    }
}

    

