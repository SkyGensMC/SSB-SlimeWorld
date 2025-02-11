package com.skygensmc.ssb.slimeworld.data;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

public interface DataSourceParams {

    DataSourceType getType();

    static DataSourceParams parse(ConfigurationSection section) throws IllegalArgumentException {
        String type = section.getString("type");
        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("Please specify a data source type!");
        }

        try {
            DataSourceType dataSourceType = DataSourceType.valueOf(type.toUpperCase());
            return switch (dataSourceType) {
                case API -> new API(section.getConfigurationSection("api"));
                case MYSQL -> new MySQL(section.getConfigurationSection("mysql"));
                case MONGODB -> new MongoDB(section.getConfigurationSection("mongodb"));
                case FILE -> new File(section.getConfigurationSection("file"));
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot handle data source type " + type);
        }

    }

    class API implements DataSourceParams {

        public final String username;
        public final String token;
        public final String uri;
        public final boolean ignoreSSLCertificate;

        private API(ConfigurationSection section) {
            this.username = section.getString("username", "");
            this.token = section.getString("token", "");
            this.uri = section.getString("uri", "");
            this.ignoreSSLCertificate = section.getBoolean("ignore-ssl-certificate", false);
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.API;
        }
    }

    class MySQL implements DataSourceParams {

        public final String url;
        public final String host;
        public final int port;
        public final String username;
        public final String password;
        public final String database;
        public final boolean useSSL;

        private MySQL(ConfigurationSection section) {
            this.url = section.getString("url", "jdbc:mysql://{host}:{port}/{database}?autoReconnect=true&allowMultiQueries=true&useSSL={usessl}");
            this.host = section.getString("host", "127.0.0.1");
            this.port = section.getInt("port", 3306);
            this.username = section.getString("username", "");
            this.password = section.getString("password", "");
            this.database = section.getString("database", "");
            this.useSSL = section.getBoolean("useSSL", false);
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.MYSQL;
        }
    }

    class MongoDB implements DataSourceParams {

        public final String url;
        public final String host;
        public final int port;
        public final String auth;
        public final String username;
        public final String password;
        public final String database;
        public final String collection;

        private MongoDB(ConfigurationSection section) {
            this.url = section.getString("url", "mongodb://{username}:{password}@{host}:{port}/");
            this.host = section.getString("host", "127.0.0.1");
            this.port = section.getInt("port", 27017);
            this.auth = section.getString("auth", "");
            this.username = section.getString("username", "");
            this.password = section.getString("password", "");
            this.database = section.getString("database", "");
            this.collection = section.getString("collection", "");
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.MONGODB;
        }
    }

    class File implements DataSourceParams {

        public final String path;

        private File(ConfigurationSection section) {
            this.path = section.getString("path", "slime_worlds");
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.FILE;
        }
    }

}
