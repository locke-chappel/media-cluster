package io.github.lc.oss.mc.api;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.lc.oss.commons.serialization.JsonMessage;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.util.TypedEnumCache;

public class Messages extends JsonMessage {
    public enum Categories implements Category {
        Application,
        Authentication;

        private static final TypedEnumCache<Categories, Category> CACHE = new TypedEnumCache<>(Categories.class);

        public static Set<Category> all() {
            return Categories.CACHE.values();
        }

        public static Category byName(String name) {
            return Categories.CACHE.byName(name);
        }

        public static boolean hasName(String name) {
            return Categories.CACHE.hasName(name);
        }

        public static Category tryParse(String name) {
            return Categories.CACHE.tryParse(name);
        }
    }

    public enum Application implements io.github.lc.oss.commons.serialization.Message {
        /*
         * Framework
         */
        UnhandledError(io.github.lc.oss.commons.api.identity.Messages.Application.UnhandledError),
        RequiredFieldMissing(io.github.lc.oss.commons.api.identity.Messages.Application.RequiredFieldMissing),
        InvalidField(io.github.lc.oss.commons.api.identity.Messages.Application.InvalidField),
        NotFound(io.github.lc.oss.commons.api.identity.Messages.Application.NotFound),
        InvalidOperation(Severities.Error, 100),
        InvalidFilePath(Severities.Error, 101),

        /*
         * General
         */
        // StaleObject(Severities.Error, 1000),
        InvalidSearchTerm(Severities.Error, 1001),
        InvalidPageSize(Severities.Error, 1002),
        InvalidPageNumber(Severities.Error, 1003),

        ChangesSaved(Severities.Success, 1),

        /*
         * Nodes
         */
        InvalidNode(Severities.Error, 2001),
        NodeNotAvailable(Severities.Error, 2002),
        DuplicateNodeName(Severities.Error, 2003),
        NodeNotBusy(Severities.Error, 2004),

        NodesInformed(Severities.Success, 2001),
        NodesStatusRefreshed(Severities.Success, 2002),

        /*
         * Jobs
         */
        AlreadyProcessingJob(Severities.Error, 3001),
        NothingToProcess(Severities.Error, 3003),
        InvalidJobType(Severities.Error, 3004),
        InvalidJobStatus(Severities.Error, 3005),
        ErrorDispatchingJob(Severities.Error, 3006),
        UnfinishedJob(Severities.Error, 3007),
        SourceAlradyExistsInCluster(Severities.Error, 3008),
        ErrorWritingVideoList(Severities.Error, 3009),
        JobRequiresProfile(Severities.Error, 3010),
        FailedToStartJob(Severities.Error, 3011),
        UnprocessableJob(Severities.Error, 3012),
        MixedClusterNames(Severities.Error, 3013),
        NoJobSelected(Severities.Error, 3014),
        ErrorAbortingJob(Severities.Error, 3015),

        JobBatchStatus(Severities.Info, 3015),

        /*
         * Scheduler
         */
        NoSchedulerUrl(Severities.Error, 4001),

        NoJobsAvailable(Severities.Info, 4001),
        ImportingJobs(Severities.Info, 4002),

        /*
         * User
         */
        PasswordMismatch(Severities.Error, 5002),

        /*
         * Backup
         */
        ErrorDecryptingBackup(Severities.Error, 6001),

        RestoreBackup(Severities.Success, 6005),

        /*
         * Profile
         */
        ArgsIncludesTrimFilter(Severities.Warning, 1);

        private static final TypedEnumCache<Messages.Application, Messages.Application> CACHE = new TypedEnumCache<>(
                Application.class);

        public static Set<Messages.Application> all() {
            return Messages.Application.CACHE.values();
        }

        public static Messages.Application byName(String name) {
            return Messages.Application.CACHE.byName(name);
        }

        public static boolean hasName(String name) {
            return Messages.Application.CACHE.hasName(name);
        }

        public static Messages.Application tryParse(String name) {
            return Messages.Application.CACHE.tryParse(name);
        }

        private final Category category;
        private final Severity severity;
        private final int number;

        private Application(io.github.lc.oss.commons.api.identity.Messages.Application src) {
            this.category = Messages.Categories.Application;
            this.severity = src.getSeverity();
            this.number = src.getNumber();
        }

        private Application(Severity severiy, int number) {
            this.category = Categories.Application;
            this.severity = severiy;
            this.number = number;
        }

        @Override
        public Category getCategory() {
            return this.category;
        }

        @Override
        public Severity getSeverity() {
            return this.severity;
        }

        @Override
        public int getNumber() {
            return this.number;
        }
    }

    public enum Authentication implements io.github.lc.oss.commons.serialization.Message {
        /*
         * Framework
         */
        InvalidCredentials(io.github.lc.oss.commons.api.identity.Messages.Authentication.InvalidCredentials),
        InvalidToken(io.github.lc.oss.commons.api.identity.Messages.Authentication.InvalidToken),
        AutoLoginError(Severities.Error, 2001),

        AutoLoginInProgress(Severities.Info, 2001),

        /*
         * API Calls
         */
        ExpiredRequest(Severities.Error, 1004),
        InvalidSignature(Severities.Error, 1005);

        private static final TypedEnumCache<Messages.Authentication, Messages.Authentication> CACHE = new TypedEnumCache<>(
                Messages.Authentication.class);

        public static Set<Messages.Authentication> all() {
            return Messages.Authentication.CACHE.values();
        }

        public static Messages.Authentication byName(String name) {
            return Messages.Authentication.CACHE.byName(name);
        }

        public static boolean hasName(String name) {
            return Messages.Authentication.CACHE.hasName(name);
        }

        public static Messages.Authentication tryParse(String name) {
            return Messages.Authentication.CACHE.tryParse(name);
        }

        private final Category category;
        private final Severity severity;
        private final int number;

        private Authentication(io.github.lc.oss.commons.api.identity.Messages.Authentication src) {
            this.category = io.github.lc.oss.commons.api.identity.Messages.Categories.Authentication;
            this.severity = src.getSeverity();
            this.number = src.getNumber();
        }

        private Authentication(Severity severiy, int number) {
            this.category = io.github.lc.oss.commons.api.identity.Messages.Categories.Authentication;
            this.severity = severiy;
            this.number = number;
        }

        @Override
        public Category getCategory() {
            return this.category;
        }

        @Override
        public Severity getSeverity() {
            return this.severity;
        }

        @Override
        public int getNumber() {
            return this.number;
        }
    }

    @JsonCreator
    public Messages( //
            @JsonProperty(value = "category", required = true) String cateogry, //
            @JsonProperty(value = "severity", required = true) String severity, //
            @JsonProperty(value = "number", required = true) int number, //
            @JsonProperty(value = "text", required = false) String text //
    ) {
        super(Categories.tryParse(cateogry), Severities.tryParse(severity), number, text);
    }

    public Messages(Message message) {
        super(message.getCategory(), message.getSeverity(), message.getNumber(), message.getText());
    }
}
