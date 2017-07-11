package com.nixmash.userservice.service;

import com.google.inject.Inject;
import com.nixmash.jangles.core.JanglesCache;
import com.nixmash.jangles.core.JanglesConfiguration;
import com.nixmash.jangles.model.JanglesUser;
import com.nixmash.userservice.db.UserSqlImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    JanglesCache janglesCache;
    JanglesConfiguration janglesConfiguration;
    UserSqlImpl userSql;

    @Inject
    public UserServiceImpl(UserSqlImpl userSql, JanglesCache janglesCache, JanglesConfiguration janglesConfiguration) {
        this.userSql = userSql;
        this.janglesCache = janglesCache;
        this.janglesConfiguration = janglesConfiguration;
    }

    @Override
    public List<JanglesUser> getJanglesUsers() {
        return getJanglesUsers(true);
    }

    @Override
    public List<JanglesUser> getJanglesUsers(boolean useCached) {

        String key = janglesUsersCacheKey();

        @SuppressWarnings("unchecked")
        List<JanglesUser> janglesUsers = (List<JanglesUser>) janglesCache.get(key);
        if (janglesUsers == null || !useCached) {
            try {
                janglesUsers = userSql.getJanglesUsers();
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
            janglesCache.put(key, (Serializable) janglesUsers);
        }
        return janglesUsers;
    }

    @Override
    public JanglesUser getJanglesUser(Long userId) {
        JanglesUser janglesUser = null;
        try {
            janglesUser = userSql.getJanglesUser(userId);
        } catch (SQLException e) {
            logger.error("JanglesUser with id " + userId + " not found!");
            return null;
        }
        janglesUser.setShowUsersLink(true);
        return janglesUser;
    }

    @Override
    public JanglesUser createJanglesUser(JanglesUser janglesUser) {
        Long userId = -1L;
        userId = userSql.createJanglesUser(janglesUser);
        janglesCache.remove(janglesUsersCacheKey());
        return getJanglesUser(userId);
    }

    private String janglesUsersCacheKey() {
        return String.format("JanglesUserList-%s", janglesConfiguration.applicationId);
    }


}
