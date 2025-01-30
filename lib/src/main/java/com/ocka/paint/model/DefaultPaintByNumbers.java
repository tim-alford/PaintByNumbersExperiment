// vim: ts=2
package com.ocka.paint.model;
import com.ocka.paint.controller.*;
import com.ocka.paint.image.*;
import java.awt.image.*;
import java.io.File;
import javax.imageio.*;
import java.awt.Point;
import java.util.*;
public class DefaultPaintByNumbers implements IPaintByNumbers {
	private List<ImageStateObserver> observers;
	public DefaultPaintByNumbers(List<ImageStateObserver> observers){
		this.observers = observers;
	}
	private List<Observation> getObservations(int[] pixels, int width, int height, boolean greyScale, GreyScale g) throws Exception {
		List<Observation> obs = new ArrayList<>(pixels.length);
		BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] greyTable = g.getGreyTable();
		for(int i = 0; i < height; i++){
			int offset = i * width;
			for(int j = 0; j < width; j++){
				int p = pixels[offset+j];
				if(!greyScale){
					p = p & 0x00FFFFFF;
					obs.add(new Observation(p, j, i));
				}else{
					int index = g.getGreyScaleIndex(p);
					int greyPixel = greyTable[index];
					obs.add(new Observation(greyPixel, j, i));
					frame.setRGB(j, i, greyPixel);
				}
			}
		}
		onImageChanged(frame);
		return obs;
	}
	private KMeans doClustering(int colours, int width, int height, List<Observation> obs) throws Exception {
		long seed = new Random().nextLong();
		KMeans k = new KMeans(colours, width, height, this.observers);
		List<Observation> all = new LinkedList<>();
		k.init(obs, seed);
		k.run(obs, 10000);
		if(!k.isConverged()){
			throw new Exception("KMeans failed to converge, please change input parameters.");
		}
		return k;
	}
	private Set<String> findAdjacentPixels(Set<String> seen, Point origin, Integer pixel, int[][] matrix){
		Set<String> group = new TreeSet<>();
		List<int[]> adjacent = new LinkedList<>();
		adjacent.add(new int[]{-1,-1});
		adjacent.add(new int[]{0,-1});
		adjacent.add(new int[]{1,-1});
		adjacent.add(new int[]{1,0});
		adjacent.add(new int[]{1,1});
		adjacent.add(new int[]{0,1});
		adjacent.add(new int[]{-1,1});
		adjacent.add(new int[]{-1,0});
		String sOrigin = String.format("%d,%d", origin.x, origin.y);
		group.add(sOrigin);
		doAdjacentPixels(group, seen, adjacent, origin, pixel, matrix);
		return group;
	}
	private void doAdjacentPixels(Set<String> group, Set<String> seen, List<int[]> adjacent, Point origin, Integer pixel, int[][] matrix){
		// look at surrounding pixels
		for(int[] a: adjacent){
			Point coord = new Point(a[0]+origin.x, a[1]+origin.y);
			String sCoord = String.format("%d,%d", coord.x, coord.y);
			// have we seen this coordinate yet?
			if(seen.contains(sCoord)){
				continue;
			}
			// make sure that coordinate is valid
			if(coord.x < 0 || coord.x >= matrix.length || coord.y >= matrix[coord.x].length || coord.y < 0){
				continue;
			}
			// record that we've seen this coord
			seen.add(sCoord);
			// grab pixel
			Integer pixel_ = matrix[coord.x][coord.y];
			// same colour?
			if(pixel.equals(pixel_)){
				// add to group
				group.add(sCoord);
				// recurse, make current coord new origin
				doAdjacentPixels(group, seen, adjacent, coord, pixel, matrix);
			}
		}
	}
	private List<Set<String>> groupPixels(Set<String> seen, List<Observation> all, int[][] matrix) throws Exception {
		List<Set<String>> groups = new LinkedList<>();
		for(Observation o: all){
			Point origin = new Point(o.getX(), o.getY());
			String sOrigin = String.format("%d,%d", origin.x, origin.y);
			if(seen.contains(sOrigin)){
				continue;
			}
			Integer pixel = o.getPixel();
			Set<String> group = findAdjacentPixels(seen, origin, pixel, matrix);
			groups.add(group);
		}
		return groups;
	}
	public void assignColours(KMeans clustering, int[][] matrix, List<Observation> all, GreyScale g) throws Exception {
		int[] greyTable = g.getGreyTable();
		final int OPACITY_MASK = 0x33000000;
		for(Cluster cl: clustering.getClusters()){
			int rgb =	(int) Math.floor(cl.getMean());
			int index = g.getGreyScaleIndex(rgb);
			rgb = greyTable[index];
			int argb = OPACITY_MASK | (rgb & 0x00FFFFFF);
			for(Observation o: cl.getObservations()){
				int x = o.getX();
				int y = o.getY();
				matrix[x][y] = argb;
				o.setPixel(argb);
				all.add(o);
			}
		}
	}
	protected List<Set<String>> filterGroups(List<Set<String>> groups, int minPixels){
		List<Set<String>> keep = new LinkedList<>();
		for(Set<String> group : groups){
			if(group.size() < minPixels)
				continue;
			keep.add(group);
		}
		return keep;
	}
	private Point getPointFromString(String s){
		String[] tokens = s.split(",");
		String x = tokens[0];
		String y = tokens[1];
		int nX = Integer.parseInt(x);
		int nY = Integer.parseInt(y);
		return new Point(nX, nY);
	}
	protected void outlineGroups(List<Set<String>> groups, int[][] matrix){
		Map<Integer, List<Integer>> mapX = new TreeMap<>(); // unique x to y values
		Map<Integer, List<Integer>> mapY = new TreeMap<>(); // unique y to x values
		for(Set<String> g_: groups){
			for(String s: g_){
				Point p = getPointFromString(s);
				Integer nX = p.x;
				Integer nY = p.y;
				List<Integer> yValues = mapX.get(nX);
				if(yValues == null){
					yValues = new LinkedList<>();
					mapX.put(nX, yValues);
				}
				yValues.add(nY);
				List<Integer> xValues = mapY.get(nY);
				if(xValues == null){
					xValues = new LinkedList<>();
					mapY.put(nY, xValues);
				}
				xValues.add(nX);
			}
			for(Integer x : mapX.keySet()){
				List<Integer> yValues = mapX.get(x);
				int[] nValues = new int[yValues.size()];
				int i = 0;
				for(Integer n: yValues){
					nValues[i++] = n;
				}
				Arrays.sort(nValues);
				int min = nValues[0];
				int max = nValues[nValues.length-1];
				matrix[x][min] = 0x55000000;
				matrix[x][max] = 0x55000000;
			}	
			for(Integer y : mapY.keySet()){
				List<Integer> xValues = mapY.get(y);
				int[] nValues = new int[xValues.size()];
				int i = 0;
				for(Integer n: xValues){
					nValues[i++] = n;
				}
				Arrays.sort(nValues);
				int min = nValues[0];
				int max = nValues[nValues.length-1];
				matrix[min][y] = 0x55000000;
				matrix[max][y] = 0x55000000;
			}
		}
	}
	private BufferedImage writeImage(int[][] matrix, String fileName) throws Exception {
		BufferedImage image = new BufferedImage(matrix.length, matrix[0].length, BufferedImage.TYPE_INT_ARGB);
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[i].length; j++){
				int pixel = matrix[i][j];
				pixel = pixel == 0x55000000 ? pixel : 0xFFFFFFFF;
				image.setRGB(i, j, pixel);	
			}
		}
		ImageIO.write(image, "png", new File(fileName));
		return image;
	}
	private void onImageChanged(BufferedImage image) throws Exception {
		for(ImageStateObserver o: this.observers){
			o.onImageChanged(image);
		}
	}
	@Override
	public BufferedImage paint(BufferedImage image, Boolean greyScale, Integer colours) throws Exception {
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		final int MIN_PIXELS_PER_GROUP = 20;
		int pixels[] = image.getRGB(0, 0, width, height, null, 0, width);
		// write original image
		onImageChanged(image);
		GreyScale g = new GreyScale();
		// write grey scale image
		List<Observation> observations = getObservations(pixels, width, height, greyScale, g);
		if(greyScale){
			KMeans clustering = doClustering(colours, width, height, observations);
			List<Observation> all = new LinkedList<>();
			int[][] matrix = new int[width][height];
			assignColours(clustering, matrix, all, g);
			Set<String> seen = new TreeSet<>();	
			List<Set<String>> groups = groupPixels(seen, all, matrix);
			groups = filterGroups(groups, MIN_PIXELS_PER_GROUP);
			outlineGroups(groups, matrix);
			String fileName = String.format("%d.png", System.currentTimeMillis());
			return writeImage(matrix, fileName);
		}else{
			throw new Exception("Only grey scale is supported now");
		}
	}
}
