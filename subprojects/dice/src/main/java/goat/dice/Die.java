package goat.dice;


/**
 * A die is simply a size of die (ie 6) and the result from throwing it (if it is thrown already).
 * @author Barry Corrigan 16/5/2007
 *
 */
public class Die {
	private int size;
	private int result;
	
	public Die(int size) {
		this.size = size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	public int getSize() {
		return size;
	}
	public void setResult(int result) {
		this.result = result;
	}
	public int getResult() {
		return result;
	}
	
	public void throwDie() {
		if( getSize()==0 )
			result = 0;
		else 
			result = (int) (Math.random()*getSize() + 1);
	}
}
