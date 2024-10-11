package io.github.lc.oss.mc.scheduler.security;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;

public class PermissionsTest extends AbstractMockTest {
    @Test
    public void test_caching() {
        Set<Permissions> all = new HashSet<>(Permissions.all());

        for (Permissions p : Permissions.values()) {
            Assertions.assertTrue(all.remove(p));
            Assertions.assertTrue(Permissions.hasName(p.name()));
            Assertions.assertSame(p, Permissions.byName(p.name()));
            Assertions.assertSame(p, Permissions.tryParse(p.name()));
        }
        Assertions.assertTrue(all.isEmpty());
    }

    @Test
    public void test_byPermission() {
        try {
            Permissions.byPermission(null);
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("No enum constant Permissions.null", ex.getMessage());
        }

        try {
            Permissions.byPermission("");
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("No enum constant Permissions.", ex.getMessage());
        }

        try {
            Permissions.byPermission(" \t \r \n \t ");
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("No enum constant Permissions. \t \r \n \t ", ex.getMessage());
        }

        try {
            Permissions.byPermission("junk");
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("No enum constant Permissions.junk", ex.getMessage());
        }

        Assertions.assertNotEquals(Permissions.User.name(), Permissions.User.getPermission());
        try {
            Permissions.byPermission(Permissions.User.name());
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("No enum constant Permissions.User", ex.getMessage());
        }

        Permissions result = Permissions.byPermission(Permissions.User.getPermission());
        Assertions.assertSame(Permissions.User, result);
    }
}
