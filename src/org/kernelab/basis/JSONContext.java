package org.kernelab.basis;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONContext extends JSON
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -7456943054258519317L;

	public static final Matcher	VAR_ENTRY_MATCHER	= Pattern
															.compile(
																	"^\\s*?(var\\s+)?\\s*?(\\S+)\\s*?=\\s*(.*)$")
															.matcher("");

	public static final Matcher	VAR_EXIT_MATCHER	= Pattern.compile(
															"^\\s*(.*?)\\s*;\\s*$")
															.matcher("");

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		File file = new File("E:/project/JSON/test.txt");
		JSONContext context = new JSONContext();
		context.read(file);
		Tools.debug(context.attrJSAN("c").attr(0).toString());
		// Tools.debug(context.attrJSON("c").attrJSON("a b").attrJSAN("k").attrJSON(0)
		// .attrJSON("Hey").context());
	}

	private DataReader	reader;

	public JSONContext()
	{
		reader = new DataReader() {

			private String			entry	= null;

			private StringBuilder	buffer	= new StringBuilder();

			@Override
			protected void readFinished()
			{

			}

			@Override
			protected void readLine(CharSequence line)
			{
				buffer.append(line);

				if (entry == null) {
					if (VAR_ENTRY_MATCHER.reset(buffer).lookingAt()) {
						entry = VAR_ENTRY_MATCHER.group(2);
						line = VAR_ENTRY_MATCHER.group(3);
						Tools.clearStringBuilder(buffer);
						buffer.append(line);
					}
				}

				if (entry != null) {

					if (VAR_EXIT_MATCHER.reset(buffer).lookingAt()) {

						line = VAR_EXIT_MATCHER.group(1);
						Tools.clearStringBuilder(buffer);
						buffer.append(line);

						Object object = JSON.Value(buffer.toString());

						if (object == JSON.NOT_A_VALUE) {
							object = JSON.Parse(buffer, null, JSONContext.this);
						}

						if (object != JSON.NOT_A_VALUE) {
							JSONQuotation.Quote(JSONContext.this, entry, object);
							entry = null;
							Tools.clearStringBuilder(buffer);
						}
					}
				}
			}

			@Override
			protected void readPrepare()
			{
				entry = null;
			}
		};
	}

	@Override
	public JSONContext context()
	{
		return this;
	}

	protected DataReader getReader()
	{
		return reader;
	}

	public JSONContext read(File file)
	{
		try {
			reader.setDataFile(file).read();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return this;
	}

	protected void setReader(DataReader reader)
	{
		this.reader = reader;
	}

}
