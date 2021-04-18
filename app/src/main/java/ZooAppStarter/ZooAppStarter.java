package ZooAppStarter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

public class ZooAppStarter implements Watcher, AsyncCallback.StatCallback {
    private static final String ZNODE = "/z";
    private static final Logger logger = LogManager.getLogger("ZooAppStarter");

    private final ZooKeeper zooKeeper;
    private final String[] exec;
    private Process app;
    private int childrenNumber = 0;

    public ZooAppStarter(String hostPort, String[] exec) throws IOException {
        this.exec = exec;
        zooKeeper = new ZooKeeper(hostPort, 3000, this);
        zooKeeper.exists(ZNODE, true, this, null);
    }

    @Override
    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            printChildrenNumber();
        } else {
            if (path != null && path.equals(ZNODE)) {
                zooKeeper.exists(ZNODE, true, this, null);
            }
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        switch (KeeperException.Code.get(rc)) {
            case OK -> this.exists(true);
            case NONODE -> this.exists(false);
            default -> zooKeeper.exists(ZNODE, true, this, null);
        }

    }

    private int watchChildren(String root) {
        int number = 0;
        String path = root + "/";
        try {
            List<String> children = zooKeeper.getChildren(root, this);
            number = children.size();
            number += children.stream().map(child -> path + child).mapToInt(this::watchChildren).sum();
        } catch (Exception ignored) {
        }
        return number;
    }

    private void printChildrenNumber() {
        int newChildrenNumber = watchChildren(ZNODE);
        if (newChildrenNumber > childrenNumber)
            logger.info("Number of children of " + ZNODE + ": " + newChildrenNumber);
        childrenNumber = newChildrenNumber;
    }

    private void printChildrenTree(int level, String path, String root) {
        System.out.println("-" + root);
        String newPath = path + root;
        try {
            zooKeeper.getChildren(newPath, false).forEach(child -> {
                System.out.print(" |".repeat(level));
                printChildrenTree(level + 1, newPath, "/" + child);
            });
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void exists(boolean exists) {
        if (exists) {
            kill();
            spawn();
        } else {
            kill();
        }
    }

    private void spawn() {
        try {
            app = Runtime.getRuntime().exec(exec);
            zooKeeper.getChildren(ZNODE, this);
            logger.info("App started");
        } catch (IOException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    private void kill() {
        if (app != null) {
            app.destroy();
            try {
                app.waitFor();
                logger.info("Killing app ... done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            app = null;
        }
    }

    private void start() {
        try {
            int read;
            synchronized (this) {
                while (true) {
                    read = System.in.read();
                    if (read == -1) break;
                    logger.info(ZNODE + " tree structure:");
                    printChildrenTree(1, "", ZNODE);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) {
        String hostPort = args[0];
        String[] exec = new String[args.length - 1];
        System.arraycopy(args, 1, exec, 0, exec.length);
        try {
            new ZooAppStarter(hostPort, exec).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
