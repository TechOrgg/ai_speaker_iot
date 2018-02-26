package com.io;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 * LED 제어를 위한 데이터를 관리하다.
 */
public class LEDBar
{
	private short [][] ledData;
	private int repeatCount;
	private int intervalTime;
	private int repeatIndex;
	private int currentIndex;
	
	public LEDBar(int repeat, int interval, short [][] data)
	{
		this.repeatCount = repeat;
		this.intervalTime = interval;
		this.ledData = data;
		
		repeatIndex = 0;
		currentIndex = 0;
	}
	
	/**
	 * 다음 위치로 이동한다.
	 * 
	 * @return
	 */
	public boolean moveNext()
	{
		currentIndex++;
		if ( currentIndex >= ledData.length ) {
			currentIndex = 0;
			repeatIndex++;
		}
		
		if ( repeatCount == 0 ) {
			return true;
		}
		else {
			if ( repeatIndex >= repeatCount )
				return false;
			else
				return true;
		}
	}
	
	/**
	 * 현재 위치의 값을 반환
	 * 
	 * @return
	 */
	public short [] currentLedValue()
	{
		return ledData[currentIndex];
	}
	
	/**
	 * @return
	 */
	public int getIntervalTime()
	{
		return intervalTime;
	}

}
