package com.shinho.maven.plugins.s3.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilePatternResolver {

    /**
     * File wildcard to search for.
     */
    private String wildcardPath = null;

    private FilePatternResolver(){
    }

    FilePatternResolver(String wildcardPath){
        this.wildcardPath = wildcardPath.replaceAll("[\\\\|/]+", "/");
    }

    /**
     * Generate regular directory.
     */
    private static String getRegPath(String wpath){
        
        char[] chars = wpath.toCharArray();
        int len = chars.length;
        StringBuilder sb = new StringBuilder();
        boolean prex = false;

        for (int i = 0; i < len; i++) {
            if (chars[i] == '*') {
                if (prex) {
                    sb.append(".*");
                    prex = false;
                } else if (i + 1 == len) {
                    sb.append("[^/\\\\]*");
                } else {
                    prex = true;
                }
            } else {
                if (prex) {
                    sb.append("[^/\\\\]*");
                    prex = false;
                }
                if (chars[i] == '?') {
                    sb.append('.');
                }else if(chars[i] == '.'){
                    sb.append("\\.");
                }else if(chars[i] == '\\'){
                    sb.append("/");
                }else if(chars[i] == '/'){
                    sb.append("[\\\\|/]+");
                }
                else {
                    sb.append(chars[i]);
                }
            }
        }
        return sb.toString();
    }

    public List<File> files(){
        String[] wpaths = this.wildcardPath.split("[;]+");
        HashSet<File> fileSet = new HashSet<File>();

        for (String wpath : wpaths){
            fileSet.addAll(getFiles(wpath));
        }

        return new ArrayList<File>(fileSet);
    }

    /**
     * get matching files
     */
    private static List<File> getFiles(String path){
        List<File> files = new ArrayList<File>();

        String regPath = getRegPath(path);
        String sourceDir = path;
        Pattern pattern = Pattern.compile("^.+?[\\*\\?]+");
        Matcher matcher = pattern.matcher(path);

        //Pattern lstSep = Pattern.compile("^.+[\\\\|/]+");
        if(matcher.find()){
            sourceDir = matcher.group();
            sourceDir = sourceDir.substring(0,sourceDir.lastIndexOf('/'));
        }

        File dir = new File(sourceDir);
        if (!dir.exists()) {
            return files;
        }

        if(dir.isFile()){
            files.add(dir);
            return files;
        }else if (dir.isDirectory()){
            files = recursionDir(dir, regPath);
        }

        return files;
    }

    private static List<File> recursionDir(File dir, String regPath){
        List<File> files = new ArrayList<File>();

        if(dir == null || dir.listFiles() == null) {
            return files;
        }

        for (File f : dir.listFiles()) {
            if (f.isFile() && !f.isHidden()
                    && Pattern.matches(regPath, f.getAbsolutePath())) {
                files.add(f);
            } else if (f.isDirectory()) {
                files.addAll(recursionDir(f, regPath));
            }
        }
        return files;
    }

}
