// vim: ts=2
package com.ocka.paint;
import com.ocka.paint.model.*;
import com.ocka.paint.image.*;
import java.awt.image.*;
import java.awt.Color;
import java.awt.Point;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
public class Main {
	private BufferedImage image;	
	private int nClusters;
	private Main(){
		this.image = null;
	}
	public void setup(String[] args) throws Exception {
		String path = args[0];
		String nClusters = args[1];
		File f = new File(path);
		if(!f.exists()){
			throw new Exception(String.format("The image file %s doesn't exist", path));
		}
		this.image = ImageIO.read(f);	
		this.nClusters = Integer.parseInt(nClusters);
	}
	private double removeAlpha(int rgb){
		return (double) (rgb & (int)(0x00FFFFFF));
	}
	public void run() throws Exception {
		int width = this.image.getWidth(null);
		int height = this.image.getHeight(null);
		int pixels[] = this.image.getRGB(0, 0, width, height, null, 0, width);
		boolean useReal = false;
		boolean useGrey = true;
		List<Observation> obs = new ArrayList<>(pixels.length);
		GreyScale g = new GreyScale();
		int[] greyTable = g.getGreyTable();
		for(int i = 0; i < height; i++){
			int offset = i * width;
			for(int j = 0; j < width; j++){
				int p = pixels[offset+j];
				if(useReal){
					// just get rid off alpha
					// we dont need to determine colours
					p = p & 0x00FFFFFF;
					obs.add(new Observation(p, j, i));
				}
				if(useGrey){
					int index = g.getGreyScaleIndex(p);
					int greyPixel = greyTable[index];
					obs.add(new Observation(greyPixel, j, i));
				}
			}
		}
		if(useReal){
			int c = 2;
			int empty = 0;
			List<Cluster> colours = new LinkedList<>();
			colours.add(new Cluster(1,removeAlpha(Color.black.getRGB())));
			colours.add(new Cluster(2,removeAlpha(Color.blue.getRGB())));
			colours.add(new Cluster(3,removeAlpha(Color.cyan.getRGB())));
			colours.add(new Cluster(5,removeAlpha(Color.gray.getRGB())));
			colours.add(new Cluster(6,removeAlpha(Color.green.getRGB())));
			colours.add(new Cluster(8,removeAlpha(Color.magenta.getRGB())));
			colours.add(new Cluster(9,removeAlpha(Color.orange.getRGB())));
			colours.add(new Cluster(10,removeAlpha(Color.pink.getRGB())));
			colours.add(new Cluster(11,removeAlpha(Color.red.getRGB())));
			colours.add(new Cluster(13,removeAlpha(Color.yellow.getRGB())));
			KMeans k = new KMeans(colours.size());
			System.out.printf("Running with %d observations, %d clusters\n", obs.size(), colours.size());
			k.init(colours);
			k.run(obs, 1000);
			if(k.isConverged()){
				System.out.printf("Converged\n");
				int tally = 0;
				for(Cluster cl: colours){
					System.out.printf("%s\n", String.valueOf(cl));
					tally += cl.getObservations().size();
				}
				if(tally != obs.size()){
					throw new Exception("Observation count failed to tally");
				}
				BufferedImage paint = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				for(Cluster cl: colours){
					int rgb =	(int) Math.floor(cl.getMean());
					int argb = rgb | 0xFF000000;
					for(Observation o: cl.getObservations()){
						int x = o.getX();
						int y = o.getY();
						paint.setRGB(x, y, argb);
					}
				}
				ImageIO.write(paint, "png", new File("result.png"));
			}else{
				System.out.printf("Didn't converge\n");
			}	
			return;
		}
		if(useGrey){
			long seed = new Random().nextLong();
			System.out.printf("Using grey scale, seed is %d\n", seed);
			KMeans k = new KMeans(this.nClusters);
			List<Observation> all = new LinkedList<>();
			k.init(obs, seed);
			k.run(obs, 10000);
			if(k.isConverged()){
				System.out.printf("Converged\n");
				int[][] matrix = new int[width][height];
				for(Cluster cl: k.getClusters()){
					int rgb =	(int) Math.floor(cl.getMean());
					int index = g.getGreyScaleIndex(rgb);
					rgb = greyTable[index];
					int argb = 0x33000000 | (rgb & 0x00FFFFFF);
					for(Observation o: cl.getObservations()){
						int x = o.getX();
						int y = o.getY();
						matrix[x][y] = argb;
						o.setPixel(argb);
						all.add(o);
					}
				}
				// group pixels into like colour and adjacent groups
				// points aren't comparable ... so need to use strings unfortunately
				Set<String> seen = new TreeSet<>();
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
				System.out.printf("Total seen pixels, %d\n", seen.size());
				System.out.printf("Total groups, %d\n", groups.size());
				List<Set<String>> keep = new LinkedList<>();
				final int MIN_PIXELS = 20;
				int total = 0;
				for(Set<String> group: groups){
					// ignore anything too small
					if(group.size() <= MIN_PIXELS)
						continue;
					total += group.size();
					keep.add(group);
				}
				System.out.printf("Total kept groups, %d\n", keep.size());
				double average = total/(double)keep.size();
				System.out.printf("Average %f\n", average);
				for(Set<String> g_: keep){
					Map<Integer, List<Integer>> mapX = new TreeMap<>(); // unique x to y values
					Map<Integer, List<Integer>> mapY = new TreeMap<>(); // unique y to x values
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
					writeImage(matrix);
				}
			}else{
				System.out.printf("Failed to converge\n");
			}
		}
	}
	private void writeImage(int[][] matrix) throws Exception {
		BufferedImage image = new BufferedImage(matrix.length, matrix[0].length, BufferedImage.TYPE_INT_ARGB);
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[i].length; j++){
				int pixel = matrix[i][j];
				pixel = pixel == 0x55000000 ? pixel : 0xFFFFFFFF;
				image.setRGB(i, j, pixel);	
			}
		}
		ImageIO.write(image, "png", new File("grey.png"));
	}
	private String getStringFromPoint(Point p){
		return String.format("%d,%d", p.x, p.y);
	}
	private Point getPointFromString(String s){
		String[] tokens = s.split(",");
		String x = tokens[0];
		String y = tokens[1];
		int nX = Integer.parseInt(x);
		int nY = Integer.parseInt(y);
		return new Point(nX, nY);
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
	private Set<String> findAdjacentPixels(Set<String> seen, Point origin, Integer pixel, int[][] matrix){
		Set<String> group = new TreeSet<>(); // points aren't comparable ...
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
	public static void main(String[] args){
		Main m = new Main();
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
