// vim: ts=2
package com.ocka.paint.controller;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
public class FrameGenerator implements ImageStateObserver {
	private int sequence = 0;
	@Override
	public void onImageChanged(BufferedImage i) throws Exception {
		ImageIO.write(i, "png", new File(String.format("frame.%03d.png", this.sequence++)));
	}
}
