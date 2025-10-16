import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Practice1 {
    public static void main(String[] args) throws Exception {
        List<Thread> orders = new ArrayList<>();
        List<Thread> allThreads = new ArrayList<>(); // список для моніторингу всіх потоків

        // створення кав'ярні та потоку баристи
        Cafe cafe = new Cafe();
        Thread barista = new Thread(new Barista(cafe), "[Barista]");
        
        allThreads.add(barista); // додаємо баристу до списку моніторингу

        // створення потоків-клієнтів
        for (int i = 1; i <= 7; i++) {
            Thread customer = new Thread(new Customer(cafe, "[Customer-" + i + "]"));
            orders.add(customer);
        }
        allThreads.addAll(orders);

        // потік періодично виводить стани всіх інших потоків
        Thread monitor = new Thread(() -> {
            for (int k = 0; k < 12; k++) { 
                safePrint("====> MONITOR: " + states(allThreads));
                try { 
                    Thread.sleep(500); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "[Monitor]");

        safePrint("Initial states: " + states(allThreads));
        
        // запуск потоків
        monitor.start();
        barista.start();
        Thread.sleep(100); // затримка, щоб бариста встиг відкрити кав'ярню

        for (Thread order : orders) {
            order.start();
            Thread.sleep(200);
        }
        
        // завершення всіх потоків
        barista.join();
        for (Thread order : orders) {
            order.join();
        }
        monitor.join(); // завершення монітора
        
        safePrint("Final states: " + states(allThreads));
    }

    public static void safePrint(String s) {
        synchronized (System.out) {
            System.out.println(s);
        }
    }

    private static String states(List<Thread> threads) {
        StringBuilder sb = new StringBuilder();
        for (Thread t : threads) {
            sb.append(t.getName()).append('=').append(t.getState()).append(' ');
        }
        return sb.toString().trim();
    }
}

class Customer implements Runnable {
    private Cafe cafe;
    private String name;

    public Customer(Cafe cafe, String name) {
        this.cafe = cafe;
        this.name = name;
    }

    // клієнт не може увійти після закриття кафе
    @Override
    public void run() {
        if (!cafe.isOpen()) {
            Practice1.safePrint(name + " can't enter, because the cafe is closed.");
            return;
        } else {
            Practice1.safePrint(name + " entered the cafe.");
        }
        try {
            cafe.serveCoffee(name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class Barista implements Runnable {
    private Cafe cafe;

    public Barista(Cafe cafe) {
        this.cafe = cafe;
    }

    // бариста відкриває/зачиняє кафе  
    @Override
    public void run() {
        cafe.openCafe();
        try {
            Thread.sleep(5000); // працює 5 сек 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cafe.closeCafe();
        Practice1.safePrint(Thread.currentThread().getName() + " finished work and went home.");
    }
}

class Cafe {
    private volatile boolean open = false; // volatile для кращої видимості між потоками

    // обмеження кількості одночасних приготувань кави до 2
    private Semaphore coffeeMachines = new Semaphore(2, true);

    public void serveCoffee(String customer) throws InterruptedException {
        // потік чекає на вільну кавомашину
        coffeeMachines.acquire();
        try {
            Practice1.safePrint("[Order] " + customer + " is placing an order...");
            Thread.sleep(2000); 
            Practice1.safePrint("[Done] " + customer + " has received their coffee!");
        } finally {
            // потік звільняє кавомашину, щоб готувати інші замовлення
            coffeeMachines.release();
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void openCafe() {
        open = true;
        Practice1.safePrint("The cafe is open!");
    }

    public void closeCafe() {
        open = false;
        Practice1.safePrint("The cafe is closed!");
    }
}