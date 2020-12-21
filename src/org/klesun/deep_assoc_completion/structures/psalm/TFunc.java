package org.klesun.deep_assoc_completion.structures.psalm;

import java.util.List;

import static org.klesun.lang.Lang.It;

public class TFunc implements IType
{
	final public List<Param> params;
	final public IType returnType;

	public TFunc(List<Param> params, IType returnType)
	{
		this.params = params;
		this.returnType = returnType;
	}

	public static class Param
	{
		final IType type;
		final Boolean isOptional;
		final Boolean isSpread;

		public Param(IType type, Boolean isOptional, Boolean isSpread) {
			this.type = type;
			this.isOptional = isOptional;
			this.isSpread = isSpread;
		}
	}

	@Override
	public String toString() {
		return "callable(" + It(params).str(", ") + "): " + returnType;
	}
}
