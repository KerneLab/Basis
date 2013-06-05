package org.kernelab.basis;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ByteOrderMarkScanner
{
	public static final int					BOM_BYTES		= 4;

	public static final byte[]				BOM_UTF_16LE	= new byte[] { (byte) 0xFF, (byte) 0xFE };

	public static final byte[]				BOM_UTF_16BE	= new byte[] { (byte) 0xFE, (byte) 0xFF };

	public static final byte[]				BOM_UTF_8		= new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	public static final byte[]				BOM_UTF_1		= new byte[] { (byte) 0xF7, (byte) 0x64, (byte) 0x4C };

	public static final byte[]				BOM_SCSU		= new byte[] { (byte) 0x0E, (byte) 0xFE, (byte) 0xFF };

	public static final byte[]				BOM_BOCU_1		= new byte[] { (byte) 0xFB, (byte) 0xEE, (byte) 0x28 };

	public static final byte[]				BOM_UTF_32LE	= new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00,
			(byte) 0x00									};

	public static final byte[]				BOM_UTF_32BE	= new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE,
			(byte) 0xFF									};

	public static final byte[]				BOM_UTF_EBCDIC	= new byte[] { (byte) 0xDD, (byte) 0x73, (byte) 0x66,
			(byte) 0x73									};

	public static final byte[]				BOM_GB18030		= new byte[] { (byte) 0x84, (byte) 0x31, (byte) 0x95,
			(byte) 0x33									};

	public static final byte[]				BOM_UTF_7		= new byte[] { (byte) 0x2B, (byte) 0x2F, (byte) 0x76,
			(byte) 0x38									};

	public static final Map<String, byte[]>	BOMS			= new LinkedHashMap<String, byte[]>();

	static
	{
		BOMS.put("UTF-16LE", BOM_UTF_16LE);
		BOMS.put("UTF-16BE", BOM_UTF_16BE);
		BOMS.put("UTF-8", BOM_UTF_8);
		BOMS.put("UTF-1", BOM_UTF_1);
		BOMS.put("SCSU", BOM_SCSU);
		BOMS.put("BOCU-1", BOM_BOCU_1);
		BOMS.put("UTF-32LE", BOM_UTF_32LE);
		BOMS.put("UTF-32BE", BOM_UTF_32BE);
		BOMS.put("UTF-EBCDIC", BOM_UTF_EBCDIC);
		BOMS.put("GB18030", BOM_GB18030);
		BOMS.put("UTF-7", BOM_UTF_7);
		BOMS.put("UTF-7|1", new byte[] { (byte) 0x2B, (byte) 0x2F, (byte) 0x76, (byte) 0x39 });
		BOMS.put("UTF-7|2", new byte[] { (byte) 0x2B, (byte) 0x2F, (byte) 0x76, (byte) 0x2B });
		BOMS.put("UTF-7|3", new byte[] { (byte) 0x2B, (byte) 0x2F, (byte) 0x76, (byte) 0x2F });
	}

	public static final byte[] getBOM(Charset charset)
	{
		return BOMS.get(charset.name());
	}

	public static final byte[] getBOM(String charsetName)
	{
		return getBOM(Charset.forName(charsetName));
	}

	public static final boolean samePrefix(byte[] a, byte[] b)
	{
		boolean is = false;

		if (a != null && b != null)
		{
			int len = Math.min(a.length, b.length);

			is = true;

			for (byte i = 0; i < len; i++)
			{
				if (a[i] != b[i])
				{
					is = false;
					break;
				}
			}
		}

		return is;
	}

	private Charset				charset;

	private InputStreamReader	reader;

	public Charset getCharset()
	{
		return charset;
	}

	public InputStreamReader getReader()
	{
		return reader;
	}

	public ByteOrderMarkScanner scan(InputStream is) throws IOException
	{
		return scan(is, Charset.defaultCharset());
	}

	public ByteOrderMarkScanner scan(InputStream is, Charset defaultCharset) throws IOException
	{
		Charset charset = defaultCharset;

		PushbackInputStream scanner = new PushbackInputStream(is, BOM_BYTES);

		byte[] bytes = new byte[BOM_BYTES];

		int reads = scanner.read(bytes);

		String charsetName = null;

		for (Entry<String, byte[]> entry : BOMS.entrySet())
		{
			if (samePrefix(bytes, entry.getValue()))
			{
				int len = entry.getValue().length;

				scanner.unread(bytes, len, reads - len);

				charsetName = entry.getKey().replaceFirst("^(.+?)(?:\\|.*)$", "$1");

				break;
			}
		}

		if (charsetName == null)
		{
			scanner.unread(bytes, 0, reads);
		}
		else
		{
			try
			{
				charset = Charset.forName(charsetName);
			}
			catch (Exception e)
			{
				charset = defaultCharset;
			}
		}

		return this.setCharset(charset).setReader(scanner);
	}

	public ByteOrderMarkScanner scan(InputStream is, String defaultCharsetName) throws IOException
	{
		return scan(is, Charset.forName(defaultCharsetName));
	}

	private ByteOrderMarkScanner setCharset(Charset charset)
	{
		this.charset = charset;
		return this;
	}

	private ByteOrderMarkScanner setReader(PushbackInputStream is)
	{
		this.reader = new InputStreamReader(is, charset);
		return this;
	}
}
