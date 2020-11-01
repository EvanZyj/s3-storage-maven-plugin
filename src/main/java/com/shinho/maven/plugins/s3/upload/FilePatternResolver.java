package com.shinho.maven.plugins.s3.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilePatternResolver {

    private String pathPattern = null;

    private FilePatternResolver(){

    }

    FilePatternResolver(String path){
        this.pathPattern = path.replaceAll("[\\\\|/]+", "/");
    }

    private static String getRegPath(String path){
        char[] chars = path.toCharArray();
        int len = chars.length;
        StringBuilder sb = new StringBuilder();
        boolean preX = false;

        for (int i = 0; i < len; i++) {
            if (chars[i] == '*') {
                if (preX) {
                    sb.append(".*");
                    preX = false;
                } else if (i + 1 == len) {
                    sb.append("[^/\\\\]*");
                } else {
                    preX = true;
                    continue;
                }
            } else {
                if (preX) {
                    sb.append("[^/\\\\]*");
                    preX = false;
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

    public List<File> files(boolean includeDir){
        List<File> files = new ArrayList<File>();
        String patterns[] = this.pathPattern.split("[;]+");

        for (String pattern : patterns){
            files.addAll(getFiles(pattern));
        }
        return files;
    }

    public List<File> files(){
        List<File> files = new ArrayList<File>();
        String patterns[] = this.pathPattern.split("[;]+");

        for (String pattern : patterns){
            files.addAll(getFiles(pattern));
        }
        return files;
    }

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

        for (File f : dir.listFiles()) {
            if (f.isFile() && !f.isHidden()
                    && Pattern.matches(regPath, f.getAbsolutePath())) {
                files.add(f);
            } else if (f.isDirectory()){
                files.addAll(recursionDir(f,regPath));
            }
        }
        return files;
    }

//    public static void main(String args[]){
//        FilePatternResolver fpr = new FilePatternResolver("\\root\\*.jar");
//        for(File f : fpr.files()){
//            System.out.println(f.getAbsolutePath());
//        }
//    }
}
