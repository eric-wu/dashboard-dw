package org.sagebionetworks.dashboard.dao.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.dashboard.dao.AccessRecordDao;
import org.sagebionetworks.dashboard.dao.LogFileDao;
import org.sagebionetworks.dashboard.parse.AccessRecord;
import org.sagebionetworks.dashboard.parse.RepoRecord;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration("classpath:/META-INF/spring/test-postgres-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class AccessRecordDaoImplTest {

    private static final int TEST_SIZE = 100;

    @Resource
    private LogFileDao logFileDao;

    @Resource
    private AccessRecordDao accessRecordDao;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(200);

    @Before
    public void before() throws Exception {
        assertNotNull(accessRecordDao);
        assertNotNull(logFileDao);
        accessRecordDao.cleanup();
        logFileDao.cleanup();
    }

    @After
    public void cleanup() {
        accessRecordDao.cleanup();
        logFileDao.cleanup();
        threadPool.shutdown();
    }

    @Test
    public void test() {
        List<Runnable> tasks = new ArrayList<Runnable>();
        List<AccessRecord> recordList = createRecordList();
        for (final AccessRecord record : recordList) {
            tasks.add(new Runnable() {
                @Override
                public void run() {
                    String file_id = UUID.randomUUID().toString();
                    logFileDao.put(file_id, file_id, 0);
                    accessRecordDao.put(record, file_id);
                }
            });
        }
        assertEquals(recordList.size(), tasks.size());

        for (Runnable task : tasks) {
            threadPool.submit(task);
        }

        try {
            Thread.sleep(recordList.size() * 5L);
            while (!isDone()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(TEST_SIZE, accessRecordDao.count());
    }

    private boolean isDone() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor)threadPool;
        return pool.getActiveCount() == 0 && pool.getQueue().size() == 0;
    }

    private List<AccessRecord> createRecordList() {
        List<AccessRecord> recordList = new ArrayList<>();
        // 100 records
        for (int i = 0; i< TEST_SIZE; i++) {
            AccessRecord record = createNewRecord();
            // copy each record 100 times
            for (int j = 0; j < TEST_SIZE; j++) {
                recordList.add(record);
            }
        }
        return recordList;
    }

    private AccessRecord createNewRecord() {
        RepoRecord record = new RepoRecord();

        Random random = new Random();

        long elapse_ms = random.nextLong();
        record.setLatency(elapse_ms);

        long timestamp = random.nextLong();
        record.setTimestamp(timestamp);

        String thread_id = Long.toString(random.nextLong());
        record.setThreadId(thread_id);

        String session_id = UUID.randomUUID().toString();
        record.setSessionId(session_id);

        String request_url = new BigInteger(100, random).toString(32);
        record.setUri(request_url);

        String method = new BigInteger(10, random).toString(32);
        record.setMethod(method);

        String instance = Integer.toString(random.nextInt());
        record.setInstance(instance);

        String response_status = Integer.toString(random.nextInt());
        record.setStatus(response_status);

        return record;
    }

}