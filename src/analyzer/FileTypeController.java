package analyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileTypeController {

    public void run(String[] args) {
        if (args.length < 2) {
            System.out.println("Wrong number of command line arguments");
            return;
        }

        String patternFileName = args[1];
        String folderName = args[0];
        List<PatternRecord> patterns = getPatterns(patternFileName);

        if (patterns.size() == 0) {
            System.out.printf("Pattern file %s not found%n", patternFileName);
        } else {
            launchSearches(folderName, patterns);
        }
    }

    private List<PatternRecord> getPatterns(String fileName) {
        List<PatternRecord> patterns = new ArrayList<>();
        File file = new File(fileName);

        if (!file.exists()) {
            return patterns;
        }

        List<String> lines;

        try {
            lines = Files.readAllLines(Paths.get(fileName));
        } catch (IOException e) {
            System.out.println("IO Exception reading " + fileName);
            e.printStackTrace();

            return patterns;
        }

        for (String line : lines) {
            String[] parts = line.split(";");

            if (parts.length != 3) {
                System.out.println("Bad record in " + fileName);
                continue;
            }

            PatternRecord patternRecord = new PatternRecord(
                    Integer.parseInt(parts[0]),
                    stripQuotes(parts[1]),
                    stripQuotes(parts[2]));
            patterns.add(patternRecord);
        }

        return patterns;
    }

    private String stripQuotes(String string) {
        if (string.startsWith("\"")) {
            string = string.substring(1);
        }

        if (string.endsWith("\"")) {
            string = string.substring(0, string.length() - 1);
        }

        return string;
    }

    private void launchSearches(String folderName, List<PatternRecord> patterns) {
        File folder = new File(folderName);

        if (!folder.isDirectory()) {
            System.out.printf("%s is not a directory%n", folderName);
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            executor.submit(() -> {
                String fileName = file.getName();
                byte[] fileBytes = readBinFile(folderName + "/" + fileName);

                if (fileBytes.length == 0) {
                    System.out.printf("File %s not found%n", fileName);
                } else {
                    printFileType(fileBytes, fileName, patterns);
                }
            });
        }

        executor.shutdown();

        try {
            boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);

            if (!terminated) {
                System.out.println("Process timed out before termination");
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted while awaiting termination");
            e.printStackTrace();
        }
    }

    private byte[] readBinFile(String fileName) {
        File file = new File(fileName);

        if (!file.exists()) {
            return new byte[0];
        }

        try {
            return Files.readAllBytes(Paths.get(fileName));
        } catch (IOException e) {
            System.out.println("IO Exception while reading bytes");
            e.printStackTrace();
        }

        return new byte[0];
    }

    private void printFileType(byte[] fileBytes, String fileName, List<PatternRecord> patterns) {
        PatternRecord highestPatternRecord = new PatternRecord(0, "", "Unknown file type");

        for (PatternRecord patternRecord : patterns) {
            if (kpmSearch(fileBytes, patternRecord.getPattern())
                    && patternRecord.getPriority() > highestPatternRecord.getPriority()) {
                highestPatternRecord = patternRecord;
            }
        }

        System.out.printf("%s: %s%n", fileName, highestPatternRecord.getDescription());
    }

    private boolean  kpmSearch(byte[] bytes, String pattern) {
        if (bytes.length < pattern.length()) {
            return false;
        }

        int[] prefixFunc = prefixFunction(pattern);
        int j = 0;

        for (byte aByte : bytes) {
            while (j > 0 && aByte != pattern.charAt(j)) {
                j = prefixFunc[j - 1];
            }

            if (aByte == pattern.charAt(j)) {
                j++;
            }

            if (j == pattern.length()) {
                return true;
            }
        }

        return false;
    }

    private int[] prefixFunction(String str) {
        int[] prefixFunc = new int[str.length()];

        for (int i = 1; i < str.length(); i++) {
            int j = prefixFunc[i - 1];

            while (j > 0 && str.charAt(i) != str.charAt(j)) {
                j = prefixFunc[j - 1];
            }

            if (str.charAt(i) == str.charAt(j)) {
                j++;
            }

            prefixFunc[i] = j;
        }

        return prefixFunc;
    }
}
