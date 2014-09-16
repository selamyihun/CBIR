package selamyihunassn5;

/* Project 2
This program uses the color code and intensity methods to allow a user
image retrieval from an image database after the user is given an 
image query from the database. Addidtionally it supports  a combination
of color code and intensity methods with relative feedback. It also
can use texture to help the intensity plus color code method.

Author: Selam Yihun 

*/

import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints; 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.Math;
import java.math.BigDecimal;
import java.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.AbstractAction;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import static javax.swing.UIManager.getColor;
import selamyihunassn5.readImage;

public class CBIR extends JFrame{
    
    private JLabel photographLabel = new JLabel();  //container to hold a large 
    private JButton [] button; //creates an array of JButtons
    private JCheckBox [] checkbox; //creates an array of checkBoxes
    private int [] buttonOrder = new int [101]; //creates an array to keep up with the image order
    private double [] imageSize = new double[101]; //keeps up with the image sizes
    private GridLayout gridLayout1;
    private GridLayout gridLayout2;
    private GridLayout gridLayout3;
    private GridLayout gridLayout4;
    private JPanel panelBottom1;
    private JPanel panelBottom2;
    private JPanel panelTop;
    private JPanel buttonPanel;
    private JCheckBox relevance;
    private JCheckBox texture;
    private boolean isRelevanceOn = false;
    private boolean isTextureOn = false;
    private Double [][] intensityMatrix = new Double [101][25];
    private Double [][] colorCodeMatrix = new Double [101][64]; 
    private Double [][] textureMatrix = new Double [101][3];
    private Double [][] intensityColorCodeMatrix = new Double [101][89];
    private Double [] weightVector = new Double [89];
    private TreeMap <Double, LinkedList<Integer>> map;
    private Vector <Integer> relevantImages = new Vector <Integer>();
    int picNo = 0;
    int imageCount = 1; //keeps up with the number of images displayed since the first page.
    int pageNo = 1;
    
    
    public static void main(String args[]) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new readImage();
                CBIR app = new CBIR();                
                app.setVisible(true);
            }
        });
    }
    
    
    
    public CBIR() {
      //The following lines set up the interface including the layout of the buttons and JPanels.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Content Based Image Retrieval");        
        panelBottom1 = new JPanel();
        panelBottom2 = new JPanel();
        panelTop = new JPanel();
        buttonPanel = new JPanel();
        gridLayout1 = new GridLayout(4, 5, 5, 5);
        gridLayout2 = new GridLayout(2, 1, 5, 5);
        gridLayout3 = new GridLayout(1, 2, 5, 5);
        gridLayout4 = new GridLayout(2, 3, 5, 5);
        setLayout(gridLayout2);
        panelBottom1.setLayout(gridLayout1);
        panelBottom2.setLayout(gridLayout1);
        panelTop.setLayout(gridLayout3);
        add(panelTop);
        add(panelBottom1);
        photographLabel.setVerticalTextPosition(JLabel.BOTTOM);
        photographLabel.setHorizontalTextPosition(JLabel.CENTER);
        photographLabel.setHorizontalAlignment(JLabel.CENTER);
        photographLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttonPanel.setLayout(gridLayout4);
        panelTop.add(photographLabel);

        panelTop.add(buttonPanel);
        JButton previousPage = new JButton("Previous Page");
        JButton nextPage = new JButton("Next Page");
        JButton intensity = new JButton("Intensity");
        JButton colorCode = new JButton("Color Code");
        JButton intensityColorCode = new JButton("Intensity + Color Code");
        relevance = new JCheckBox("Relevance");
        texture = new JCheckBox("Texture");
        relevance.setVisible(false);
        buttonPanel.add(intensity);
        buttonPanel.add(colorCode);
        buttonPanel.add(previousPage);
        buttonPanel.add(nextPage);
        buttonPanel.add(intensityColorCode);
        buttonPanel.add(relevance);
        buttonPanel.add(texture);
        
        nextPage.addActionListener(new nextPageHandler());
        previousPage.addActionListener(new previousPageHandler());
        intensity.addActionListener(new intensityHandler());
        colorCode.addActionListener(new colorCodeHandler());
        intensityColorCode.addActionListener(new intensityColorCodeHandler());
        relevance.addItemListener(new itemChangeHandler());
        texture.addItemListener(new itemChangeHandler());
        setSize(1100, 750);
        // this centers the frame on the screen
        setLocationRelativeTo(null);
        
        
        button = new JButton[101];
        checkbox = new JCheckBox[101];
        /*This for loop goes through the images in the database and stores them as icons and adds
         * the images to JButtons and then to the JButton array
        */
        for (int i = 1; i < 101; i++) {
                ImageIcon icon;
                icon = new ImageIcon("images/images/" + i + ".jpg");  
                
                 if(icon != null){
                     ImageIcon smallIcon = new ImageIcon(icon.getImage().getScaledInstance
                                    (200, 80, java.awt.Image.SCALE_SMOOTH));
                    // we scale the image before it is put on the button
                    button[i] = new JButton(smallIcon);
                    //panelBottom1.add(button[i]);
                    button[i].addActionListener(new IconButtonHandler(i, icon));
                    buttonOrder[i] = i;
                    checkbox[i] = new JCheckBox(smallIcon);
                    checkbox[i].addItemListener(new itemChangeHandler());
                }
                 
        }
        
        initializeWeights();
        readIntensityFile();        
        readColorCodeFile();
        readTextureFile();
        readFileSizeFile();
        normalizeFeatures();
        displayFirstPage();
    }
    
    /*This method opens the intensity text file containing the intensity matrix with the histogram bin values for each image.
     * The contents of the matrix are processed and stored in a two dimensional array called intensityMatrix.
    */
    public void readIntensityFile(){
        Scanner read;
        Double intensityBin;
      
        // iterates through each image in the and adds its intensity bins from
        // the given file
         try{
           read =new Scanner(new File ("intensity.txt"));
           for(int i = 1; i < 101; i++){
                for(int j = 0; j < 25; j++){
                    if(read.hasNext()){
                        intensityBin = read.nextDouble();
                        intensityMatrix[i][j] = intensityBin;
                    }
                }
            }
         }
         catch(FileNotFoundException EE){
           System.out.println("The file intensity.txt does not exist");
         }
      
    }
    
    /*This method opens the color code text file containing the color code matrix with the histogram bin values for each image.
     * The contents of the matrix are processed and stored in a two dimensional array called colorCodeMatrix.
    */
    private void readColorCodeFile(){
        Scanner read;
        Double colorCodeBin;
        
        // iterates through each image in the and adds its color code bins from
        // the given file
         try{
           read =new Scanner(new File ("colorCodes.txt"));
            for(int i = 1; i < 101; i++){
                for(int j = 0; j < 64; j++){
                    if(read.hasNext()){
                        colorCodeBin = read.nextDouble();
                        colorCodeMatrix[i][j] = colorCodeBin;
                    }
                }
            }
         }
         catch(FileNotFoundException EE){
           System.out.println("The file colorCodes.txt does not exist");
         }
      
      
    }
    
      /*This method opens the texture file to get the extracted feature values
     * for each image.
    */
    private void readTextureFile(){
        Scanner read;
        Double textureValue;
        
        // iterates through each image in the and adds its color code bins from
        // the given file
         try{
           read =new Scanner(new File ("texture.txt"));
            for(int i = 1; i < 101; i++){
                for(int j = 0; j < 3; j++){
                    if(read.hasNext()){
                        textureValue = read.nextDouble();
                        textureMatrix[i][j] = textureValue;
                    }
                }
            }
         }
         catch(FileNotFoundException EE){
           System.out.println("The file texture.txt does not exist");
         }
      
      
    }
    
    
    /*This method divides each image with its size.
    */ 
    private void normalizeFeatures(){
        for (int i = 1; i < 101; i++){
            for(int j = 0; j < 64; j++) {
                intensityColorCodeMatrix[i][j] = colorCodeMatrix[i][j] / (double)imageSize[i];
            }
            for(int j = 64; j < 89; j++) {
                intensityColorCodeMatrix[i][j] = intensityMatrix[i][j - 64] / (double)imageSize[i];
            }
        }
        normalize();
    }
    
    /*This method calculates the average and standard deviation of each feature of 
    * all the images then normalizes the feature matrix. 
    */
    private void normalize() {
        for (int i = 0; i < 89; i++){
            double total = 0.0;
            double average = 0.0;
            double standardDev = 0.0;
            double varianceTotal = 0.0;
            
            //calculates the average feature values
            for (int j = 1; j < 101; j++){
                total += intensityColorCodeMatrix[j][i];
            }
            average = total / 100.0;
            
            // calculates the variance and standard deviation
            for (int j = 1; j < 101; j++){
                varianceTotal += Math.pow(intensityColorCodeMatrix[j][i] - average, 2);
            }
            standardDev = Math.sqrt(varianceTotal / 99.0);
            
            // normalizes the feature matrix 
            for (int j = 1; j < 101; j++){
                if(standardDev != 0){
                    intensityColorCodeMatrix[j][i] = 
                    (intensityColorCodeMatrix[j][i] - average)/standardDev;
                }
            }
        }
    }
        
        
        
    /*This method opens the file size file to read the image 
    sizes for each file. The contents of the file are processed and 
    stored in an array called imageSize
    */
    private void readFileSizeFile(){
      Scanner read;
      Double currentImageSize;
         try{
           read =new Scanner(new File ("fileSize.txt"));
            for(int i = 1; i < 101; i++){
                if(read.hasNext()){
                   currentImageSize = read.nextDouble();
                   imageSize[i] = currentImageSize;
               }
                
            }
         }
         catch(FileNotFoundException EE){
           System.out.println("The file colorCodes.txt does not exist");
         }
      
      
    }
    
    /*This method initializes the weight array that holds the weight for each feature.
    */
    private void initializeWeights(){
        for (int i = 0; i < 89; i++){
            weightVector[i] = (double)1.0/89; // initial weight is set to 1/number of features
        }
    }
    
    /* This method updates the weight array by calculating the average and standard
    deviation of each feature of all the relevant images. The weight is 1 divided
    by the standard deviation.
    */    
    private void updateWeights() {
        double weightSum = 0.0;
        double standardDevArray[] = new double[89];
        double averageArray[] = new double[89];
        double minStandardDev = Double.MAX_VALUE;
        for (int i = 0; i < 89; i++){
            double total = 0.0;
            double average = 0.0;
            double standardDev = 0.0;
            double varianceTotal = 0.0;
            int size = relevantImages.size();
            
            boolean sameStdDev = true; //checks to see if all the relevant images have the same
                                        // std deviation, if so then average is the number
                                        // This is to handle numerical inaccuracy by double
            //calculates the average feature values
            double temp = 0.0;
            for (int j = 0; j < size - 1; j++){
                temp = intensityColorCodeMatrix[relevantImages.get(j)][i];
                if (temp != intensityColorCodeMatrix[relevantImages.get(j + 1)][i]){
                    sameStdDev = false;              
                }
            }
            
            if(sameStdDev) {
                average = temp;
            }
            else {
                //calculates the average feature values
                for (int j = 0; j < size; j++){
                    total += intensityColorCodeMatrix[relevantImages.get(j)][i];
                }
                if(size != 0) {average = total / (double)size;}
            }
            // calculates the variance and standard deviation
            for (int j = 0; j < size; j++){
                varianceTotal += Math.pow(intensityColorCodeMatrix[relevantImages.get(j)][i]
                        - average, 2);
            }
            if(size != 0) {standardDev = Math.sqrt(varianceTotal / (double)(size));}
            standardDevArray[i] = standardDev;
            averageArray[i] = average;
            if (standardDev != 0) {     // keeps track of the minimum non-zero standard deviation of all features
                minStandardDev = Math.min(standardDev, minStandardDev); 
            }
        }
        // update the weight
        for (int i = 0; i < 89; i++){
            if(standardDevArray[i] == 0){
                if(averageArray[i] == 0){
                    weightVector[i] = 0.0;
                } else {
                    weightVector[i] = 0.5 * minStandardDev;
                }                    
            }else{
                weightVector[i] = 1 / standardDevArray[i];
            }
            weightSum += weightVector[i];
        }
        for (int i = 0; i < 89; i++){       // normalize the weight array
            weightVector[i] = weightVector[i] / weightSum;
        }
        
    }
    
    /*
    */
    private void setAllChecked(boolean checked){
        for (int i = 1; i < 101; i++){
            checkbox[i].setSelected(checked);
        }
    }
    
    /*This method displays the first twenty images in the panelBottom.  The for loop starts at number one and gets the image
     * number stored in the buttonOrder array and assigns the value to imageButNo.  The button associated with the image is 
     * then added to panelBottom1.  The for loop continues this process until twenty images are displayed in the panelBottom1
    */
    private void displayFirstPage(){
      int imageButNo = 0;
      panelBottom1.removeAll(); 
      for(int i = 1; i < 21; i++){
        //System.out.println(button[i]);
        imageButNo = buttonOrder[i];
        // the following adds check boxes for each image if relevance is checked
        if(isRelevanceOn){
            panelBottom1.add(checkbox[imageButNo]);
        } else {
            panelBottom1.add(button[imageButNo]); 
        }
        imageCount ++;
      }
      panelBottom1.revalidate();  
      panelBottom1.repaint();

    }
    
    
    
    /*This class implements an ActionListener for each iconButton.  When an icon button is clicked, the image on the 
     * the button is added to the photographLabel and the picNo is set to the image number selected and being displayed.
    */ 
    private class IconButtonHandler implements ActionListener{
      int pNo = 0;
      ImageIcon iconUsed;
      
      IconButtonHandler(int i, ImageIcon j){
        pNo = i;
        iconUsed = j;  //sets the icon to the one used in the button
      }
      
      public void actionPerformed( ActionEvent e){
        photographLabel.setIcon(iconUsed);
        picNo = pNo;
      }
      
    }
    
    /*This class implements an ActionListener for the nextPageButton.  The last image number to be displayed is set to the 
     * current image count plus 20.  If the endImage number equals 101, then the next page button does not display any new 
     * images because there are only 100 images to be displayed.  The first picture on the next page is the image located in 
     * the buttonOrder array at the imageCount
    */
    private class nextPageHandler implements ActionListener{

      public void actionPerformed( ActionEvent e){
          int imageButNo = 0;
          int endImage = imageCount + 20;
          if(endImage <= 101){
            panelBottom1.removeAll(); 
            for (int i = imageCount; i < endImage; i++) {
                    imageButNo = buttonOrder[i];
                    if(isRelevanceOn){
                        panelBottom1.add(checkbox[imageButNo]);
                    }else {
                        panelBottom1.add(button[imageButNo]); 
                    }
                    imageCount++;
          
            }
  
            panelBottom1.revalidate();  
            panelBottom1.repaint();
          }
      }
      
    }
    
    /*This class implements an ActionListener for the previousPageButton.  The last image number to be displayed is set to the 
     * current image count minus 40.  If the endImage number is less than 1, then the previous page button does not display any new 
     * images because the starting image is 1.  The first picture on the next page is the image located in 
     * the buttonOrder array at the imageCount
    */
    private class previousPageHandler implements ActionListener{

      public void actionPerformed( ActionEvent e){
          int imageButNo = 0;
          int startImage = imageCount - 40;
          int endImage = imageCount - 20;
          if(startImage >= 1){
            panelBottom1.removeAll();
            /*The for loop goes through the buttonOrder array starting with the startImage value
             * and retrieves the image at that place and then adds the button to the panelBottom1.
            */
            for (int i = startImage; i < endImage; i++) {
                    imageButNo = buttonOrder[i];
                    if(isRelevanceOn){
                        panelBottom1.add(checkbox[imageButNo]);
                    }else {
                        panelBottom1.add(button[imageButNo]); 
                    }
                    imageCount--;
          
            }
  
            panelBottom1.revalidate();  
            panelBottom1.repaint();
          }
      }
      
    }
    
    
    /*This class implements an ActionListener when the user selects the intensityHandler button.  The image number that the
     * user would like to find similar images for is stored in the variable pic.  pic takes the image number associated with
     * the image selected and subtracts one to account for the fact that the intensityMatrix starts with zero and not one.
     * The size of the image is retrieved from the imageSize array.  The selected image's intensity bin values are 
     * compared to all the other image's intensity bin values and a score is determined for how well the images compare.
     * The images are then arranged from most similar to the least.
     */
    private class intensityHandler implements ActionListener{

        public void actionPerformed( ActionEvent e){
            if(picNo != 0){
            double [] distance = new double [101];
            map = new TreeMap<Double, LinkedList<Integer>>();
            double d = 0;
            int buttonNo = 1;
          
            for(int i = 1; i < 101; i++){
                d = 0;
                // compute the Manhattan Distance between images
                for(int j = 0; j < 25; j++) {
                      d += Math.abs((intensityMatrix[picNo][j] / imageSize[picNo] -
                       intensityMatrix[i][j] / imageSize[i]));
                }
                if (isTextureOn) {
                    for (int j = 0; j < 3; j++){
                        d += Math.abs(textureMatrix[picNo][j] - textureMatrix[i][j]);
                    }
                }
                distance[i] = d;
                if(map.get(d) == null){
                    // creates a new linked list for that key if the key
                    // doesn't exist
                   map.put(d, new LinkedList<Integer>());
                }
                map.get(d).add(i);  // add the compared image number keyed to the distance                          
            }
          
            // traverse through the map to sort the buttons so that closest
          // distance is displayed first
            for(Map.Entry<Double,LinkedList<Integer>> entry : map.entrySet()) {
                LinkedList<Integer> list = entry.getValue();
                while (list.size() > 0) {
                    buttonOrder[buttonNo] = list.pop();
                    buttonNo++;
                }
            }
          
          // the following displays the image retrieval results          
          imageCount = 1;
          relevance.setSelected(false);
          relevance.setVisible(false); // makes the relevance check box disappear
          displayFirstPage(); 
        }
      }
      
    }
    
    /*This class implements an ActionListener when the user selects the colorCode button.  The image number that the
     * user would like to find similar images for is stored in the variable pic.  pic takes the image number associated with
     * the image selected and subtracts one to account for the fact that the intensityMatrix starts with zero and not one. 
     * The size of the image is retrieved from the imageSize array.  The selected image's intensity bin values are 
     * compared to all the other image's intensity bin values and a score is determined for how well the images compare.
     * The images are then arranged from most similar to the least.
     */ 
    private class colorCodeHandler implements ActionListener{

      public void actionPerformed( ActionEvent e){
          if (picNo != 0){
          double [] distance = new double [101];
          map = new TreeMap<Double, LinkedList<Integer>>();
          double d = 0;
          int buttonNo = 1;
          
          for(int i = 1; i < 101; i++){
                d = 0;
                // compute the Manhattan Distance between images
                for(int j = 0; j < 64; j++) {
                      d += Math.abs((colorCodeMatrix[picNo][j] / imageSize[picNo] -
                       colorCodeMatrix[i][j] / imageSize[i]));
                }
                if (isTextureOn) {
                    for (int j = 0; j < 3; j++){
                        d += Math.abs(textureMatrix[picNo][j] - textureMatrix[i][j]);
                    }
                }
                distance[i] = d;        // set distance in distance array 
                
                if(map.get(d) == null){
                    // creates a new linked list for that key if the key
                    // doesn't exist
                   map.put(d, new LinkedList<Integer>());
                }

                map.get(d).add(i);  // add the compared image number keyed to the distance                
            }
            
          // traverse through the map to sort the buttons so that closest
          // distance is displayed first
            for(Map.Entry<Double,LinkedList<Integer>> entry : map.entrySet()) {
                LinkedList<Integer> list = entry.getValue();
                while (list.size() > 0) {
                    buttonOrder[buttonNo] = list.pop();
                    buttonNo++;
                }
            }         
          // the following displays the image retrieval results
          imageCount = 1;
          relevance.setSelected(false);
          relevance.setVisible(false); // makes the relevance check box disappear
          displayFirstPage();    
          }
      
      }
    }

      /*This class implements an ActionListener when the user selects the intensityColorCode button.  The image number that the
     * user would like to find similar images for is stored in the variable pic.  pic takes the image number associated with
     * the image selected. 
     * The size of the image is retrieved from the imageSize array.  The selected image's intensity and color code bin values are 
     * compared to all the other image's intensity and colod code bin values and a score is determined for how well the images compare.
     * The images are then arranged from most similar to the least.
     */ 
    private class intensityColorCodeHandler implements ActionListener{

      public void actionPerformed( ActionEvent e){
          if (picNo != 0){
          double [] distance = new double [101];
          map = new TreeMap<Double, LinkedList<Integer>>();
          double d = 0;
          int buttonNo = 1;
          if(isRelevanceOn) { updateWeights(); }
          for(int i = 1; i < 101; i++){
                d = 0;
                // compute the Manhattan Distance between images
                for(int j = 0; j < 89; j++) {
                      d += weightVector[j] * Math.abs(intensityColorCodeMatrix[picNo][j] - 
                              intensityColorCodeMatrix[i][j]);
                }
                if (isTextureOn) {
                    for (int j = 0; j < 3; j++){
                        d += Math.abs(textureMatrix[picNo][j] - textureMatrix[i][j]);
                    }
                }
                distance[i] = d;        // set distance in distance array 
                if(map.get(d) == null){
                    // creates a new linked list for that key if the key
                    // doesn't exist
                   map.put(d, new LinkedList<Integer>());
                }

                map.get(d).add(i);  // add the compared image number keyed to the distance                
            }
            
          // traverse through the map to sort the buttons so that closest
          // distance is displayed first
            for(Map.Entry<Double,LinkedList<Integer>> entry : map.entrySet()) {
                LinkedList<Integer> list = entry.getValue();
                while (list.size() > 0) {
                    buttonOrder[buttonNo] = list.pop();
                    buttonNo++;
                }
            }         
          // the following displays the image retrieval results
          imageCount = 1;
          relevance.setVisible(true); // makes the relevance check box appear
          displayFirstPage();    
          }
      }
    }
    
    
    /*This class is a listener class for the checkboxes. when the relevance toggle is
     *is checked we turn on all the other checkboxes for the image. You click
    * on the image to select. Weights are also initialized when the RF is turned
    * off and all the check boxes are removed. 
    */
    
    private class itemChangeHandler implements ItemListener{

        /** Listens to the check boxes. */
        public void itemStateChanged(ItemEvent e) {
            Object source = e.getItemSelectable();

            if (source == relevance) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    isRelevanceOn = false;
                    setAllChecked(false);
                    initializeWeights();
                    displayFirstPage();
                } else if (e.getStateChange() == ItemEvent.SELECTED) {
                    isRelevanceOn = true;
                    displayFirstPage();
                }
            } 
            
            if (source == texture) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    isTextureOn = false;
                    isRelevanceOn = false;
                    setAllChecked(false);
                    initializeWeights();
                    displayFirstPage();
                } else if (e.getStateChange() == ItemEvent.SELECTED) {
                    isTextureOn = true;
                    displayFirstPage();
                }

            } 
            
            // the following adds the relevant images to the 
            // relevantImages vector when their respective checkbox
            // is selected
            for(int i = 1; i < 101; i++){
                if (source == checkbox[i]){
                     if (e.getStateChange() == ItemEvent.DESELECTED) {
                        checkbox[i].setBackground(getColor(Color.TRANSLUCENT));
                        relevantImages.remove((Integer)i);
                    } else if (e.getStateChange() == ItemEvent.SELECTED) {
                        checkbox[i].setBackground(Color.red);
                        relevantImages.add(i);
                    }
                }
            }
                      
        }        
    }
}
