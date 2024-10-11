package io.github.lc.oss.mc.api;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProfileTest extends AbstractApiEntityTest {
    @Test
    public void test_getset() {
        Profile profile = new Profile();

        Assertions.assertNull(profile.getAudioArgs());
        Assertions.assertNull(profile.getCommonArgs());
        Assertions.assertNull(profile.getExt());
        Assertions.assertNull(profile.getName());
        Assertions.assertNull(profile.getSliceLength());
        Assertions.assertNull(profile.getVideoArgs());

        Assertions.assertTrue(profile.hasAudio());
        Assertions.assertTrue(profile.hasVideo());

        profile.setAudioArgs(Arrays.asList("-b:a", "128k"));
        profile.setCommonArgs(Arrays.asList("--common"));
        profile.setExt("ext");
        profile.setName("name");
        profile.setSliceLength(99999999);
        profile.setVideoArgs(Arrays.asList("-c:v", "mjpeg"));

        Assertions.assertEquals(2, profile.getAudioArgs().size());
        Assertions.assertEquals(1, profile.getCommonArgs().size());
        Assertions.assertEquals("--common", profile.getCommonArgs().iterator().next());
        Assertions.assertEquals("ext", profile.getExt());
        Assertions.assertEquals("name", profile.getName());
        Assertions.assertEquals(99999999, profile.getSliceLength());
        Assertions.assertEquals(2, profile.getVideoArgs().size());

        Assertions.assertTrue(profile.hasAudio());
        Assertions.assertTrue(profile.hasVideo());

        profile.setCommonArgs(null);
        profile.setVideoArgs(null);
        profile.setAudioArgs(null);

        Assertions.assertNull(profile.getAudioArgs());
        Assertions.assertNull(profile.getCommonArgs());
        Assertions.assertNull(profile.getVideoArgs());
    }

    @Test
    public void test_altConstructor() {
        AbstractEntity base = new TestEntity();

        Profile profile = new Profile(base, "name", "ext", 100, Arrays.asList("-an"), Arrays.asList("-vn"),
                Arrays.asList("-common"));

        Assertions.assertEquals(1, profile.getAudioArgs().size());
        Assertions.assertEquals("-an", profile.getAudioArgs().iterator().next());
        Assertions.assertEquals(1, profile.getCommonArgs().size());
        Assertions.assertEquals("-common", profile.getCommonArgs().iterator().next());
        Assertions.assertEquals("ext", profile.getExt());
        Assertions.assertEquals("name", profile.getName());
        Assertions.assertEquals(100, profile.getSliceLength());
        Assertions.assertEquals(1, profile.getVideoArgs().size());
        Assertions.assertEquals("-vn", profile.getVideoArgs().iterator().next());

        Assertions.assertFalse(profile.hasAudio());
        Assertions.assertFalse(profile.hasVideo());
    }
}
