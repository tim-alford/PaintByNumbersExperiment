// vim: ts=2
package com.ocka.paint;
import com.ocka.paint.model.*;
import com.ocka.paint.image.*;
import java.awt.image.*;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
public class Grey {
	private BufferedImage image;	
	private Grey(){
		this.image = null;
	}
	public void setup(String[] args) throws Exception {
		String path = args[0];
		File f = new File(path);
		if(!f.exists()){
			throw new Exception(String.format("The image file %s doesn't exist", path));
		}
		this.image = ImageIO.read(f);	
	}
	public void run() throws Exception {
		int width = this.image.getWidth(null);
		int height = this.image.getHeight(null);
		int pixels[] = this.image.getRGB(0, 0, width, height, null, 0, width);
		BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		GreyScale g = new GreyScale();
		int[] table = g.getGreyTable();
		for(int i = 0; i < height; i++){
			int offset = i * width;
			for(int j = 0; j < width; j++){
				int p = pixels[offset+j];
				int index = g.getGreyScaleIndex(p);
				int grey = table[index];
				copy.setRGB(j, i, grey);
			}
		}
		ImageIO.write(copy, "png", new File("grey.png"));
	}
	public static void main(String[] args){
		Grey m = new Grey();
		try{
			m.setup(args);
		}catch(Exception x){
			throw new RuntimeException("Failed to setup application", x);
		}
		try{
			m.run();
		}catch(Exception x){
			throw new RuntimeException("Caught exception while running application", x);
		}
	}
}
