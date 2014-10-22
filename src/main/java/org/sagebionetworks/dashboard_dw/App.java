package org.sagebionetworks.dashboard_dw;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.dashboard.model.WriteRecordResult;
import org.sagebionetworks.dashboard.service.UpdateFileCallback;
import org.sagebionetworks.dashboard.service.UpdateRecordCallback;
import org.sagebionetworks.dashboard.service.UpdateService;
import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {

    // Run with 'gradle run -PfilePath=/path/to/access/log/files'
    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Must provide the file path to the access log files.");
        }
        final File filePath = new File(args[0]);
        if (!filePath.exists()) {
            throw new IllegalArgumentException("File " + filePath.getPath() + " does not exist.");
        }

        /*if (args[1]=="true" || args[1]=="TRUE" || args[1]=="True") {
            @SuppressWarnings("resource")
            final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("/META-INF/spring/scheduler-context.xml");
            context.registerShutdownHook();
            context.start();
        } else */{
            final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
            context.registerShutdownHook();
            final Logger logger = org.slf4j.LoggerFactory.getLogger(App.class);
    
            final List<File> files = new ArrayList<File>();
            getCsvGzFiles(filePath, files);
            final int total = files.size();
            logger.info("Total number of files: " + total);
            if (total == 0) {
                context.close();
                return;
            }
    
            final UpdateService updateService = context.getBean(UpdateService.class);
    
            final long start = System.nanoTime();
            try {
                for (int i = files.size() - 1; i >= 0; i--) {
                    File file = files.get(i);
                    logger.info("Loading file " + (files.size() - i) + " of " + total);
                    try {
                        InputStream is = new FileInputStream(file);
                        updateService.update(is, file.getPath(), 
                                new UpdateFileCallback() {
                                    @Override
                                    public void call(UpdateResult result) {}
                                },
                                new UpdateRecordCallback() {
                                    @Override
                                    public void handle(WriteRecordResult result) {}
                                });
                        if (is != null) {
                            is.close();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                final long end = System.nanoTime();
                logger.info("Done loading log files. Time spent (seconds): " + (end - start) / 1000000000L);
                context.close();
            }
        }
    }

    /**
     * Gets all the "csv.gz" files but exclude the "rolling" ones.
     */
    private static void getCsvGzFiles(File file, List<File> files) {
        if (file.isFile()) {
            final String fileName = file.getName();
            if (fileName.endsWith("csv.gz") && !fileName.contains("rolling")) {
                files.add(file);
            }
            return;
        }
        File[] moreFiles = file.listFiles();
        for (File f : moreFiles) {
            getCsvGzFiles(f, files);
        }
    }
}
