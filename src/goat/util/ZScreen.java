// Z Machine V3/V4/V5 Runtime 
//
// Copyright 2002, Brian J. Swetland <swetland@frotz.net>
// Available under a BSD-Style License.  Share and Enjoy.
//
// Front-end Interface.
// The back-end in ZMachine.java is platform-agnostic

//turned this into an interface, was a class before, but java has no multiple inheritance
//and need to inherit this in goat.module.Adventure as well as goat.core.Module ~ bc

package goat.util;

public abstract interface ZScreen
{
	//public ZScreen();

	public void NewLine();
	public void Print(char data[], int len);

	public int Read();
	public int ReadLine(char buffer[]);

	public void exit();
	public int Random(int limit);
	public void SetStatus(char line[], int len);

	public int GetWidth();
	public int GetHeight();
	
	public void Restart();
	public boolean Save(byte state[]);
	public byte[] Restore();

	public void SetWindow(int num);
	public void SplitWindow(int height);
	public void EraseWindow(int number);
	public void MoveCursor(int x, int y);
	
	public void PrintNumber(int num);

	public void PrintChar(int ch);

}
