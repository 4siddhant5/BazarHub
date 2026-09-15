package com.siddhant.bazarhub;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdminList {
    // Add your admin emails (lowercase). You can edit anytime.
    private static final Set<String> ADMINS = new HashSet<>(Arrays.asList(
            "admin@bazarhub.demo",
            "siddhant@bazarhub.demo"
    ));

    public static boolean isAdminEmail(String email) {
        if (email == null) return false;
        return ADMINS.contains(email.trim().toLowerCase());
    }
}
