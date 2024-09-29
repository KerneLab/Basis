package org.kernelab.basis;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

public class Accessor
{
	public static Accessor Of(Class<?> cls, String name)
	{
		return cls != null && name != null ? new Accessor(cls, name) : null;
	}

	public static Accessor Of(Field field)
	{
		return field != null ? Of(field.getDeclaringClass(), field.getName()) : null;
	}

	private Class<?>				cls;

	private String					name;

	private Field					field;

	private Member					getter;

	private Map<Class<?>, Member>	setter	= new HashMap<Class<?>, Member>();

	public Accessor(Class<?> cls, String name)
	{
		this.init(cls, name);
	}

	protected Member findSetter(Class<?> paramType)
	{
		if (paramType == null)
		{
			paramType = Tools.getAccessorType(this.getGetter());
		}

		if (paramType != null)
		{
			Member setter = this.getSetter(paramType);

			if (setter == null && !this.getSetter().containsKey(paramType))
			{
				setter = Tools.accessor(this.getDeclaringClass(), this.getName(), paramType);

				this.setSetter(paramType, setter);
			}

			if (setter != null)
			{
				return setter;
			}
		}

		return this.getDefaultSetter();
	}

	@SuppressWarnings("unchecked")
	public <T, E> E get(T object) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		if (this.getGetter() != null)
		{
			return Tools.access(object, this.getGetter());
		}

		if (this.getField() != null)
		{
			return (E) this.getField().get(object);
		}

		throw new IllegalAccessException(this.getName());
	}

	public Class<?> getDeclaringClass()
	{
		return cls;
	}

	protected Member getDefaultSetter()
	{
		if (this.getField() != null)
		{
			return this.getSetter(this.getField().getType());
		}
		else
		{
			return null;
		}
	}

	public Field getField()
	{
		return field;
	}

	public Member getGetter()
	{
		return getter;
	}

	public String getName()
	{
		return name;
	}

	protected Map<Class<?>, Member> getSetter()
	{
		return setter;
	}

	public Member getSetter(Class<?> paramType)
	{
		return this.getSetter().get(paramType);
	}

	protected void init(Class<?> cls, String name)
	{
		this.cls = cls;
		this.name = name;

		Field field = Tools.fieldOf(cls, name);
		this.setField(field);

		this.setGetter(Tools.accessor(cls, name));

		this.getSetter().clear();
		if (field != null)
		{
			this.setSetter(field.getType(), Tools.accessor(cls, name, field, field.getType()));
		}
	}

	public <T> T set(T object, Object value)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Class<?> paramType = value == null ? null : value.getClass();

		Member setter = this.findSetter(paramType);

		Tools.access(object, setter, value);

		return object;
	}

	protected void setField(Field field)
	{
		this.field = field;
	}

	protected void setGetter(Member getter)
	{
		this.getter = getter;
	}

	protected void setSetter(Class<?> paramType, Member setter)
	{
		this.getSetter().put(paramType, setter);
	}
}
