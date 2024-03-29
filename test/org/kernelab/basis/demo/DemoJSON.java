package org.kernelab.basis.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.ParseException;

import org.kernelab.basis.JSON;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Tools;

public class DemoJSON
{
	public static void main(String[] args) throws IOException, ParseException
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
				) //
				.attr("g", "2022-07-12T12:15:07.077Z") //
				.attr("h", new Object[] { 1, 2.1, "3", true });
		Tools.debug(jsonA.valDouble("d"));
		Tools.debug(jsonA.toString(0));
		Tools.debug(jsonA.values());
		Tools.debug(jsonA.entrySet());
		Tools.debug(jsonA.keySet());

		Tools.debug(jsonA.valTimestamp("g"));
		Tools.debug(jsonA.valCalendar("g"));
		Tools.debug(jsonA.val("h"));

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

		JSAN table = new JSAN();
		table.add(new JSAN().addAll(0, "a", 1, true, null)) //
				.add(new JSAN().addAll(0, "b", 2, false, new JSON().attr("a", 1)));
		JSON.Output(new PrintWriter(System.out, true), table, 0, "  ");
	}
}
