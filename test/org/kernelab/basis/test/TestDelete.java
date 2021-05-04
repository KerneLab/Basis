package org.kernelab.basis.test;

import java.io.File;

import org.kernelab.basis.Tools;

public class TestDelete
{

	public static void main(String[] args)
	{
		File path = new File("E:\\test\\a");

		Tools.debug(Tools.delete(path));
	}

}
