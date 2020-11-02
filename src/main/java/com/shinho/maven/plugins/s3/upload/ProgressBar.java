package com.shinho.maven.plugins.s3.upload;

public class ProgressBar {

    private ProgressBar(){
    }

    /**
     * Show progress by percentage。
     */
    public static void printProgressBar(double pct) {
        // if bar_size changes, then change erase_bar (in eraseProgressBar) to
        // match.
        final int bar_size = 40;
        final String empty_bar = "                                        ";
        final String filled_bar = "########################################";
        int amt_full = (int) (bar_size * (pct / 100.0));

        System.out.format("  [%s%s]", filled_bar.substring(0, amt_full),
                empty_bar.substring(0, bar_size - amt_full) + Math.round(pct) + "%");
    }

    public static void eraseProgressBar() {
        // erase_bar is bar_size (from printProgressBar) + 4 chars.
        final String erase_bar = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b" +
                                 "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";
        System.out.format(erase_bar);
    }

}
