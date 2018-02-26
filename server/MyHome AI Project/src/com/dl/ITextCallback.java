package com.dl;

import com.google.gson.JsonElement;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public interface ITextCallback
{
	/**
	 * @param resText
	 * @param actionResult
	 * @param postResult
	 */
	public void setResult(String resText, Object actionResult, JsonElement postResult);
}
