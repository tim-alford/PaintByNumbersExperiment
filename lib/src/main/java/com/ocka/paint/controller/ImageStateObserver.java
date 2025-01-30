// vim: ts=2
package com.ocka.paint.controller;
import java.awt.image.*;
public interface ImageStateObserver { 
	public void onImageChanged(BufferedImage i) throws Exception;
}
