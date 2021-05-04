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
				.attr("a", "On\ne") //
				.attr("b", 2) //
				.attr("c", true)//
				.attr("d", "3.1") //
				.attr("e", null) //
				.attr("f", new JSON() //
						.attr("r", 1.5) //
						.attr("s", "Sss") //
						.attr("t", true) //
				);
		Tools.debug(jsonA.valDouble("d"));
		Tools.debug(jsonA.toString(0));
		Tools.debug(jsonA.values());
		Tools.debug(jsonA.entrySet());
		Tools.debug(jsonA.keySet());

		JSON jsonB = new JSON();
		jsonB.attrAll(jsonA);
		Tools.debug(jsonB.toString(0));

		JSON jsonC = JSON.Parse(jsonB.toString());
		Tools.debug(jsonC.toString(0));

		JSAN jsanA = new JSAN().add("one").add("1").add(true).add(0.1);
		jsanA.attr("k1", 2);
		jsanA.addLast(3, null, "yes");
		Writer out = new PrintWriter(System.out);
		JSON.Serialize(jsanA, out, 0);
		out.write('\n');
		out.flush();

		JSON jsonD = new JSON().pairs("a", jsonA, "AA", jsanA);
		Tools.debug(jsonD.flatten().toString(0));
	}
}
