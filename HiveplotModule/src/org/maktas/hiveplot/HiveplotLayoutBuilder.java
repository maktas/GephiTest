
package org.maktas.hiveplot;

import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutProperty;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.GraphModel;
import org.gephi.ui.propertyeditor.NodeColumnNumbersEditor;
import org.openide.util.Lookup;


@ServiceProvider(service = LayoutBuilder.class)
public class HiveplotLayoutBuilder implements LayoutBuilder 
{
    int callCount = 0;
    GridLayout grid = new GridLayout(0,1);
    JPanel p = new JPanel(grid);
    private HiveplotLayoutUI ui = new HiveplotLayoutUI();
    HiveplotLayout hl = new HiveplotLayout(this);
    JComboBox<AttributeColumn> cb, cb2;
    AttributeColumn nodeOrder, axisOrder;
    int tickSpace=1;
    int numAxes=3;
    int oldnumAxes=3;
    public int radioValue=0;
 
    @Override
    public String getName()
    {
        return NbBundle.getMessage(HiveplotLayout.class, "name");
    }
    
    public AttributeColumn getNodeOrder()
    {
        return nodeOrder;
    }
    
    public AttributeColumn getAxisOrder()
    {
        return axisOrder;
    }


    @Override
    public LayoutUI getUI() 
    {
        return ui;
    }


    @Override
    public Layout buildLayout() 
    {
        return hl;
    }
    

    public class HivePanel extends JPanel implements ActionListener{
        JPanel hivePanel;
        JSpinner spinner[] = new JSpinner[10];
        JSlider slider[] = new JSlider[10];
        JTextField jText[] = new JTextField[10];
        JLabel[] emptyLabel = new JLabel[10];
        int jsValue,jspos=0;
        int min=0, max=10;
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable attributeTable = attributeModel.getNodeTable();
        AttributeColumn[] n = new AttributeColumn[attributeTable.countColumns()];
        
        public HivePanel(){
          
          GridLayout grid = new GridLayout(0,2);
                    
          hivePanel = new JPanel();
          hivePanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
          hivePanel.setLayout(grid);
          
          //Calculate the columns for axis assignment property
          for(int i=0;i<n.length;i++){
                n[i] = attributeTable.getColumn(i);
            }
            
            
            JLabel label = new JLabel("Press refresh to get new columns");
            label.setPreferredSize(new Dimension(150,20));
            hivePanel.add(label, grid);
            
            JButton button = new JButton("Refresh");
            button.setPreferredSize(new Dimension(100,20));
            button.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent actionEvent){
                    AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
                    attributeTable = attributeModel.getNodeTable();
                    n = new AttributeColumn[attributeTable.countColumns()];
                    refresh(0);
                }
            });
            
            hivePanel.add(button, grid);
            
            label = new JLabel("Axis Assignment Property:");
            hivePanel.add(label, grid);
            
            label = new JLabel("(Select the radio button if nominal)");
            hivePanel.add(label, grid);
            
            JRadioButton radio = new JRadioButton("Nominal/Numerical");
            radio.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){
                JRadioButton rb = (JRadioButton)e.getSource();
                int newSelection =  rb.getAccessibleContext().getAccessibleValue().getCurrentAccessibleValue().intValue();
                radioValue = newSelection;
                refresh(1);
                }
            });
            hivePanel.add(radio, grid);
            
            int count=0, pos=0;
            for(int i=0;i<n.length;i++){
                if(radioValue == 0){
                    if(!attributeTable.getColumn(i).getType().equals(AttributeType.STRING))
                    count++;
                }
                else{
                    if(attributeTable.getColumn(i).getType().equals(AttributeType.STRING))
                    count++;
                }
            }
            
            AttributeColumn[] m = new AttributeColumn[count];
            
            //Calculate the columns for axis assignment property
            for(int i=0;i<n.length;i++){
                if(radioValue == 0){
                    if(!n[i].getType().equals(AttributeType.STRING)){
                    m[pos] = n[i];
                    pos++;
                    }
                }
                else{
                    if(n[i].getType().equals(AttributeType.STRING)){
                    m[pos] = n[i];
                    pos++;
                    }
                }
            }
            nodeOrder = m[0];
            hl.setColumn(nodeOrder);
            
            cb = new JComboBox<AttributeColumn> (m);
            cb.addItemListener(new ItemListener() {

              @Override
              public void itemStateChanged(ItemEvent ie) {
                  if(ie.getStateChange() == 1){
                    nodeOrder = (AttributeColumn) cb.getSelectedItem();
                    hl.setColumn(nodeOrder);
                    min = (int) Math.floor(hl.getMin(nodeOrder));
                    max = (int) Math.floor(hl.getMax(nodeOrder));
                    oldnumAxes = numAxes;
                    refresh(2);
                  }
              }
          });

            hivePanel.add(cb, grid);
            
            label = new JLabel("On-Axis Ordering Property:");
            hivePanel.add(label, grid);
            
            count=0; pos=0;
            //Calculate the number of columns for on-axis ordering property
            for(int i=0;i<n.length;i++){
                    if(!n[i].getType().equals(AttributeType.STRING))
                    count++;
            }
            
            AttributeColumn[] o = new AttributeColumn[count];
            
            //Calculate the columns for on-axis ordering property
            for(int i=0;i<n.length;i++){
                    if(!n[i].getType().equals(AttributeType.STRING)){
                    o[pos] = n[i];
                    pos++;
                    }
            }
            
            //Set the first value in the combo box as on-axis ordering property
            axisOrder = o[0];
            hl.setAxisColumn(axisOrder);
            
            cb2 = new JComboBox<AttributeColumn> (o);
            cb2.addItemListener(new ItemListener() {

              @Override
              public void itemStateChanged(ItemEvent ie) {
                  if(ie.getStateChange() == 1){
                    axisOrder = (AttributeColumn) cb2.getSelectedItem();
                    hl.setAxisColumn(axisOrder);
                  }
              }
          });
            
          hivePanel.add(cb2, grid);
          
          label = new JLabel("Number of axes:");
          hivePanel.add(label, grid);
          
          JSlider axesNumber = new JSlider(2,10);
          axesNumber.setPreferredSize(new Dimension(150,30));
          axesNumber.setPaintLabels(true);
          axesNumber.setValue(3);
          hl.setNumAxes(numAxes);
          axesNumber.setMajorTickSpacing(1);
          
          axesNumber.addPropertyChangeListener(new PropertyChangeListener(){
              @Override
              public void propertyChange(PropertyChangeEvent evt) {
                  JSlider js = (JSlider) evt.getSource();
                  oldnumAxes = numAxes;
                  numAxes = js.getValue();
                  if(oldnumAxes != numAxes)
                  refresh(2);
              }
              
          });
          hivePanel.add(axesNumber, grid);
          
          min = (int) Math.floor(hl.getMin(nodeOrder));
          max = (int) Math.floor(hl.getMax(nodeOrder));
          
            if(max-min > 1000)
              tickSpace = 100;
            else if(max-min > 500)
              tickSpace = 50;
            else if(max-min > 100)
              tickSpace = 10;
            else if(max-min > 50)
              tickSpace = 5;
            else if(max-min > 30)
              tickSpace = 3;
            else if(max-min > 10)
              tickSpace = 2;
            else
              tickSpace = 1;
          
            for(int i=0;i<numAxes-1;i++){
                slider[i] = new JSlider(min, max);
                slider[i].setPreferredSize(new Dimension(150,30));
                slider[i].setPaintLabels(true);
                slider[i].setMajorTickSpacing(tickSpace);
                jsValue = slider[i].getValue();
                String stringValue = jsValue + "";
                hl.setParameter(i,stringValue);
                jsValue = slider[i].getValue();
                
                slider[i].addChangeListener(new ChangeListener(){
                     @Override
                     public void stateChanged(ChangeEvent ce) {
                          JSlider js = (JSlider) ce.getSource();
                          jsValue = js.getValue();
                          String stringValue = jsValue + "";
                          jspos = (js.getAccessibleContext().getAccessibleIndexInParent()-10)/2;
                          jText[jspos].setText(stringValue);
                          
                          hl.setParameter(jspos,stringValue);
                          for(int j=jspos+1;j<numAxes-1;j++){
                                slider[j].setMinimum(jsValue+1);
                                jsValue = slider[j].getValue();
                                stringValue = jsValue + "";
                                jText[j].setText(stringValue);
                          }
                          
                     }
                });
                        
                hivePanel.add(slider[i]);
              
                jText[i] = new JTextField();
                jText[i].setEditable(false);
                jText[i].setText(jsValue + "");
                jText[i].addActionListener(new ActionListener() {
                      @Override
                      public void actionPerformed(ActionEvent ae) {
                          JTextField jt = (JTextField) ae.getSource();
                          jspos = (jt.getAccessibleContext().getAccessibleIndexInParent()-10)/2;
                          //hl.setParameter(jspos,jt.getText());
                      }
                });
                
                hivePanel.add(jText[i]);
            }
          
          this.add(hivePanel);
          callCount++;
        }
        
       public void refresh(int value){

           //If refresh button is pressed repopulate both combo boxes
          if(value == 0){
          
            cb.removeAllItems();
            
            AttributeColumn[] n = new AttributeColumn[attributeTable.countColumns()];
            for(int i=0;i<n.length;i++){
                n[i] = attributeTable.getColumn(i);
            }
           
           //Calculate the columns for the axis assignment property
            int count=0, pos=0;
            for(int i=0;i<n.length;i++){
                if(radioValue == 1){
                    if(attributeTable.getColumn(i).getType().equals(AttributeType.STRING))
                    count++;
                }
                else{
                    if(!attributeTable.getColumn(i).getType().equals(AttributeType.STRING))
                    count++;
                }
            }
            
            AttributeColumn[] m = new AttributeColumn[count];
            
            for(int i=0;i<n.length;i++){
                if(radioValue == 1){
                    if(n[i].getType().equals(AttributeType.STRING)){
                    m[pos] = n[i];
                    cb.addItem(m[pos]);
                    pos++;
                    }
                }
                else{
                    if(!n[i].getType().equals(AttributeType.STRING)){
                    m[pos] = n[i];
                    cb.addItem(m[pos]);
                    pos++;
                    }
                }
            }
            
            cb2.removeAllItems();
                
            //Calculate the number of columns for on-axis ordering property
            count=0; pos=0;
            for(int i=0;i<n.length;i++){
                if(radioValue == 1){
                    if(n[i].getType().equals(AttributeType.STRING))
                    count++;
                }
                else{
                    if(!n[i].getType().equals(AttributeType.STRING))
                    count++;
                }
            }
            
            AttributeColumn[] o = new AttributeColumn[count];
            
            for(int i=0;i<n.length;i++){
                if(radioValue == 1){
                    if(n[i].getType().equals(AttributeType.STRING)){
                    o[pos] = n[i];
                    cb2.addItem(o[pos]);
                    pos++;
                    }
                }
                else{
                    if(!n[i].getType().equals(AttributeType.STRING)){
                    o[pos] = n[i];
                    cb2.addItem(o[pos]);
                    pos++;
                    }
                 }
             }
          }
            
          //Else if radio button is clicked repopulate only first combo box for axis assignment property and set sliders visible/invisible
          else if(value == 1){
            cb.removeAllItems();
            
            AttributeColumn[] n = new AttributeColumn[attributeTable.countColumns()];
            for(int i=0;i<n.length;i++){
                n[i] = attributeTable.getColumn(i);
            }
                
            //Calculate the number of columns for axis assignment property
            int count=0, pos=0;
            for(int i=0;i<n.length;i++){
                if(radioValue == 1){
                    if(attributeTable.getColumn(i).getType().equals(AttributeType.STRING))
                    count++;
                }
                else{
                    if(!attributeTable.getColumn(i).getType().equals(AttributeType.STRING))
                    count++;
                }
            }
            
            AttributeColumn[] m = new AttributeColumn[count];
            
            //If nominal is chosen in radio, bring nominal values to axis assignment combo box else bring numerical values
                if(radioValue == 1){
                  for(int i=0;i<n.length;i++){
                    if(n[i].getType().equals(AttributeType.STRING)){
                    m[pos] = attributeTable.getColumn(i);
                    cb.addItem(m[pos]);
                    pos++;
                    }
                  }
                  for(int i=0;i<numAxes-1;i++){
                      slider[i].setVisible(false);
                      jText[i].setText("");
                      jText[i].setEditable(true);
                  }
                  emptyLabel[numAxes-1] = new JLabel("");
                  hivePanel.add(emptyLabel[numAxes-1]);
                  
                  jText[numAxes-1] = new JTextField();
                  jText[numAxes-1].setEditable(true);
                  jText[numAxes-1].addActionListener(new ActionListener() {
                          @Override
                          public void actionPerformed(ActionEvent ae) {
                              JTextField jt = (JTextField) ae.getSource();
                              jspos = (jt.getAccessibleContext().getAccessibleIndexInParent()-10)/2;
                              //hl.setParameter(jspos,jt.getText());
                          }
                      });
                  hivePanel.add(jText[numAxes-1]);
                      
                }
                else{
                  hivePanel.remove(emptyLabel[numAxes-1]);
                  hivePanel.remove(jText[numAxes-1]);
                    
                  for(int i=0;i<n.length;i++){
                    if(!n[i].getType().equals(AttributeType.STRING)){
                    m[pos] = attributeTable.getColumn(i);
                    cb.addItem(m[pos]);
                    pos++;
                    }
                  }
                  for(int i=0;i<numAxes-1;i++){
                      slider[i].setVisible(true);
                      jText[i].setEditable(false);
                  }
                  
                }
            }
            
          //else if number of axes is changed or a value is selected in axis assignment combobox, recompute numerical sliders
          else if(value==2){
            if(max-min > 1000)
              tickSpace = 100;
            else if(max-min > 500)
              tickSpace = 50;
            else if(max-min > 100)
              tickSpace = 10;
            else if(max-min > 50)
              tickSpace = 5;
            else if(max-min > 30)
              tickSpace = 3;
            else if(max-min > 10)
              tickSpace = 2;
            else
              tickSpace = 1;
              
              if(oldnumAxes < numAxes){
                  if(radioValue == 1){
                    hivePanel.remove(emptyLabel[oldnumAxes-1]);
                    hivePanel.remove(jText[oldnumAxes-1]);
                  }
                  for(int i=oldnumAxes-1;i<numAxes-1;i++){
                        slider[i] = new JSlider(min, max);
                        slider[i].setPreferredSize(new Dimension(150,30));
                        slider[i].setPaintLabels(true);
                        slider[i].setMajorTickSpacing(tickSpace);
                        jsValue = slider[i].getValue();
                        String stringValue = jspos + "";
                        hl.setParameter(i,stringValue);
                        if(radioValue == 1){
                        jText[jspos].setText(stringValue);
                        }
                
                        slider[i].addChangeListener(new ChangeListener(){
                            @Override
                            public void stateChanged(ChangeEvent ce) {
                                JSlider js = (JSlider) ce.getSource();
                                jsValue = js.getValue();
                                String stringValue = jsValue + "";
                                jspos = (js.getAccessibleContext().getAccessibleIndexInParent()-10)/2;
                                jText[jspos].setText(stringValue);
                                
                                hl.setParameter(jspos,stringValue);
                                for(int j=jspos+1;j<numAxes-1;j++){
                                    slider[j].setMinimum(jsValue+1);
                                    jsValue = slider[j].getValue();
                                    stringValue = jsValue + "";
                                    jText[j].setText(stringValue);
                                }
                                
                            }
                        });
                
                    hivePanel.add(slider[i]);
                     
                    jText[i] = new JTextField();
                    if(radioValue == 1){
                        slider[i].setVisible(false);
                        jText[i].setText("");
                        jText[i].setEditable(true);
                    }
                    else{
                        slider[i].setVisible(true);
                        jText[i].setEditable(false);
                        jText[i].setText(jsValue + "");
                    }
                    
                    hivePanel.add(jText[i]);
                    
                    if(radioValue == 1){
                        emptyLabel[numAxes-1] = new JLabel("");
                        hivePanel.add(emptyLabel[numAxes-1]);
                        jText[numAxes-1] = new JTextField();
                        jText[numAxes-1].setEditable(true);
                        jText[numAxes-1].addActionListener(new ActionListener() {
                          @Override
                          public void actionPerformed(ActionEvent ae) {
                              JTextField jt = (JTextField) ae.getSource();
                              jspos = (jt.getAccessibleContext().getAccessibleIndexInParent()-10)/2;
                              //hl.setParameter(jspos,jt.getText());
                          }
                        });
                    
                        hivePanel.add(jText[numAxes-1]);
                    }
                        
                  }
              }
              
              else if(oldnumAxes > numAxes){
                  if(radioValue == 1){
                    hivePanel.remove(emptyLabel[oldnumAxes-1]);
                    hivePanel.remove(jText[oldnumAxes-1]);
                  }
                   for(int i=numAxes-1;i<oldnumAxes-1;i++){
                    if(radioValue == 1){
                        hivePanel.remove(slider[i]);
                        hivePanel.remove(jText[i]);
                    }
                    else{
                        hivePanel.remove(slider[i]);
                        hivePanel.remove(jText[i]);
                    }
                   }
                   if(radioValue == 1){
                        emptyLabel[numAxes-1] = new JLabel("");
                        hivePanel.add(emptyLabel[numAxes-1]);
                        jText[numAxes-1] = new JTextField();
                        jText[numAxes-1].setEditable(true);
                        jText[numAxes-1].addActionListener(new ActionListener() {
                          @Override
                          public void actionPerformed(ActionEvent ae) {
                              JTextField jt = (JTextField) ae.getSource();
                              jspos = (jt.getAccessibleContext().getAccessibleIndexInParent()-10)/2;
                              //hl.setParameter(jspos,jt.getText());
                          }
                        });
                    
                        hivePanel.add(jText[numAxes-1]);
                  }
                   
              }
              
              else{
                  for(int i=0;i<numAxes-1;i++){
                        hivePanel.remove(slider[i]);
                        hivePanel.remove(jText[i]);
                  }
                  
                  for(int i=0;i<numAxes-1;i++){
                        slider[i] = new JSlider(min, max);
                        slider[i].setPreferredSize(new Dimension(150,30));
                        slider[i].setPaintLabels(true);
                        slider[i].setMajorTickSpacing(tickSpace);
                        jsValue = slider[i].getValue();
                
                        slider[i].addChangeListener(new ChangeListener(){
                            @Override
                            public void stateChanged(ChangeEvent ce) {
                                JSlider js = (JSlider) ce.getSource();
                                jsValue = js.getValue();
                                String stringValue = jsValue + "";
                                jspos = (js.getAccessibleContext().getAccessibleIndexInParent()-10)/2;
                                jText[jspos].setText(stringValue);
                                
                                hl.setParameter(jspos,stringValue);
                                for(int j=jspos+1;j<numAxes-1;j++){
                                    slider[j].setMinimum(jsValue+1);
                                    jsValue = slider[j].getValue();
                                    stringValue = jsValue + "";
                                    jText[j].setText(stringValue);
                                }
                                
                            }
                        });
                        
                    hivePanel.add(slider[i]);

                    jText[i] = new JTextField();
                    jText[i].setEditable(false);
                    jText[i].setText(jsValue + "");
                
                    hivePanel.add(jText[i]);
                }
            }
              
          }

        }
        
        @Override
        public void actionPerformed(ActionEvent e){
            JComboBox jb = (JComboBox)e.getSource();
            String newSelection = jb.getSelectedItem().toString();
            }
        
        }

    
    private class HiveplotLayoutUI implements LayoutUI {


        @Override
        public String getDescription()
        {
            return NbBundle.getMessage(HiveplotLayout.class, "description");
        }


        @Override
        public Icon getIcon()
        {
            return null;
        }


        @Override
        public JPanel getSimplePanel(Layout layout) {
            
            HivePanel hp;
        
            p.setComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);
            
            if(callCount == 0){
            hp = new HivePanel();
            p.add(hp);
            }
            
            return p;
        }


        @Override
        public int getQualityRank() 
        {
            return 5;
        }


        @Override
        public int getSpeedRank() 
        {
            return 5;
        }
    }
}

