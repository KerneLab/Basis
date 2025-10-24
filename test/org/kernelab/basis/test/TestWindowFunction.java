package org.kernelab.basis.test;

import java.math.BigDecimal;

import org.kernelab.basis.Canal;
import org.kernelab.basis.Canal.Action;
import org.kernelab.basis.Mapper;
import org.kernelab.basis.Tools;
import org.kernelab.basis.sql.Row;

public class TestWindowFunction
{
	public static void main(String[] args)
	{
		test1();
	}

	/**
	 * <pre>
	SELECT * FROM (
	SELECT
	t.*,
	count(1) OVER(PARTITION BY t.gender) cnt,
	ROW_NUMBER() OVER(PARTITION BY t.gender ORDER BY t.age) rn,
	lead(t.name) over(PARTITION BY t.gender ORDER BY t.age) lea2,
	lag(t.name) over(PARTITION BY t.gender ORDER BY t.age) lag2,
	sum(t.INCOME) over(PARTITION BY t.gender) sum1,
	sum(t.income) over(PARTITION BY t.gender ORDER BY t.age) sum2,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN CURRENT ROW AND UNBOUNDED following) sum3_1,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT row) sum3_2,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN UNBOUNDED PRECEDING AND 1 following) sum3_3,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN 1 PRECEDING AND unbounded following) sum3_4,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN 1 following AND unbounded following) sum3_5,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN UNBOUNDED PRECEDING AND 1 preceding) sum3_6,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN 3 PRECEDING AND 1 preceding) sum3_7,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN 1 FOLLOWING AND 3 following) sum3_8,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc ROWS BETWEEN 1 preceding AND 1 following) sum3_9,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN CURRENT ROW AND UNBOUNDED following) sum4_1,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT row) sum4_2,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN UNBOUNDED PRECEDING AND 1 following) sum4_3,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN 1 PRECEDING AND unbounded following) sum4_4,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN 1 following AND unbounded following) sum4_5,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN UNBOUNDED PRECEDING AND 1 preceding) sum4_6,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN 3 PRECEDING AND 1 preceding) sum4_7,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN 1 FOLLOWING AND 3 following) sum4_8,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN 1 preceding AND 1 following) sum4_9,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN CURRENT ROW AND UNBOUNDED following) sum5_1,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN UNBOUNDED PRECEDING AND CURRENT row) sum5_2,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN UNBOUNDED PRECEDING AND 1 following) sum5_3,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN 1 PRECEDING AND unbounded following) sum5_4,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN 1 following AND unbounded following) sum5_5,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN UNBOUNDED PRECEDING AND 1 preceding) sum5_6,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN 3 PRECEDING AND 1 preceding) sum5_7,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN 1 FOLLOWING AND 3 following) sum5_8,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age desc range BETWEEN 1 preceding AND 1 following) sum5_9,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN CURRENT ROW AND UNBOUNDED following) sum6_1,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN UNBOUNDED PRECEDING AND CURRENT row) sum6_2,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN UNBOUNDED PRECEDING AND 1 following) sum6_3,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN 1 PRECEDING AND unbounded following) sum6_4,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN 1 following AND unbounded following) sum6_5,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN UNBOUNDED PRECEDING AND 1 preceding) sum6_6,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN 3 PRECEDING AND 1 preceding) sum6_7,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN 1 FOLLOWING AND 3 following) sum6_8,
	sum(t.INCOME) over(PARTITION BY t.gender ORDER BY t.age range BETWEEN 1 preceding AND 1 following) sum6_9,
	last_value(t.name) over(PARTITION BY t.gender) las1,
	last_value(t.name) over(PARTITION BY t.gender ORDER BY t.age) las2,
	last_value(t.name) over(PARTITION BY t.gender ORDER BY t.age ROWS BETWEEN CURRENT ROW AND 1 following) las3
	FROM
	(SELECT * FROM jdl_test_tab t ORDER BY t.id) t
	) s ORDER BY s.id
	 * </pre>
	 */
	@SuppressWarnings("unchecked")
	public static void test1()
	{
		Row[] data = new Row[] { //
				new Row("id", 1, "name", "mike", "gender", 1, "age", 28, "income", new BigDecimal("1261.54")), //
				new Row("id", 2, "name", "rose", "gender", 2, "age", 32, "income", new BigDecimal("3324.55")), //
				new Row("id", 3, "name", "tom", "gender", 1, "age", 28, "income", new BigDecimal("7531.43")), //
				new Row("id", 4, "name", "hellen", "gender", 2, "age", 30, "income", new BigDecimal("4483.12")), //
				new Row("id", 5, "name", "jack", "gender", 1, "age", 31, "income", new BigDecimal("5331.50")), //
		};

		// Tools.debug(Canal.of(data).toRows().stratifyBy(Canal.Canal.$("gender")).collectAsJSAN().toString(0));

		Canal.of(data).toRows(Row.class).window(Canal.wf.COUNT(Canal.wf.item(1)).partBy(Canal.$("gender")).as("cnt"), //
				Canal.wf.ROW_NUMBER().partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("rn"), //
				Canal.wf.LEAD(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("lea2"), //
				Canal.wf.LAG(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("lag2"), //
				Canal.wf.SUM(Canal.<BigDecimal> $("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age"))
						.as("sum2"), //
				Canal.wf.SUM(Canal.<BigDecimal> $("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).rows()
						.between(Canal.wf.CURRENT_ROW, Canal.wf.following(1)).as("sum3"), //
				Canal.wf.SUM(Canal.<BigDecimal> $("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).range()
						.between(Canal.wf.CURRENT_ROW, Canal.wf.following(1)).as("sum4"), //
				Canal.wf.SUM(Canal.<BigDecimal> $("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).range()
						.between(Canal.wf.preceding(1), Canal.wf.CURRENT_ROW).as("sum5"), //
				Canal.wf.SUM(Canal.<BigDecimal> $("income")).partBy(Canal.$("gender")).orderBy(Canal.$("age").asc())
						.range().between(Canal.wf.preceding(3), Canal.wf.preceding(1)).as("sum6"), //
				Canal.wf.LAST_VALUE(Canal.$("name")).partBy(Canal.$("gender")).as("las1"), //
				Canal.wf.LAST_VALUE(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).as("las2"), //
				Canal.wf.LAST_VALUE(Canal.$("name")).partBy(Canal.$("gender")).orderBy(Canal.$("age")).rows()
						.between(Canal.wf.CURRENT_ROW, Canal.wf.following(1.0)).as("las3") //
		).sortBy(Canal.$("id")).map(new Mapper<Row, String>()
		{
			@Override
			public String map(Row el) throws Exception
			{
				return Canal.of(el.values()).map(new Mapper<Object, String>()
				{
					@Override
					public String map(Object el) throws Exception
					{
						return String.valueOf(el);
					}
				}).toString("\t");
			}
		}).foreach(new Action<String>()
		{
			@Override
			public void action(String el) throws Exception
			{
				Tools.debug(el);
			}
		});
	}
}
