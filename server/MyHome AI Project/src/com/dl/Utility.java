package com.dl;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.Part;

import com.utils.SqlSessionManager;


/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public class Utility 
{
	public static final int RESIZE = 300;
	
	/**
	 * @param image
	 * @param jpeg
	 * @return
	 */
	public static byte [] resize(byte [] image, boolean jpeg)
	{
		byte [] resizeImage = null;
		
		try
		{
			BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(image));
			
			int scaledWidth = RESIZE;
			int scaledHeight = RESIZE;
			if ( inputImage.getWidth() > RESIZE )
			{
				scaledHeight = (int)((RESIZE/(float)inputImage.getWidth()) * inputImage.getHeight());
			}
			else if ( inputImage.getHeight() > RESIZE ) {
				scaledWidth = (int)((RESIZE/(float)inputImage.getHeight()) * inputImage.getWidth());;
			}
			
			BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());
			
			Graphics2D g2d = outputImage.createGraphics();
	        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
	        g2d.dispose();
			
	        ByteArrayOutputStream bo = new ByteArrayOutputStream();
	        if ( jpeg )
	        {
	        	ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
	        	ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
	        	jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	        	jpgWriteParam.setCompressionQuality(0.85f);

	        	ImageOutputStream outputStream = ImageIO.createImageOutputStream(bo);
	        	jpgWriter.setOutput(outputStream);
	        	IIOImage output = new IIOImage(outputImage, null, null);
	        	jpgWriter.write(null, output, jpgWriteParam);
	        	jpgWriter.dispose();	        	
	        }
	        else {
		        ImageIO.write(outputImage, "png", bo);
	        }
	        
	        resizeImage = bo.toByteArray();
	        bo.close();
	        bo = null;
		}catch(Exception e) {
			resizeImage = image;
		}

		return resizeImage;
	}
	
	/**
	 * @param seqName
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String getSequence(String seqName)
	{
		String seq = "0";
		
		try
		{
			Map parm = new HashMap();
			parm.put("seq_name", seqName);
			
			Map row = (Map)SqlSessionManager.getSqlMapClient().queryForObject("WINE.nextSequence", parm);
			
			SqlSessionManager.getSqlMapClient().update("WINE.updateSequence", parm);
			
//			seq = row.get("cur_val").toString();
			seq = row.get("CUR_VAL").toString();
			
		}catch(Exception ex) {
			ex.printStackTrace();
			try{
				SqlSessionManager.getSqlMapClient().endTransaction();
			}catch(Exception ig){}
		}finally{
			try{
				SqlSessionManager.getSqlMapClient().commitTransaction();
			}catch(Exception ig){}
		}

		return seq;
	}
	
	/**
	 * 파일명을 추출한다.
	 * 
	 * @param part
	 * @return
	 */
	public static String extractFileName(Part part)
	{
		String contentDisp = part.getHeader("content-disposition");
		String[] items = contentDisp.split(";");
		for (String s : items) {
			if (s.trim().startsWith("filename")) {
				return s.substring(s.indexOf("=") + 2, s.length() - 1);
			}
		}
		
		return null;
	}
	
	/**
	 * 파일명을 추출한다.
	 * 
	 * @param url
	 * @return
	 */
	public static String extractFileName(String url)
	{
		int ix = url.lastIndexOf("/");
		if ( ix != -1 )
			return url.substring(ix+1);
		else
			return url;
	}

}
