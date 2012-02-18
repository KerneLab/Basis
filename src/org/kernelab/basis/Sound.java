package org.kernelab.basis;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class Sound implements Runnable
{

	private URL			url;

	private AudioClip	sound;

	public Sound(File file) throws MalformedURLException
	{
		this(file.toURI().toURL());
	}

	public Sound(String url) throws MalformedURLException
	{
		this(new URL(url));
	}

	public Sound(URL url)
	{
		this.url = url;
	}

	public AudioClip getSound()
	{
		return sound;
	}

	public URL getURL()
	{
		return url;
	}

	public void load()
	{
		this.sound = Applet.newAudioClip(url);
	}

	public void play()
	{
		if (this.sound == null) {
			this.load();
		}

		this.getSound().play();
	}

	public void run()
	{
		this.play();
	}

	public void setURL(URL url)
	{
		this.url = url;
	}

	public void stop()
	{
		if (this.sound != null) {
			this.sound.stop();
		}
	}

}
