package org.kernelab.basis.test;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Canal.Item;
import org.kernelab.basis.JSON.JSAN;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.Row;

public class TestWindowFunction
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		Row[] data = new Row[] { //
				new Row().set("id", 1).set("name", "mike").set("gender", 1).set("age", 28).set("income", 1261.54), //
				new Row().set("id", 2).set("name", "rose").set("gender", 2).set("age", 32).set("income", 3324.55), //
				new Row().set("id", 3).set("name", "tom").set("gender", 1).set("age", 28).set("income", 7531.43), //
				new Row().set("id", 4).set("name", "hellen").set("gender", 2).set("age", 30).set("income", 4483.12), //
				new Row().set("id", 5).set("name", "jack").set("gender", 1).set("age", 31).set("income", 5331.50), //
		};

		// Tools.debug(Canal.of(data).toRows().stratifyBy(Canal.Canal.$("gender")).collectAsJSAN().toString(0));

		JSAN j = Canal.of(data).toRows(Row.class).window(Canal.COUNT(Canal.item(1)).partBy(Canal.$("gender")).as("cnt"), //
				Canal.ROW_NUMBER().partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("rn"), //
				Canal.LEAD(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("lea2"), //
				Canal.LAG(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("lag2"), //
				Canal.SUM((Item<Double>) Canal.$("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age"))
						.as("sum2"), //
				Canal.SUM((Item<Double>) Canal.$("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).rows()
						.between(Canal.CURRENT_ROW, Canal.following(1)).as("sum3"), //
				Canal.SUM((Item<Double>) Canal.$("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).range()
						.between(Canal.CURRENT_ROW, Canal.following(1)).as("sum4"), //
				Canal.SUM((Item<Double>) Canal.$("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).range()
						.between(Canal.preceding(1), Canal.item(0.0)).as("sum5"), //
				Canal.SUM((Item<Double>) Canal.$("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age").asc())
						.range().between(Canal.preceding(3), Canal.preceding(1)).as("sum6"), //
				Canal.LAST_VALUE(Canal.$("name")).partBy(Canal.$("gender")).as("las1"), //
				Canal.LAST_VALUE(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("las2"), //
				Canal.LAST_VALUE(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).rows()
						.between(Canal.CURRENT_ROW, Canal.following(1.0)).as("las3") //
		).collectAsJSAN();

		Tools.debug(j.toString().replace(",{", ",\n{"));
	}
}
