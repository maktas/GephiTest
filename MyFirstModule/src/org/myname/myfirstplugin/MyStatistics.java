/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.myname.myfirstplugin;

import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Graph;
import org.gephi.statistics.plugin.*;
import org.openide.util.lookup.ServiceProvider;
import org.gephi.statistics.spi.*;


/**
 *
 * @author Maktas
 */
@ServiceProvider (service = StatisticsBuilder.class)
public class MyStatistics implements StatisticsBuilder{

    String name = "Brand New Statistics Module";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Statistics getStatistics() {
        return new MyMetric();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return MyMetric.class;

    }
}