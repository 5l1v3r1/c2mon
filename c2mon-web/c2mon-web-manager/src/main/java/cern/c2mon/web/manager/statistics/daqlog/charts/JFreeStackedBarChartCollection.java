package cern.c2mon.web.manager.statistics.daqlog.charts;

import java.sql.SQLException;
import java.util.List;

import cern.c2mon.web.manager.statistics.daqlog.values.IChartCollectionValue;

public class JFreeStackedBarChartCollection extends JFreeWebChartCollection {
    /**
     * Set the chart class at initialisation.
     */
    public JFreeStackedBarChartCollection() {
        super();
        chartClass = JFreeStackedBarChart.class;
    }


    @Override
    public List<IChartCollectionValue> getValuesFromDatabase(String tableName) throws SQLException {
        //retrieve the chart values from the database
        return mapper.getStackedBarChartCollectionData(tableName);
    }
}