// vim: ts=2
package com.ocka.paint.model;
import com.ocka.paint.controller.*;
import java.awt.image.*;
import java.util.*;
public class KMeans {
	private Integer size; // number of clusters	
	private List<Cluster> clusters;
	private Boolean converged = false;
	private List<ImageStateObserver> observers;
	private int width, height;
	public KMeans(Integer size, int width, int height, List<ImageStateObserver> observers){
		this.size = size;
		this.clusters = new ArrayList<>(size);
		this.observers = observers;
		this.width = width;
		this.height = height;
	}
	public List<Cluster> getClusters(){ return clusters; }
	public Boolean isConverged(){
		return converged;
	}
	public void addCluster(Integer choice){
		Integer id = this.clusters.size()+1;
		Cluster c = new Cluster(id, (double)choice.intValue());
		this.clusters.add(c);
	}
	public void addObservation(Observation obs){
		Double diff = null;
		Cluster selected = null;
		Integer v = obs.getValue();
		for(Cluster c: this.clusters){
			Double m = c.getMean();
			Double current = Math.sqrt(Math.pow(m-v,2));
			if(diff == null || current < diff){
				selected = c;
				diff = current;
			}
		}
		selected.getObservations().add(obs);
	}
	// initialise with specific values
	public void init(List<Cluster> clusters){
		this.clusters = clusters;
	}
	// initialise randomly
	public void init(List<Observation> obs, long seed){
		Integer i = 0;
		Random rand = new Random(seed);
		while(i < this.size){
			Integer index = rand.nextInt(obs.size());
			Observation o = obs.get(index);
			Integer choice = o.getValue();
			addCluster(choice);
			i++;
		}
	}
	private BufferedImage getImageState(){
		BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
		for(Cluster c: this.clusters){
			int pixel = c.getSeedColor().getRGB();
			for(Observation o: c.getObservations()){
				int x = o.getX(), y = o.getY();
				image.setRGB(x, y, pixel);
			}
		}
		return image;
	}
	private void onIterationComplete(){
		BufferedImage i = getImageState(); // get copy of image after this current iteration
		for(ImageStateObserver o: this.observers){
			try{
				o.onImageChanged(i);
			}catch(Exception x){
				throw new RuntimeException("Failed to write image", x);
			}
		}
	}
	public void run(List<Observation> obs, Integer limit){
		Integer iterations = 0;
		Boolean trace = false;
		this.converged = false;
		while(iterations < limit){
			for(Cluster c: this.clusters)
				c.clearObservations();
			for(Observation o: obs)	
				this.addObservation(o);
			if(trace){
				for(Cluster c: this.clusters){
					System.out.printf("Add obs %d :: %s\n", iterations, c);
				}
			}
			for(Cluster c: this.clusters){
				c.calculateMean();
				if(trace){
					System.out.printf("Calculate mean %d :: %s\n", iterations, c);
				}
			}
			for(Cluster c: this.clusters)
				c.updateCounts();
			onIterationComplete();
			Boolean converged = true;
			for(Cluster c: this.clusters)
				converged = converged && c.isConverged();
			if(converged){
				this.converged = true;
				break;
			}
			iterations++;
		}
	}
}
