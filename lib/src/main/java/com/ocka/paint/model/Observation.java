// vim: ts=2
package com.ocka.paint.model;
public class Observation {
	private Integer pixel, x, y;
	public Observation(Integer pixel, Integer x, Integer y){
		this.pixel = pixel;
		this.x = x;
		this.y = y;
	}
	public void setPixel(Integer pixel){ this.pixel = pixel; }
	public Integer getPixel(){ return pixel; }
	public Integer getX(){ return x; }
	public Integer getY(){ return y; }
	public Integer getValue(){
		Integer v = 0;
		return pixel;
		/*
		v += (int) Math.pow(pixel, 2);
		v += (int) Math.pow(x, 2);
		v += (int) Math.pow(y, 2);
		return (int) Math.floor(Math.sqrt(v));
		*/
	}
	public boolean equals(Object other){
		if(other == null){
			return false;
		}
		if(this == other){
			return true;
		}
		if(other instanceof Observation){
			Observation obs = (Observation) other;
			return this.pixel.equals(obs.pixel);
		}
		return false;
	}
}
