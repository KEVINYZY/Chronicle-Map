package net.openhft.chronicle.map.example;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.lang.model.constraints.MaxSize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by peter on 19/12/14.
 */
public class LotsOfEntriesMain {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        workEntries(true);
        workEntries(false);
    }

    private static void workEntries(final boolean add) throws IOException, ExecutionException, InterruptedException {
        final long entries = 100_000_000;
        File file = new File("/tmp/lotsOfEntries.dat");
        final ChronicleMap<CharSequence, MyFloats> map = ChronicleMapBuilder
                .of(CharSequence.class, MyFloats.class)
                .entries(entries*6/5)
                .entrySize(256)
                .createPersistedTo(file);
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService es = Executors.newFixedThreadPool(threads);
        long block = (entries + threads - 1) / threads;

        final long start = System.nanoTime();
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final long startI = t * block;
            final long endI = Math.min((t + 1) * block, entries);
            futures.add(es.submit(new Runnable() {
                @Override
                public void run() {
                    Random rand = new Random(startI);
                    StringBuilder sb = new StringBuilder();
                    MyFloats mf = map.newValueInstance();
                    if (add)
                        for (int i = 0; i < 6; i++)
                            mf.setValueAt(i, i);
                    for (long i = startI; i < endI; i++) {
                        sb.setLength(0);
                        int length = (int) (24 / (rand.nextFloat() + 24.0 / 1000));
                        sb.append(i);
                        while (sb.length() < length)
                            sb.append("-key");
                        try {
                            if (add)
                                map.put(sb, mf);
                            else
                                map.getUsing(sb, mf);
                        } catch (Exception e) {
                            System.out.println("map.size: "+map.size());
                            throw e;
                        }
                    }
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        long time = System.nanoTime() - start;
        es.shutdown();
        System.out.printf("Map.size: %,d with a throughput of %.1f million/sec to %s.%n",
                map.size(), entries * 1e3 / time, add ? "add" : "get");
        map.close();
    }
}

interface MyFloats {
    public void setValueAt(@MaxSize(6) int index, float f);

    public float getValueAt(int index);
}
