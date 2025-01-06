// vim: ts=2
package com.ocka.paint.model;
import java.util.*;
public class Cluster {
	private Integer id;
	private Integer previousCount;
	private Integer currentCount;
	private Double minimum;
	private Double maximum;
	private Double mean;
	private List<Observation> obs;
	public Cluster(Integer id, Double mean){
		this.id = id;
		this.mean = mean;
		this.obs = new LinkedList<>();
		this.currentCount = null;
		this.previousCount = null;
	}
	@Override
	public String toString(){
		return String.format("ID %d\tMEAN %f\tOBS %d\tPREV %d", id, mean, obs.size(), previousCount);	
	}
	public Double getMean(){
		return this.mean;
	}
	public Boolean isConverged(){
		return this.currentCount.equals(this.previousCount);
	}
	public List<Observation> getObservations(){
		return this.obs;
	}
	public void calculateMean(){
		if(this.obs.isEmpty()){
			this.mean = 0.0;
			this.minimum = 0.0;
			this.maximum = 0.0;
			return;
		}
		Double max = null, min = null, total = 0.0;
		for(Observation o: this.obs){
			Double v = (double) o.getValue();
			if(max == null || v > max)
				max = v;
			if(min == null || v < min)
				min = v;
			total += v;
		}
		this.mean = (1/(double)this.obs.size())*total;
		this.maximum = max;
		this.minimum = min;
	}
	public void clearObservations(){
		this.obs = new LinkedList<>();
	}
	public void updateCounts(){
		if(currentCount == null){
			currentCount = obs.size();
			return;
		}
		previousCount = currentCount;
		currentCount = obs.size();
	}
}
