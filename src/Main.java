package fqntypeparser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import fqntypeparser.FileUtil;


public class Main {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String srcPath = "C:\\Users\\axy190020\\Desktop\\fqndata\\xstream";
        String lib = "com.thoughtworks.xstream";
        String outPath = "C:\\Users\\axy190020\\Desktop\\fqndata\\typedata\\xstream";

        ProjectSequencesGenerator psg = new ProjectSequencesGenerator(srcPath, false);
        File outDir = new File(outPath);
        int n = 0;
        if (!outDir.exists())
            outDir.mkdirs();
        try {
            n = psg.generateSequences(false, lib, outDir.getAbsolutePath());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        long end = System.currentTimeMillis();
        System.out.println("Finish parsing corpus in " + (end - start) / 1000);
    }
}