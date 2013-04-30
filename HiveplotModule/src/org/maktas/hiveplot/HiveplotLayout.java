
package org.maktas.hiveplot;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeData;
import org.gephi.layout.plugin.AbstractLayout;
import org.gephi.layout.plugin.ForceVectorNodeLayoutData;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutProperty;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.ui.propertyeditor.NodeColumnNumbersEditor;

public class HiveplotLayout extends AbstractLayout implements Layout
{
    boolean executing;
    private float canvasArea;           // Set boundary for node placement.
    private int numAxes;                // Total number of axes.
    private String parameter1="";          // Parameter for the nodes on the first axis
    private String parameter2="";          // Parameter for the nodes on the second axis
    private String parameter3="";          // Parameter for the nodes on the third axis
    private String parameter4="";          // Parameter for the nodes on the fourth axis
    private String parameter5="";          // Parameter for the nodes on the fifth axis
    protected Graph graph;              // The graph being laid out.
    private AttributeColumn nodeOrder;
    private AttributeColumn axesOrder;
    private int[] nodeAxis;             // The array for storing which axis the node belongs to
    private double[] nodeLocation;             // The array for storing which axis the node belongs to


    /**
     * @param layoutBuilder 
     */
    public HiveplotLayout(HiveplotLayoutBuilder layoutBuilder)
    {
        super(layoutBuilder);
        this.canvasArea = 5000;
        this.numAxes = 3;
    }
    
    @Override
    public void initAlgo()
    {
        executing = true;
        this.graph = graphModel.getGraphVisible();
        for (Node n : graph.getNodes())
            n.getNodeData().setLayoutData(new ForceVectorNodeLayoutData());
        /*Renderer mr;
        
        PreviewProperty properties;
                properties = new PreviewProperty(this.mr,
                PreviewProperty.EDGE_CURVED,
                Boolean.class,
                "Curved Edges",
                "The edges will be curved",
                PreviewProperty.EDGE_CURVED).setValue(true);*/
    }
    
    @Override
    public void goAlgo() 
    {
        this.graph.readLock();
        nodeAxis = new int[this.graph.getNodeCount()+1];
        boolean axisSort = this.nodeOrder != null && !this.nodeOrder.getId().equals("");
        double degree = 360/this.numAxes;                       // angle between axes
        float coeff = (float)(50.0/canvasArea);
        float value;
        double dvalue= 0;
        Double min, max;
        double dmin, dmax;
        Node[] nodes = this.graph.getNodes().toArray();

        List<Node[]> sortNodes = generateAxes(axisSort, false);  // Axes 
        Point2D.Float[] p = new Point2D.Float[this.numAxes];    // Max points for each axis
        Point2D.Float[] z = new Point2D.Float[this.numAxes];    // Next node position to draw
        Point2D.Float[] d = new Point2D.Float[this.numAxes];    // Absolute values of max points for each axis
        
        // Calculate outer boundary points for all axes - centered on (0,0)
        for(int x = 0; x < this.numAxes; x++) {
            p[x] = new Point2D.Float(StrictMath.round(this.canvasArea * (Math.cos(Math.toRadians(degree * (x + 1))))),
                                     StrictMath.round(this.canvasArea * (Math.sin(Math.toRadians(degree * (x + 1))))));
            d[x] = new Point2D.Float(Math.abs(p[x].x), Math.abs(p[x].y));
        }   
        z = p;
        
        Node node = nodes[0];
        
        for(Node[] groups : sortNodes)
        {
            int pos = sortNodes.indexOf(groups);
            float ratio = (float) 0.75;
            float denom = (float) 0.5;

            if(node.getAttributes().getValue(this.axesOrder.getIndex()).getClass().getName().contentEquals("java.lang.String")){
                dmin = nodes.length;
                dmax = 0;
            }
            else if(this.axesOrder.getTitle().contentEquals("Degree")){
                dmin = (double) this.graph.getDegree(groups[groups.length-1]);
                dmax = (double) this.graph.getDegree(groups[0]);
            }
            else{
                min = (Double) groups[groups.length-1].getAttributes().getValue(axesOrder.getIndex());
                dmin = (double) min;
                max = (Double) groups[0].getAttributes().getValue(axesOrder.getIndex());
                dmax = (double) max;
            }
            
           if(dmax != dmin){ 
                    denom = ((float)dmax-(float)dmin)*(float)0.5 / ((float)dmax-(float)dmin + (float)Math.cbrt(dmax));
                }
            
            for (Node n : groups)
            {
                if(node.getAttributes().getValue(this.axesOrder.getIndex()).getClass().getName().contentEquals("java.lang.String")){
                    dvalue += 1;
                    value = (float) dvalue;
                }
                else if(this.axesOrder.getTitle().contentEquals("Degree")){
                dvalue = (double) this.graph.getDegree(n);
                value = (float) dvalue;
                }
                else{
                dvalue = (Double) n.getAttributes().getValue(axesOrder.getIndex());
                value = (float) dvalue;
                }
                
                if(dmax != dmin){ 
                    ratio = (value-(float)dmin + (float)Math.cbrt(dmax) ) / ((float)dmax-(float)dmin + (float)Math.cbrt(dmax));
                }
                
                z[pos] = new Point2D.Float((z[pos].x > 0 ? d[pos].x * ratio : -d[pos].x * ratio),
                                           (z[pos].y > 0 ? d[pos].y * ratio : -d[pos].y * ratio));

                n.getNodeData().setX(z[pos].x);
                n.getNodeData().setY(z[pos].y);
            }
            
            //if two nodes are in the same location slide one of them just a little so that each one could be seen
            for(Node n:groups){
                float x = n.getNodeData().x();
                float y = n.getNodeData().y();
                
                for(int i=0;i <groups.length; i++){
                    if(n.getId()!= groups[i].getId() && x == groups[i].getNodeData().x()){
                        n.getNodeData().setX(x > 0 ? n.getNodeData().x() + denom*d[pos].x*coeff : n.getNodeData().x() - denom*d[pos].x*coeff);
                        n.getNodeData().setY(y > 0 ? n.getNodeData().y() + denom*d[pos].y*coeff : n.getNodeData().y() - denom*d[pos].y*coeff);
                    }
                }
            }

        }

        this.graph.readUnlock();
        endAlgo();
    }


    /**
     * 
     */
    @Override
    public void endAlgo() 
    {
        for (Node n : graph.getNodes()){
            NodeData data = n.getNodeData();
            data.setLayoutData(null);
        }
        executing = false;
    }
    
    @Override
    public boolean canAlgo() 
    {
        return executing;
    }
    

    /**
     * @return 
     */
    @Override
    public LayoutProperty[] getProperties() 
    {
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        final String HIVEPLOT  = "Hiveplot Layout";

        try 
        {
            properties.add(LayoutProperty.createProperty(
                    this, AttributeColumn.class,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.axisAssign.name"),
                    HIVEPLOT,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.axisAssign.desc"),
                    "getColumn", "setColumn", NodeColumnNumbersEditor.class));
            properties.add(LayoutProperty.createProperty(
                    this, AttributeColumn.class,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.axisOrder.name"),
                    HIVEPLOT,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.axisOrder.desc"),
                    "getAxesColumn", "setAxesColumn", NodeColumnNumbersEditor.class));
            properties.add(LayoutProperty.createProperty(
                    this, Float.class,
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.area.name"),
                    HIVEPLOT,
                    "hiveplot.area.name",
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.area.desc"),
                    "getCanvasArea", "setCanvasArea"));
            properties.add(LayoutProperty.createProperty(
                    this, Integer.class,
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.numAxes.name"),
                    HIVEPLOT,
                    "hiveplot.numAxes.name",
                    NbBundle.getMessage(HiveplotLayout.class, "hiveplot.numAxes.desc"),
                    "getNumAxes", "setNumAxes"));
           properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter1.name"),
                    HIVEPLOT,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter1.desc"),
                    "getParameter1", "setParameter1"));
           properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter2.name"),
                    HIVEPLOT,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter2.desc"),
                    "getParameter2", "setParameter2"));
           properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter3.name"),
                    HIVEPLOT,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter3.desc"),
                    "getParameter3", "setParameter3"));
           properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter4.name"),
                    HIVEPLOT,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter4.desc"),
                    "getParameter4", "setParameter4"));
           properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter5.name"),
                    HIVEPLOT,
                    NbBundle.getMessage(HiveplotLayout.class,"hiveplot.parameter5.desc"),
                    "getParameter5", "setParameter5"));
        } 
        catch (MissingResourceException e)
        {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Exceptions.printStackTrace(e);
        }
        
        return properties.toArray(new LayoutProperty[0]);
    }


    /**
     * Resets the value of the layout attributes.
     */
    @Override
    public void resetPropertiesValues()
    {
        this.canvasArea = 5000;
        this.numAxes = 3;
    }
    
    /**
     * 
     * @return 
     */
    private List<Node[]> generateAxes(boolean sortNodesOnAxis, boolean asc)
    {
        ArrayList<Node[]> nodeGroups = new ArrayList<Node[]>();
        Node[] n = this.graph.getNodes().toArray();
        Node node = n[0];
        
        if (nodeOrder != null && !node.getAttributes().getValue(this.nodeOrder.getIndex()).getClass().getName().contentEquals("java.lang.String")) {
            Arrays.sort(n, new Comparator<Node>() {

                @Override
                public int compare(Node o1, Node o2) {
                    Number n1 = (Number) o1.getAttributes().getValue(nodeOrder.getIndex());
                    Number n2 = (Number) o2.getAttributes().getValue(nodeOrder.getIndex());
                    
                    if (n1.doubleValue() < n2.doubleValue()) {
                        return -1;
                    } else if (n1.doubleValue() > n2.doubleValue()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }

        int[] order = findBins(n);
        int lowerBound = 0;
        int upperBound = order[0];
        
        for(int bin = 0; bin < numAxes; bin++){
            nodeGroups.add((lowerBound != upperBound) ? Arrays.copyOfRange(n, lowerBound, upperBound) : Arrays.copyOf(n, lowerBound));
            lowerBound = upperBound;
            if(bin < (order.length - 1))
            {
                upperBound = lowerBound + order[bin + 1];
            }
        }
        
        if(sortNodesOnAxis)
        {
            ArrayList<Node[]> nodeGroupsAxis = new ArrayList<Node[]>();
            for(Node[] ng : nodeGroups)
            {
                
                if (!node.getAttributes().getValue(this.axesOrder.getIndex()).getClass().getName().contentEquals("java.lang.String")) {
                Arrays.sort(ng, new Comparator<Node>() {

                @Override
                public int compare(Node o1, Node o2) {
                    Number n1 = (Number) o1.getAttributes().getValue(axesOrder.getIndex());
                    Number n2 = (Number) o2.getAttributes().getValue(axesOrder.getIndex());
                    if (n1.doubleValue() < n2.doubleValue()) {
                        return 1;
                    } else if (n1.doubleValue() > n2.doubleValue()) {
                        return -1;
                    } else {
                        return 0;
                    }
                  }
                });
              }
                
                nodeGroupsAxis.add(ng);
            }
            return nodeGroupsAxis;
        }
        else
        {
            return nodeGroups;
        }
    }
    
    /**
     * Generates an array of integers which represent bin cut-offs for sorted array.
     * @param nodes
     * @return 
     */
    private int[] findBins(Node[] nodes)
    {
        int totalBins = this.numAxes;
        int[] bins = new int[totalBins];
        int binIndex = 0;
        Double value;
        String svalue;
        boolean isString;
        isString = nodes[0].getAttributes().getValue(this.nodeOrder.getIndex()).getClass().getName().contentEquals("java.lang.String");
        
        for (Node n : nodes)
        {
            if(isString){
                svalue = (String) n.getAttributes().getValue(this.nodeOrder.getIndex());
                if(svalue.toLowerCase().contentEquals(parameter1)) binIndex=0;
                else if(svalue.toLowerCase().contentEquals(parameter2)) binIndex=1;
                else if(svalue.toLowerCase().contentEquals(parameter3)) binIndex=2;
                else if(svalue.toLowerCase().contentEquals(parameter4)) binIndex=3;
                else if(svalue.toLowerCase().contentEquals(parameter5)) binIndex=4;
                else binIndex=numAxes-1;
            }
            else{
                if(this.nodeOrder.getTitle().contentEquals("Degree")){
                    value = (double) this.graph.getDegree(n);
                }
                else{
                value = (Double) n.getAttributes().getValue(nodeOrder.getIndex());
                }

              if(value <= Double.valueOf(parameter1)){
                binIndex = 0;
                }
              else if(value <= Double.valueOf(parameter2)){
                binIndex = 1;
                }
              else if(numAxes > 3 && value <= Double.valueOf(parameter3)){
                binIndex = 2;
                }
              else if(numAxes > 4 && value <= Double.valueOf(parameter4)){
                binIndex = 3;
                }
              else if(numAxes > 5 && value <= Double.valueOf(parameter5)){
                binIndex = 4;
                }
              else binIndex = totalBins-1;
            }
            nodeAxis[n.getId()-1] = binIndex;
            System.out.println(n.getId());
            System.out.println(this.graph.getNodeCount());
            bins[binIndex]++;
        }
        
        return(bins);
    }


    // Accessors 


    /**
     * 
     * @return 
     */
    public int getNumAxes()
    {
     return this.numAxes;   
    }
    
    /**
     * 
     * @param numAxes 
     */
    public void setNumAxes(Integer numAxes)
    {
        this.numAxes = numAxes;
    }

    
    /**
     * 
     * @return 
     */
    public float getCanvasArea()
    {
        return this.canvasArea;
    }
    
    /**
     * 
     * @param area 
     */
    public void setCanvasArea(Float canvasArea)
    {
        this.canvasArea = canvasArea;
    }
    
    public AttributeColumn getColumn() {
        return nodeOrder;
    }

    public void setColumn(AttributeColumn column) {
        this.nodeOrder = column;
    }
    
    public AttributeColumn getAxesColumn() {
        return axesOrder;
    }

    public void setAxesColumn(AttributeColumn column) {
        this.axesOrder = column;
    }
    
    public String getParameter1() {
        return parameter1;
    }

    public void setParameter1(String parameter) {
        this.parameter1 = parameter;
    }

    public String getParameter2() {
        return parameter2;
    }

    public void setParameter2(String parameter) {
        this.parameter2 = parameter;
    }

    public String getParameter3() {
        return parameter3;
    }

    public void setParameter3(String parameter) {
        this.parameter3 = parameter;
    }
    
    public String getParameter4() {
        return parameter4;
    }

    public void setParameter4(String parameter) {
        this.parameter4 = parameter;
    }
    
    public String getParameter5() {
        return parameter5;
    }

    public void setParameter5(String parameter) {
        this.parameter5 = parameter;
    }
}
