package org.kernelab.basis.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Tools;

public class DemoJSON
{
	public static void main(String[] args) throws IOException
	{
		JSON jsonA = new JSON() //
				.attr("a", "One") //
				.attr("b", 2) //
				.attr("c", true)//
				.attr("d", "3.1");
		Tools.debug(jsonA.valDouble("d"));
		Tools.debug(jsonA.toString(0));
		Tools.debug(jsonA.values());
		Tools.debug(jsonA.entrySet());
		Tools.debug(jsonA.keySet());

		JSON jsonB = new JSON();
		jsonB.attrAll(jsonA);
		Tools.debug(jsonB.toString(0));

		JSAN jsanA = new JSAN().add("one").add("2").add(true).add(0.1);
		jsanA.attr("k1", 2);
		Writer out = new PrintWriter(System.out);
		JSON.Serialize(jsanA, out, 0);
		out.flush();
	}
}
