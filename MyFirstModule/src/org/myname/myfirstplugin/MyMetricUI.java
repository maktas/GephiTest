/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.myname.myfirstplugin;
import javax.swing.JPanel;
import org.gephi.statistics.spi.*;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Maktas
 */
@ServiceProvider(service = StatisticsUI.class)
public class MyMetricUI implements StatisticsUI{
    
    private MyMetric statistics;

    @Override
    public JPanel getSettingsPanel() {
        return null;
    }

    @Override
    public void setup(Statistics ststcs) {
        this.statistics = (MyMetric) ststcs;
    }

    @Override
    public void unsetup() {
        this.statistics = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
         return MyMetric.class;

    }

    @Override
    public String getValue() {
        return "Nothing to Show";
    }

    @Override
    public String getDisplayName() {
    return "My Metric";
    }

    @Override
    public String getShortDescription() {
        return "This is a dummy statistics plugin for testing purposes";
    }

    @Override
    public String getCategory() {
    return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
    }

    @Override
    public int getPosition() {
        return 1000;

    }
    
}
