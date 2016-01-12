/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package cern.c2mon.configloader.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import cern.c2mon.configloader.C2MonConfigLoaderConfig;
import cern.c2mon.configloader.Configuration;
import cern.c2mon.configloader.dao.ConfigLoaderDAO;

/**
 * This class encapsulates all db-access operations performed by C2MON configuration loader
 * 
 * @author wbuczak
 */
public class ConfigLoaderDaoImpl implements ConfigLoaderDAO {

    protected JdbcTemplate jdbcTemplate;

    static final String SQL_DEFAULT_GET_CONFIG_QUERY = "select configid, configname, configdesc, author, createdate, applydate "
            + "from %table% where applydate is null order by configid";

    static final String SQL_DEFAULT_UPDATE_CONFIG_QUERY = "update %table% set applied_by=? where configid=?";

    @Resource
    protected C2MonConfigLoaderConfig config;

    private String dbQuery;

    private String dbUpdateQuery;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    private void init() {
        dbQuery = config.getDbConfigQuery() == null ? SQL_DEFAULT_GET_CONFIG_QUERY : config.getDbConfigQuery();
        dbQuery = dbQuery.replace("%table%", config.getDbConfigTableName());

        dbUpdateQuery = config.getDbUpdateConfigQuery() == null ? SQL_DEFAULT_UPDATE_CONFIG_QUERY : config
                .getDbUpdateConfigQuery();
        dbUpdateQuery = dbUpdateQuery.replace("%table%", config.getDbConfigTableName());
    }

    /**
     * returns list of configurations which are not yet applied to the server
     * 
     * @return list of config ids
     */
    @Override
    public List<Configuration> getConfigurationsForLoading() {

        RowMapper<Configuration> mapper = new RowMapper<Configuration>() {
            @Override
            public Configuration mapRow(ResultSet rs, int arg1) throws SQLException {
                Configuration conf = new Configuration(rs.getLong("configid"), rs.getString("configname").trim(), rs
                        .getString("configdesc").trim(), rs.getString("author").trim(), rs.getTimestamp("createdate")
                        .getTime(), rs.getTimestamp("applydate") == null ? Configuration.TIMESTAMP_NOT_SET : rs
                        .getTimestamp("applydate").getTime());

                return conf;
            }
        };

        return this.jdbcTemplate.query(dbQuery, mapper);
    }

    @Override
    public void update(final long configId, final String userName) {
        jdbcTemplate.update(dbUpdateQuery, userName, configId);
    }
}
