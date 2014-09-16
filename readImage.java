/*
The readImage class provides the ability to read images from a folder. Each 
image is processed so that its features are extracted in terms of its
color code matrix and intensity matrix. Texture features are also extracted.
*/
package selamyihunassn5;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.Object.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;


public class readImage
{
  int imageCount = 1;
  double intensityBins [] = new double [25];
  double intensityMatrix [][] = new double[101][25];
  double colorCodeBins [] = new double [64];
  double colorCodeMatrix [][] = new double[101][64];
  double imageSizeList[] = new double[101];
  double coOccurenceMatrix [][] = new double[256][256];
  double textureMatrix [][] = new double[101][3];
  
  
  /*Each image is retrieved from the file.  The height and width are found
  for the image and the getIntensity and getColorCode methods are called
  and the image's size is recorded as width multiplied by height.
  */
  public readImage()
  {
      BufferedImage image = null; 
    while(imageCount < 101){
      try
      {
          String filename = "images/images/" + imageCount + ".jpg";
          image = ImageIO.read(new File(filename));
         
          initializeIntensityBins();
          initializeColorCodeBins();
          initializeCoOccurenceMatrix();
          int width = image.getWidth();
          int height = image.getHeight();
          imageSizeList[imageCount] = width * height;
          getIntensity(image, height, width);
          getColorCode(image, height, width);
          getTexture(image,height,width);
          imageCount++;
      } 
      catch (IOException e)
      {
        System.out.println("Error occurred when reading the file.");
      }
        
    }
    
    normalizeTexture();
    writeIntensity();       // writes the intensity file    
    writeColorCode();       //  writes the color code file
    writeTexture();         // writes the texture file
    writeSize();            // writes the file size file
  }
  
  /* This method computes the intensity of each image by traversing
  through each pixel to take the red green and blue values. These values
  are put through the intensity formula to increment the intensity bins 
  */
  public void getIntensity(BufferedImage image, int height, int width){
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            Color c = new Color(image.getRGB(col, row));
            int red = c.getRed();
            int green = c.getGreen();
            int blue = c.getBlue();
            double intensity  = (0.299* (double)red + 0.587* (double)green
                    + 0.114* (double)blue);
            incrementIntensity(intensity);
        }
    }
    for (int i = 0; i < 25; i++) {
        intensityMatrix[imageCount][i] = intensityBins[i];
    }  

  }
   
  /* This method computes the texture of each image by traversing
  through each pixel to take the red green and blue values. These values
  are put through the intensity formula to evaluate their gray tone
  so that a gray tone normalized cooccurence matrix is built.
  We then extract the energy, entropy, and contrast from the GLCM.
  */
  public void getTexture(BufferedImage image, int height, int width){
    for (int row = 0; row < height - 1; row++) {
        for (int col = 0; col < width - 1; col++) {
            Color c = new Color(image.getRGB(col, row));
            Color c2 = new Color(image.getRGB(col + 1, row + 1));
            int intensity1  = (int)(0.299* (double)c.getRed() + 0.587* (double)c.getGreen() + 0.114* 
                    (double)c.getBlue());
            int intensity2  = (int)(0.299* (double)c2.getRed() + 0.587* (double)c2.getGreen() + 0.114* 
                    (double)c2.getBlue());
            coOccurenceMatrix[intensity1][intensity2] += 1/(double)((width - 1)*(height - 1)*2);
            coOccurenceMatrix[intensity2][intensity1] += 1/(double)((width - 1)*(height - 1)*2);
        }
    }
    for (int i = 0; i < 256; i++) {
        for (int j = 0; j < 256; j++) {
            double coOccurenceValue = (coOccurenceMatrix[i][j]);
            textureMatrix[imageCount][0] += coOccurenceValue * coOccurenceValue;
            if (coOccurenceValue != 0.0){
            textureMatrix[imageCount][1] += Math.log(coOccurenceValue) / Math.log(2) * coOccurenceValue;
            }
            textureMatrix[imageCount][2] += (i-j)*(i-j) * coOccurenceValue;
        }
    }  

  }
   
  /* This method gets the intensity of a pixel to increment the the intensity
  bin value that correspondes to the given intensity  
  */
  public void incrementIntensity(double intensity){
    if (intensity >= 0 && intensity < 240){
        intensityBins[(int)(intensity/10)]++;
    }else if (intensity >= 240 && intensity < 255){
        intensityBins[24]++;
    }
  }
  
  /* This method initializes the intensity bins by setting them to zero
  */
    public void initializeIntensityBins() {
        for(int i = 0; i < 25; i++){
            intensityBins[i] = 0;
        }
    }
    
    /* This method initializes the color code bins by setting them to zero
  */
    public void initializeColorCodeBins() {
        for(int i = 0; i < 64; i++){
            colorCodeBins[i] = 0;
        }
    }
    
    /* This method initializes the coOccurence GrayLevel Matrix by setting values to zero
  */
    public void initializeCoOccurenceMatrix() {
        for(int i = 0; i < 256; i++){
            for(int j = 0; j < 256; j++){
                coOccurenceMatrix[i][j] = 0;
            }
        }
    }
    
    
    /* This method normalizes the texture feature matrix
  */
    private void normalizeTexture() {
        double totalEnergy = 0.0;
        double totalEntropy = 0.0;
        double totalContrast = 0.0;
        for(int i = 1; i < 101; i++){
            totalEnergy +=  textureMatrix[i][0];
            totalEntropy +=  textureMatrix[i][1];
            totalContrast +=  textureMatrix[i][2];
        }
        for(int i = 1; i < 101; i++){
            textureMatrix[i][0] =  textureMatrix[i][0] / totalEnergy;
            textureMatrix[i][1] =  textureMatrix[i][1] / totalEntropy;
            textureMatrix[i][2] =  textureMatrix[i][2] / totalContrast;
        }
    }
  
  /* This method computes the intensity of each image by traversing
  through each pixel to take the red green and blue values. These values
  are then compartmentalized into different color code bins 
  */
  public void getColorCode(BufferedImage image, int height, int width){
    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            Color c = new Color(image.getRGB(col, row));
            
            int code = computeCodeBit(c); // handles the bit manipulation to
                                            // get color code
            
            if(code >= 0 && code < 64){
                colorCodeBins[code]++;
            }
        }
    }
    for (int i = 0; i < 64; i++) {
        colorCodeMatrix[imageCount][i] = colorCodeBins[i];
    }  
  }
  
  // This method computes the the 6 bit color code of a given pixel using its
  // red, green, and blue values.
  private int computeCodeBit(Color c){
      int red = c.getRed();
            int green = c.getGreen();
            int blue = c.getBlue();
            
            String colorCode = "";
            int bit = getBit(red, 7);
            colorCode += bit;
            bit = getBit(red, 6);
            colorCode += bit;
            bit = getBit(green, 7);
            colorCode += bit;
            bit = getBit(green, 6);
            colorCode += bit;
            bit = getBit(blue, 7);
            colorCode += bit;
            bit = getBit(blue, 6);
            colorCode += bit;
            
            int base = 2;
            int code = Integer.parseInt(colorCode, base);
      return code;
  }
  
  // This method gets the ith bit in number
  private int getBit(int number, int i) {
         return (number >> i) & 1;
    }
  
  
  //This method writes the contents of the colorCode matrix to a 
  //file named colorCodes.txt.
  public void writeColorCode(){
        try{
             // Create file
            FileWriter fstream = new FileWriter("colorCodes.txt",true);
            //create BufferedWriter to write data to the file
            BufferedWriter out = new BufferedWriter(fstream);
            //writes data and blank line to file
            
            for(int i = 1; i < 101; i++){
                for(int j = 0; j < 64; j++){
                    out.write(colorCodeMatrix[i][j] + " ");
                }
                out.newLine();
            }
                //Close the output stream
            out.close();
        }catch(IOException e){
           System.out.print("File couldn't be created exception!");
        }
  }
  
  
   //This method writes the contents of the intensity matrix 
  //to a file called intensity.txt
  public void writeIntensity(){
        try{
             // Create file
            FileWriter fstream = new FileWriter("intensity.txt",true);
            //create BufferedWriter to write data to the file
            BufferedWriter out = new BufferedWriter(fstream);
            //writes data and blank line to file
            
            for(int i = 1; i < 101; i++){
                for(int j = 0; j < 25; j++){
                    out.write(intensityMatrix[i][j] + " ");
                }
                out.newLine();
            }
                //Close the output stream
            out.close();
        }catch(IOException e){
           System.out.print("File couldn't be created exception!");
        }	
  }
  
    
  //This method writes the texture feature of each image 
  //to a file called texture.txt
   public void writeTexture(){
        try{
             // Create file
            FileWriter fstream = new FileWriter("texture.txt",true);
            //create BufferedWriter to write data to the file
            BufferedWriter out = new BufferedWriter(fstream);
            //writes data and blank line to file
                        
            for(int i = 1; i < 101; i++){
                for(int j = 0; j < 3; j++){
                    out.write(textureMatrix[i][j] + " ");
                }
                out.newLine();
            }
                //Close the output stream
            out.close();
        }catch(IOException e){
           System.out.print("File couldn't be created exception!");
        }	
  }
   

    
  //This method writes the size of each image 
  //to a file called fileSize.txt
   public void writeSize(){
        try{
             // Create file
            FileWriter fstream = new FileWriter("fileSize.txt",true);
            //create BufferedWriter to write data to the file
            BufferedWriter out = new BufferedWriter(fstream);
            //writes data and blank line to file
            
            for(int i = 1; i < 101; i++){
                out.write("" + imageSizeList[i]);
                out.newLine();
            }
                //Close the output stream
            out.close();
        }catch(IOException e){
           System.out.print("File couldn't be created exception!");
        }	
  }
}
