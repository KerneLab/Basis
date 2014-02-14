package org.kernelab.basis.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextArea;

public class JTextAreaOutputStream extends OutputStream
{

	public static final int	APPEND	= 0;
	public static final int	INSERT	= 1;

	private JTextArea		textArea;
	private int				mode;
	private byte[]			buffer;

	public JTextAreaOutputStream(JTextArea ta, int mode)
	{
		this.textArea = ta;
		this.mode = mode;
		this.buffer = new byte[1];
	}

	public int getMode()
	{
		return mode;
	}

	public PrintStream getPrintStream()
	{
		return new PrintStream(this);
	}

	public JTextArea getTextArea()
	{
		return textArea;
	}

	public void setMode(int mode)
	{
		this.mode = mode;
	}

	public void setTextArea(JTextArea textArea)
	{
		this.textArea = textArea;
	}

	public void write(byte data[]) throws IOException
	{
		switch (this.getMode())
		{
			case APPEND:
				textArea.append(new String(data));
				break;
			case INSERT:
				textArea.insert(new String(data), 0);
				break;
		}
	}

	public void write(byte data[], int off, int len) throws IOException
	{
		switch (this.getMode())
		{
			case APPEND:
				textArea.append(new String(data, off, len));
				textArea.setCaretPosition(textArea.getText().length());
				break;

			case INSERT:
				textArea.insert(new String(data, off, len), 0);
				textArea.setCaretPosition(0);
				break;
		}

	}

	@Override
	public void write(int b) throws IOException
	{
		buffer[0] = (byte) b;
		switch (this.getMode())
		{
			case APPEND:
				textArea.append(new String(buffer));
				break;
			case INSERT:
				textArea.insert(new String(buffer), 0);
				break;
		}
	}

}
