package org.kernelab.basis.demo;

import java.io.File;

import org.kernelab.basis.ProcessHandler;

public class DemoProcessHandler
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ProcessHandler ph = new ProcessHandler();

		// 设定执行的命令
		ph.setCommand("cmd", "/c", "dir");
		// 设定执行命令的环境目录
		ph.setDirectory(new File("D:"));
		// 设定执行的打印输出流
		ph.setOutputStream(System.out);

		// 上述三个设定等价于下面的链式风格
		ph.setCommand("cmd", "/c", "dir").setDirectory(new File("D:")).setOutputStream(System.out);

		// 同步执行
		ph.run();

		// 异步执行
		new Thread(ph).start();
	}

}
