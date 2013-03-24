/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.myname.myfirstplugin;
import org.gephi.graph.api.GraphModel;
import org.gephi.statistics.spi.*;
import org.gephi.data.attributes.api.*;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

/**
 *
 * @author Maktas
 */
public class MyMetric implements Statistics{
    private String report = "<HTML> <BODY> <h1>Hello World </h1>" +
                            "<br /> This is a dummy statistics module for test purposes</BODY> </HTML>";
    
 private boolean cancel = false;
 private ProgressTicket progressTicket;

    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {

   Graph graph = graphModel.getGraphVisible();
   graph.readLock();
 
   try {
      Progress.start(progressTicket, graph.getNodeCount());
 
      for (Node n : graph.getNodes()) {
         //do something
         Progress.progress(progressTicket);
         if (cancel) {
            break;
         }
      }
      graph.readUnlockAll();
   } catch (Exception e) {
      e.printStackTrace();
      //Unlock graph
      graph.readUnlockAll();
   }
 }

    @Override
    public String getReport() {
        return report;
    }
    
     public boolean cancel() {
   cancel = true;
   return true;
 }
 
 public void setProgressTicket(ProgressTicket progressTicket) {
   this.progressTicket = progressTicket;
 }
}
