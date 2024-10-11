package io.github.lc.oss.mc.scheduler.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.encryption.Ciphers;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.scheduler.Application;
import io.github.lc.oss.mc.scheduler.app.model.EncryptedBackup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class BackupService extends AbstractService {
    /*
     * Note: Job and JobHistory are NOT backed up - this is intentional
     */
    private static final List<String> TABLES_TO_BACKUP = Collections.unmodifiableList(Arrays.asList( //
            "NODE", //
            "PROFILE", //
            "USERS", //
            "USER_HASH" //
    ));

    @Autowired
    private ConfigurableApplicationContext context;
    @Autowired
    private Environment env;
    @Autowired
    private NodeService nodeSerivce;

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public ServiceResponse<EncryptedBackup> getBackup(char[] password) {
        ServiceResponse<EncryptedBackup> response = new ServiceResponse<>();

        if (this.isBlank(password)) {
            this.addMessage(response, Messages.Application.InvalidField, this.getFieldVar("settings.backup.password"));
            return response;
        }

        List<String> data = new ArrayList<>();
        List<String> script = this.em.createNativeQuery("SCRIPT NODATA DROP").getResultList();
        this.appendUnique(data, script);

        for (String table : BackupService.TABLES_TO_BACKUP) {
            script = this.em.createNativeQuery("SCRIPT TABLE " + table).getResultList();
            this.appendUnique(data, script);
        }

        EncryptedBackup backup = new EncryptedBackup();
        String encrypted = Ciphers.AES256.encrypt(data.stream().collect(Collectors.joining("\n")), password);
        backup.setData(encrypted);

        response.setEntity(backup);
        return response;
    }

    /**
     * Specialized append method that filters out duplicate statements while still
     * preserving statement order.
     */
    private void appendUnique(List<String> data, List<String> script) {
        for (String command : script) {
            if (data.contains(command)) {
                continue;
            }

            data.add(command);
        }
    }

    private boolean isBlank(char[] password) {
        if (password == null) {
            return true;
        }

        if (password.length < 1) {
            return true;
        }

        for (int i = 0; i < password.length; i++) {
            if (!Character.isWhitespace(password[i])) {
                return false;
            }
        }

        return true;
    }

    @Transactional
    public ServiceResponse<EncryptedBackup> restoreBackup(EncryptedBackup backup) {
        ServiceResponse<EncryptedBackup> response = new ServiceResponse<>();
        String script;
        try {
            script = Ciphers.AES256.decryptString(backup.getData(), backup.getPassword());
        } catch (Exception ex) {
            this.rollback();
            this.addMessage(response, Messages.Application.ErrorDecryptingBackup);
            return response;
        }

        this.em.createNativeQuery(script).executeUpdate();

        this.nodeSerivce.clearUrlPrefix();

        if (!this.isIntegrationtest()) {
            this.restartApp();
        }

        return response;
    }

    private void restartApp() {
        ApplicationArguments args = this.context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
            }
            this.context.close();
            Application.main(args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();
    }

    private boolean isIntegrationtest() {
        return this.env.getProperty("integrationtest", Boolean.class, Boolean.FALSE);
    }
}
