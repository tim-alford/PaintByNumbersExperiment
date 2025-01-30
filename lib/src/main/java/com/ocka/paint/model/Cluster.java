// vim: ts=2
package com.ocka.paint.model;
import java.util.*;
import java.awt.Color;
public class Cluster {
	private static final Color[] SEED_COLOURS = new Color[]{
		new Color(15732500),
		new Color(16035339),
		new Color(15987723),
		new Color(7665416),
		new Color(3590455),
		new Color(2546087),
		new Color(693749),
		new Color(741108),
		new Color(7146994),
		new Color(12589287),
		new Color(15797988),
		new Color(11358881),
		new Color(15797842),
		new Color(15899917)
	};
	private Integer id;
	private Integer previousCount;
	private Integer currentCount;
	private Double minimum;
	private Double maximum;
	private Double mean;
	private Color seed; // for tracking changes in cluster shape/size
	private List<Observation> obs;
	public Cluster(Integer id, Double mean){
		this.id = id;
		this.mean = mean;
		this.obs = new LinkedList<>();
		this.currentCount = null;
		this.previousCount = null;
		this.seed = SEED_COLOURS[id-1]; // id is one indexed
	}
	public Color getSeedColor(){
		return this.seed;
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
