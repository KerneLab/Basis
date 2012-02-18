package org.kernelab.basis.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.kernelab.basis.DataReader;
import org.kernelab.basis.Tools;

/**
 * 设想有一个文本文件data.txt<br />
 * 其中各行有两列数据，以"\t"分割，如：<br />
 * 
 * <pre>
 * name	John
 * age	23
 * gender	男
 * ...	...
 * </pre>
 * 
 * 通过DataReader可将该文本的两列对应的读取到两个字符串列表中。
 * 
 * @author Dilly King
 * 
 */
public class DemoDataReader
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// 设想将两列数据读到a、b这两个列表中
		final List<String> a = new LinkedList<String>();
		final List<String> b = new LinkedList<String>();

		// 定义数据阅读器
		DataReader reader = new DataReader() {

			@Override
			protected void readFinished()
			{
				// 读完文件后的一些后续工作可以写在这里
			}

			@Override
			protected void readLine(CharSequence line)
			{
				// 对于文本中的每一行，以"\t"分割成2列
				String[] pair = Tools.splitCharSequence(line, "\t", 2);
				if (pair.length == 2) {
					a.add(pair[0]); // 第一列的元素加入到列表a的末尾
					b.add(pair[1]); // 第二列的元素加入到列表b的末尾
				}
			}

			@Override
			protected void readPrepare()
			{
				// 读取文件前的一些准备工作可以写在这里
			}
		};

		try {
			// 为reader设置要读取的文件"data.txt"，并使用“链式”调用方式开始读取。
			reader.setDataFile(new File("data.txt"), "GBK").read();

			// 到这里已经将两列数据读到a、b两个列表中了
		} catch (FileNotFoundException e) {
			e.printStackTrace(); // 如果文件不存在则会执行这里
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
