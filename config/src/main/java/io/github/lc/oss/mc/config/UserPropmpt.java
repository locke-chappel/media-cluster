package io.github.lc.oss.mc.config;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class UserPropmpt {
    protected static Scanner stdin;

    public List<String> readList(String prompt) {
        return this.readList(prompt, null);
    }

    public List<String> readList(String prompt, String defaultValue) {
        return this.read(prompt, defaultValue, true);
    }

    public String read(String prompt) {
        return this.read(prompt, null);
    }

    public String read(String prompt, String defaultValue) {
        return this.read(prompt, defaultValue, false).get(0);
    }

    private boolean confirm() {
        System.out.print(this.getConfirmPrompt() + "y/N [n] ");
        String line = this.trim(this.readLine());
        return line.toLowerCase().startsWith("y");
    }

    private List<String> read(String prompt, String defaultValue, boolean isList) {
        String p = defaultValue == null ? prompt : prompt + "[" + defaultValue + "] ";
        System.out.print(p);
        String line = this.trim(this.readLine());
        if ("".equals(line) && defaultValue != null) {
            if (this.mustConfirm(defaultValue) && !this.confirm()) {
                this.onConfrimDecline();
            }
            if (isList) {
                return Arrays.asList(defaultValue.split(","));
            }
            return Arrays.asList(defaultValue);
        }

        while (true) {
            List<String> values;
            if (isList) {
                values = this.filter(line.split(","));
                if (this.isValid(values)) {
                    return values;
                }
            } else {
                values = this.filter(line);
                if (!values.isEmpty()) {
                    line = values.iterator().next();
                    if (this.isValid(line)) {
                        if (this.mustConfirm(line) && !this.confirm()) {
                            this.onConfrimDecline();
                        }
                        return Arrays.asList(line);
                    }
                }
            }

            System.out.print(p);
            line = this.trim(this.readLine());
        }
    }

    protected String readLine() {
        return this.readLine(false);
    }

    protected String readLine(boolean isPassword) {
        Console c = this.getConsole();
        if (c != null) {
            if (isPassword) {
                return new String(c.readPassword());
            } else {
                return c.readLine();
            }
        } else {
            if (UserPropmpt.stdin == null) {
                /*
                 * System.in can only be used once so we can't close the Scanner as we normally
                 * would once done, that would prevent future use. So we store a static
                 * reference to the scanner. Fortunately this should be a corner case for
                 * unusual systems where the console is not available.
                 */
                UserPropmpt.stdin = new Scanner(System.in);
            }

            return UserPropmpt.stdin.nextLine();
        }
    }

    protected Console getConsole() {
        return Application.getConsole();
    }

    protected List<String> filter(String... lines) {
        return Arrays.stream(lines). //
                filter(s -> !"".equals(this.trim(s))). //
                distinct(). //
                collect(Collectors.toList());
    }

    protected boolean isValid(String line) {
        return line != null && !"".equals(line);
    }

    protected boolean isValid(List<String> lines) {
        return lines != null && !lines.isEmpty();
    }

    protected boolean mustConfirm(String line) {
        return false;
    }

    protected String getConfirmPrompt() {
        return "Are you sure? ";
    }

    protected void onConfrimDecline() {
    }

    protected String trim(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
