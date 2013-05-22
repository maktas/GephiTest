
package org.maktas.hiveplot;

import java.awt.geom.Point2D;
import java.util.*;
import javax.swing.JOptionPane;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
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
    boolean executing = false;
    private float canvasArea;           // Set boundary for node placement.
    private int numAxes;                // Total number of axes.
    private double absmin=0, absmax=0;  //Smallest and biggest node values
    private String[] parameter= new String[10];          // Parameter array for the inputs
    protected Graph graph;              // The graph being laid out.
    private String nodeOrder;
    private String axisOrder;
    private int[] nodeAxis;             // The array for storing which axis the node belongs to
    private double[] nodeLocation;             // The array for storing which axis the node belongs to
    boolean isString;
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

    }
    
    @Override
    public void goAlgo() 
    {
        this.graph.readLock();
        nodeAxis = new int[this.graph.getNodeCount()+1];
        boolean axisSort = this.axisOrder != null && !this.axisOrder.contentEquals("");
        double degree = 360/this.numAxes;                       // angle between axes
        float coeff = (float)(50.0/canvasArea);
        float value;
        double dvalue= 0;
        Double min, max;
        double dmin, dmax;
        Node[] nodes = this.graph.getNodes().toArray();
        
       this.setAxisColumn(axisOrder);

      try{
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
            float denom = (float) 1.0;

            if(node.getAttributes().getValue(this.axisOrder).getClass().getName().contentEquals("java.lang.String")){
                dmin = 0;
                dmax = groups.length;
            }
            
            else if(this.axisOrder.toLowerCase().contentEquals("degree")){
                dmin = (double) this.graph.getDegree(groups[groups.length-1]);
                dmax = (double) this.graph.getDegree(groups[0]);
            }
            else{
                min = Double.valueOf(groups[groups.length-1].getAttributes().getValue(axisOrder).toString());
                dmin = (double) min;
                max = Double.valueOf(groups[0].getAttributes().getValue(axisOrder).toString());
                dmax = (double) max;
            }
            
           if(dmax != dmin){ 
                    denom = ((float)dmax-(float)dmin)*(float)0.5 / ((float)dmax-(float)dmin + (float)Math.cbrt(dmax));
                }
                       
            for (Node n : groups)
            {
                if(node.getAttributes().getValue(this.axisOrder).getClass().getName().contentEquals("java.lang.String")){
                    dvalue += 1;
                    value = (float) dvalue;
                }
                else if(this.axisOrder.equalsIgnoreCase("Degree")){
                dvalue = (double) this.graph.getDegree(n);
                value = (float) dvalue;
                }
                else{
                dvalue = Double.valueOf(n.getAttributes().getValue(axisOrder).toString());
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
            
            //If two nodes are in the same location slide one of them just a little so that each one could be seen
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
        
      }
      catch (NullPointerException e)
      {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,"Please select the ordering properties!");
      }
      catch(IndexOutOfBoundsException e){
            e.printStackTrace();
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
        /*final String HIVEPLOT  = "Hiveplot Layout";
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
                    "getAxisColumn", "setAxisColumn", NodeColumnNumbersEditor.class));
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
        */        
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
        
        isString = node.getAttributes().getValue(this.nodeOrder).getClass().getName().contentEquals("java.lang.String");
        
        if (nodeOrder != null && !isString) {
            
            Arrays.sort(n, new Comparator<Node>() {

                @Override
                public int compare(Node o1, Node o2) {
                    
                    Double n1 = Double.valueOf(o1.getAttributes().getValue(nodeOrder).toString());
                    Double n2 = Double.valueOf(o2.getAttributes().getValue(nodeOrder).toString());
                    
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
                Arrays.sort(ng, new Comparator<Node>() {

                @Override
                public int compare(Node o1, Node o2) {
                    Number n1 = Double.valueOf(o1.getAttributes().getValue(axisOrder).toString());
                    Number n2 = Double.valueOf(o2.getAttributes().getValue(axisOrder).toString());
                    if (n1.doubleValue() < n2.doubleValue()) {
                        return 1;
                    } else if (n1.doubleValue() > n2.doubleValue()) {
                        return -1;
                    } else {
                        return 0;
                    }
                  }
                });

                
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
        Integer ivalue;
        String svalue;
        
        try{
            
        for (Node n : nodes)
        {
            if(isString){
                svalue = (String) n.getAttributes().getValue(this.nodeOrder);
              for(int i=0; i<numAxes; i++){
                if( svalue.toLowerCase().contentEquals(parameter[i])) 
                    binIndex=i;
                }
            }
            else{
                if(this.nodeOrder.equalsIgnoreCase("Degree")){
                    value = (double) this.graph.getDegree(n);
                }
                else if(n.getAttributes().getValue(nodeOrder).getClass().getName().contentEquals("java.lang.Integer")){
                    ivalue = (Integer) n.getAttributes().getValue(nodeOrder);
                    value = ivalue.doubleValue();
                }
                else{
                value = Double.valueOf(n.getAttributes().getValue(nodeOrder).toString());
                }

              for(int i=0; i<numAxes-1; i++){
                if(value <= Double.valueOf(parameter[i])){
                    binIndex = i;
                    break;
                }
                else binIndex = numAxes-1;
              }
            
            int pos = Integer.valueOf((String)n.getNodeData().getAttributes().getValue("Id"));
            nodeAxis[pos] = binIndex;
            bins[binIndex]++;
            }
          }
        }
        catch(NumberFormatException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,"Please enter the parameters correctly!");
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
    
    public String getColumn() {
        return nodeOrder;
    }

    public void setColumn(String column) {
        this.nodeOrder = column;
    }
    
    public String getAxisColumn() {
        return axisOrder;
    }

    public void setAxisColumn(String column) {
        this.axisOrder = column;
    }
    
    public String getParameter(int num) {
        return parameter[num];
    }

    public void setParameter(int num, String parameter) {
        this.parameter[num] = parameter;
    }
    
    public double getMin(final String nodeOrder) {
        
        this.graph = graphModel.getGraphVisible();
        for (Node n : graph.getNodes())
            n.getNodeData().setLayoutData(new ForceVectorNodeLayoutData());
        
        Node[] n = this.graph.getNodes().toArray();
        
        Node node = n[0];
        isString = node.getAttributes().getValue(this.nodeOrder).getClass().getName().contentEquals("java.lang.String");
        
        if (nodeOrder != null && !isString) {
            Arrays.sort(n, new Comparator<Node>() {

                @Override
                public int compare(Node o1, Node o2) {
                    Number n1 = (Number) o1.getAttributes().getValue(nodeOrder);
                    Number n2 = (Number) o2.getAttributes().getValue(nodeOrder);
                    
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
        
        Double min;
        
        if(!isString){
                min = Double.valueOf(n[0].getAttributes().getValue(nodeOrder).toString());
                absmin = (double) min.floatValue();
        }
        else
                absmin = 0;

        return absmin;
    }
    
    public double getMax(final String nodeOrder) {
        this.graph.readLock();
        
        Node[] n = this.graph.getNodes().toArray();
        Node node = n[0];
        isString = node.getAttributes().getValue(this.nodeOrder).getClass().getName().contentEquals("java.lang.String");
        
        if (nodeOrder != null && !isString) {
            Arrays.sort(n, new Comparator<Node>() {

                @Override
                public int compare(Node o1, Node o2) {
                    Number n1 = (Number) o1.getAttributes().getValue(nodeOrder);
                    Number n2 = (Number) o2.getAttributes().getValue(nodeOrder);
                    
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
        
        Double max;
        
        if(!isString){
                max = Double.valueOf(n[n.length-1].getAttributes().getValue(nodeOrder).toString());
                absmax = (double) max.floatValue();
        }
        else
                absmax = 1;
        
        return absmax;
    }
   
}
