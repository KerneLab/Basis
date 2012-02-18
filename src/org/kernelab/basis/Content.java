package org.kernelab.basis;

public class Content
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

	}

	private String	content;

	public Content()
	{
		this("");
	}

	public Content(String content)
	{
		this.setContent(content);
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

}
