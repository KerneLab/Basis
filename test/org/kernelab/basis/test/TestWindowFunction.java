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
				new Row("id", 1, "name", "mike", "gender", 1, "age", 28, "income", 1261.54), //
				new Row("id", 2, "name", "rose", "gender", 2, "age", 32, "income", 3324.55), //
				new Row("id", 3, "name", "tom", "gender", 1, "age", 28, "income", 7531.43), //
				new Row("id", 4, "name", "hellen", "gender", 2, "age", 30, "income", 4483.12), //
				new Row("id", 5, "name", "jack", "gender", 1, "age", 31, "income", 5331.50), //
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
