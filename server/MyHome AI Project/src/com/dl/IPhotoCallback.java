package com.dl;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public interface IPhotoCallback
{
	/**
	 * @param found
	 * @param food
	 */
	public void setResult(boolean found, String food);
}
