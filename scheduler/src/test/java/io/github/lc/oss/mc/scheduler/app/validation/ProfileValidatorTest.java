package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.app.model.Profile;
import io.github.lc.oss.mc.validation.CommandValidator;

public class ProfileValidatorTest extends AbstractValidatorTest {
    @Mock
    private CommandValidator commandValidator;

    @InjectMocks
    private ProfileValidator validator;

    @Test
    public void test_null() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("Profile");

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_missingValues() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("settings.profiles.profile");
        this.expectFieldVar("ffmpeg.ext");

        Profile profile = new Profile();
        profile.setSliceLength(1);

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList( //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing), result);
    }

    @Test
    public void test_sliceLengthTooSmall() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidField);
        this.expectFieldVar("ffmpeg.video.sliceLength");

        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(0);

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList(Messages.Application.InvalidField), result);
    }

    @Test
    public void test_sliceLengthTooSmall_v2() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidField);
        this.expectFieldVar("ffmpeg.video.sliceLength");

        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(-1000);

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList(Messages.Application.InvalidField), result);
    }

    @Test
    public void test_sliceLengthTooLarge() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidField);
        this.expectFieldVar("ffmpeg.video.sliceLength");

        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(999999999 + 1);

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList(Messages.Application.InvalidField), result);
    }

    @Test
    public void test_invalidFields() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidField);
        this.expectFieldVar("settings.profiles.profile");
        this.expectFieldVar("ffmpeg.ext");

        Profile profile = new Profile();
        profile.setExt(".ext");
        profile.setName("#junit");
        profile.setSliceLength(999999999);

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList( //
                Messages.Application.InvalidField, //
                Messages.Application.InvalidField), result);
    }

    @Test
    public void test_nothingToProcess() {
        this.expectLocale();
        this.expectMessage(Messages.Application.NothingToProcess);

        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(1000);
        profile.setAudioArgs(Arrays.asList("-an"));
        profile.setVideoArgs(Arrays.asList("-vn"));

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList(Messages.Application.NothingToProcess), result);
    }

    @Test
    public void test_nothingToProcess_v2() {
        this.expectLocale();
        this.expectMessage(Messages.Application.NothingToProcess);

        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(1000);
        profile.setAudioArgs(null);
        profile.setVideoArgs(new ArrayList<>());
        profile.setCommonArgs(Arrays.asList("-an", "-vn"));

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList(Messages.Application.NothingToProcess), result);
    }

    @Test
    public void test_withTrimFilter() {
        this.expectLocale();
        this.expectMessage(Messages.Application.ArgsIncludesTrimFilter);

        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(1000);
        profile.setVideoArgs(Arrays.asList("-c:v", "copy", "-vf", "trim="));

        Set<Message> result = this.validator.validate(profile);

        this.assertMessages(Arrays.asList(Messages.Application.ArgsIncludesTrimFilter), result);
    }

    @Test
    public void test_valid() {
        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(1000);
        // in audio processing -vn is enforced, adding it here means nothing and is OK
        profile.setAudioArgs(Arrays.asList("-vn", "-c:a", "copy"));
        // in video processing -an is enforced, adding it here means nothing and is OK
        profile.setVideoArgs(Arrays.asList("-an", "-c:v", "copy", "-vf", "scale=1920:1080"));

        Set<Message> result = this.validator.validate(profile);

        this.assertValid(result);
    }

    @Test
    public void test_valid_videoOnly() {
        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(1000);
        profile.setAudioArgs(Arrays.asList("-an"));
        profile.setVideoArgs(Arrays.asList("-c:v", "copy", "-vf", "scale=1920:1080"));

        Set<Message> result = this.validator.validate(profile);

        this.assertValid(result);
    }

    @Test
    public void test_valid_audioOnly() {
        Profile profile = new Profile();
        profile.setExt("ext");
        profile.setName("junit");
        profile.setSliceLength(1000);
        profile.setAudioArgs(Arrays.asList("-c:a", "copy"));
        profile.setVideoArgs(Arrays.asList("-vn"));

        Set<Message> result = this.validator.validate(profile);

        this.assertValid(result);
    }
}
