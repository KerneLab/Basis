package org.kernelab.basis.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

public interface ReaderFactory
{
	public static class DefaultReaderFactory implements ReaderFactory
	{
		private Reader reader;

		public DefaultReaderFactory(Reader reader)
		{
			this.setReader(reader);
		}

		public Reader getReader() throws IOException
		{
			return reader;
		}

		protected void setReader(Reader reader)
		{
			this.reader = reader;
		}
	}

	public static class FileReaderFactory implements ReaderFactory
	{
		private File	file;

		private Charset	charset;

		public FileReaderFactory(File file, Charset charset)
		{
			this.setFile(file);
			this.setCharset(charset);
		}

		public Charset getCharset()
		{
			return charset;
		}

		public File getFile()
		{
			return file;
		}

		public Reader getReader() throws IOException
		{
			return new InputStreamReader(new FileInputStream(this.getFile()), this.getCharset());
		}

		protected void setCharset(Charset charset)
		{
			this.charset = charset;
		}

		protected void setFile(File file)
		{
			this.file = file;
		}
	}

	public Reader getReader() throws IOException;
}
