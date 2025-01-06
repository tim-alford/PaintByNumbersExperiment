// vim: ts=2
package com.ocka.paint.image;
public class GreyScale {
	public int getGreyScaleIndex(int rgb){
		int r = (rgb & 0xFF0000) >> 16;
		int g = (rgb & 0x00FF00) >> 8;
		int b = rgb & 0x0000FF;
		double weight = (r+g+b)/3.0;
		return (int) weight;
	}
	public int[] getGreyTable(){
		int[] table = new int[256];
		for(int i = 0; i < 256; i++)
			table[i] = 0xFF000000 | i | (i << 8) | (i << 16);
		return table;
	}
}
