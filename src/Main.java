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
		String[] libs = new String[]{"android.", "com.google.gwt.", "org.hibernate.", "org.apache.commons.", "org.joda.time.", "com.thoughtworks.xstream."};
		String basePath = "C:\\Users\\axy190020\\Desktop\\fqndata\\";
		String[] srcPaths = new String[]{"android", "gwt", "hibernate-orm", "jdk", "joda-time", "xstream"};
		String outBasePath = "C:\\Users\\axy190020\\Desktop\\fqndata\\typedata\\";

		for (int i = 0; i < libs.length; i++) {
			ProjectSequencesGenerator psg = new ProjectSequencesGenerator(basePath + srcPaths[i], false);
			File outDir = new File(outBasePath + srcPaths[i]);
			int n = 0;
			if (!outDir.exists())
				outDir.mkdirs();
			try {
				n = psg.generateSequences(false, libs[i], outDir.getAbsolutePath());
			} catch (Throwable t) {
				t.printStackTrace();
			}

			long end = System.currentTimeMillis();
			System.out.println("Finish parsing corpus in " + (end - start) / 1000);
		}
    }
}