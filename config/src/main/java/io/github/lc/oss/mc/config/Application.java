package io.github.lc.oss.mc.config;

import java.io.Console;
import java.io.File;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.lc.oss.commons.encryption.Ciphers;
import io.github.lc.oss.commons.encryption.ephemeral.FileBackedCipher;
import io.github.lc.oss.commons.util.IoTools;

public class Application {
    private static final Pattern WINDOWS_ENVVAR = Pattern.compile(".*%[^%/]+%.*");

    private static boolean abort = false;
    private static Console console = System.console();
    private static String pathSeparator = System.getProperty("file.separator");

    public static void main(String[] args) {
        if (Application.getConsole() == null) {
            System.err.println("WARNING: Unable to bind to system console. " + //
                    "Password inputs will not be properly masked. " + //
                    "Continue at your own risk.");
        }

        boolean isExternalAuth = false;

        Map<String, Object> data = new HashMap<>();
        String keyPath;
        String configPath;
        String line;
        List<String> lines;
        UserPropmpt prompt = new UserPropmpt();
        UserPropmpt filePrompt = new UserPropmpt() {
            @Override
            protected String getConfirmPrompt() {
                return "File already exists, overwrite? ";
            }

            @Override
            protected boolean mustConfirm(String line) {
                File f = new File(line);
                return f.exists();
            }

            @Override
            protected void onConfrimDecline() {
                System.err.println("Unable to continue, exiting");
                /*
                 * Yes, yes - somewhat of a hackish design but given the critical nature of the
                 * path this enables easier and more reliable testability without the need for
                 * clever interception of System.exit() calls...
                 *
                 * An example of allowing testability to drive design in favor or more elegant
                 * designs. I don't regret my choice here. :)
                 */
                Application.abort = true;
            }
        };
        UserPropmpt passwordPrompt = new UserPropmpt() {
            @Override
            protected String readLine() {
                return this.readLine(true);
            }
        };

        line = prompt.read("Use external authentication: y/N ", "n");
        isExternalAuth = line.toLowerCase().startsWith("y");

        if (isExternalAuth) {
            lines = prompt.readList("Enter JWT Issuer IDs, comma delimited: ");
            data.put("JwtIssuers", lines);

            line = passwordPrompt.read("Enter Scheduler's assigned Private Key: ");
            data.put("PrivateKey", line);
        }

        line = prompt.read("Enter Database URL: ", "jdbc:h2:~/app-data/database");
        data.put("DatabaseUrl", Application.normalizeFilePath(line));

        line = prompt.read("Enter Database Username: ", "sa");
        data.put("DatabaseUser.username", line);
        line = passwordPrompt.read("Enter Database Password: ");
        data.put("DatabaseUser.password", line);

        line = passwordPrompt.read("Enter Keystore Password: ");
        data.put("KeystorePassword", line);

        lines = prompt.readList("Enter User JWT Secret IDs, comma delimited: ", "user-sessions-1");
        data.put("UserJwtSecrets", lines);

        String cwd = Application.normalizeFilePath(System.getProperty("user.dir"));
        if (!cwd.endsWith("/")) {
            cwd = cwd + "/";
        }

        keyPath = filePrompt.read("Enter key file path: ", "key");
        keyPath = Application.normalizeFilePath(keyPath);
        if (!Application.abort) {
            if (keyPath.contains("/")) {
                line = keyPath.substring(0, keyPath.lastIndexOf("/")) + "/config";
            } else {
                line = "config";
                keyPath = cwd + keyPath;
            }
            configPath = filePrompt.read("Enter encrypted config file path: ", line);
            configPath = Application.normalizeFilePath(configPath);
            if (!Application.abort) {
                if (!configPath.contains("/")) {
                    configPath = cwd + configPath;
                }

                new File(keyPath.substring(0, keyPath.lastIndexOf("/"))).mkdirs();
                new File(configPath.substring(0, configPath.lastIndexOf("/"))).mkdirs();

                String json = Application.toJson(data);

                byte[] key = new byte[1024];
                SecureRandom rnd = new SecureRandom();
                rnd.nextBytes(key);
                IoTools.writeToFile(key, keyPath);

                FileBackedCipher cipher = new FileBackedCipher(keyPath);
                IoTools.writeToFile(cipher.encrypt(json, Ciphers.AES256), configPath);

                File keyFile = new File(keyPath);
                File configFile = new File(configPath);
                System.out.println(String.format("Done. Please copy '%s' and '%s' to the scheduler instance.",
                        keyFile.getAbsolutePath(), configFile.getAbsolutePath()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Map<String, Object> data) {
        String jwtIssuers = "";
        if (data.containsKey("JwtIssuers")) {
            List<String> values = (List<String>) data.get("JwtIssuers");
            jwtIssuers = "  \"JwtIssuers\" : [\n" + //
                    values.stream().map(s -> "    \"" + s + "\"").collect(Collectors.joining(",\n")) + //
                    "\n  ],\n";
        }

        String privateKey = "";
        if (data.containsKey("PrivateKey")) {
            privateKey = "  \"PrivateKey\" : \"" + (String) data.get("PrivateKey") + "\",\n";
        }

        return "{\n" + //
                jwtIssuers + //
                privateKey + //
                "  \"DatabaseUrl\" : \"" + (String) data.get("DatabaseUrl") + "\",\n" + //
                "  \"DatabaseUser\" : {\n" + //
                "    \"username\" : \"" + (String) data.get("DatabaseUser.username") + "\",\n" + //
                "    \"password\" : \"" + (String) data.get("DatabaseUser.password") + "\"\n" + //
                "  },\n" + //
                "  \"KeystorePassword\" : \"" + (String) data.get("KeystorePassword") + "\",\n" + //
                "  \"UserJwtSecrets\" : [\n" + //
                ((List<String>) data.get("UserJwtSecrets")).stream().map(s -> "    \"" + s + "\"")
                        .collect(Collectors.joining(",\n"))
                + //
                "\n  ]\n" + //
                "}\n";
    }

    /**
     * Attempts to detect Windows file paths and normalizes them to *nix paths.
     */
    private static String normalizeFilePath(String path) {
        return normalizeFilePath(path, false);
    }

    /*
     * Exposed for testing
     */
    static String normalizeFilePath(String path, boolean includeTrailingSlash) {
        if (path == null) {
            return includeTrailingSlash ? "/" : null;
        }

        String p = path.trim();
        if (Application.getFileSeparator().equals("\\") && Application.isWindowsFilePath(p)) {
            p = p.replace("\\", "/");
        }

        if (includeTrailingSlash && !p.endsWith("/")) {
            p = p + "/";
        }

        return p;
    }

    /*
     * Exposed for testing
     */
    static Console getConsole() {
        return Application.console;
    }

    /*
     * Exposed for testing
     */
    static boolean isWindowsFilePath(String path) {
        if (path.toLowerCase().contains("\\")) {
            if (path.toLowerCase().contains(":") || //
                    path.toLowerCase().contains("${user.home}") || //
                    Application.WINDOWS_ENVVAR.matcher(path).matches() //
            ) {
                return true;
            }
        }

        return false;
    }

    /*
     * Exposed for testing
     */
    static String getFileSeparator() {
        return Application.pathSeparator;
    }

    private Application() {
    }
}
