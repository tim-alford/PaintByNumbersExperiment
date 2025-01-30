// vim: ts=2
package com.ocka.paint.model;
import java.awt.image.*;
public interface IPaintByNumbers {
	public BufferedImage paint(BufferedImage image, Boolean greyScale, Integer colours) throws Exception;
}
