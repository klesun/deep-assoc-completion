package org.klesun.lib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PhpToolbox {

	/**
	 * @author Daniel Espendiller <daniel@espendiller.net>
	 * Provide support for some more user friendly use cases
	 * <p>
	 * com.intellij.icons.AllIcons$Actions.Back -> com.intellij.icons.AllIcons.Actions.Back
	 */
	@Nullable
	private static Class<?> findClass(@NotNull String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException ignored) {
		}

		int i = className.lastIndexOf(".");
		if (i <= 0) {
			return null;
		}

		className = className.substring(0, i) + "$" + className.substring(i + 1);
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * @author Daniel Espendiller <daniel@espendiller.net>
	 */
	@Nullable
	public static Icon getLookupIconOnString(@NotNull String icon) {

		int endIndex = icon.lastIndexOf(".");
		if (endIndex < 0 || icon.length() - endIndex < 1) {
			return null;
		}

		String className = icon.substring(0, endIndex);

		Class<?> iconClass = findClass(className);
		if (iconClass == null) {
			return null;
		}

		try {
			java.lang.reflect.Field field = iconClass.getDeclaredField(icon.substring(endIndex + 1));
			return ((Icon) field.get(null));

		} catch (IllegalAccessException | NoSuchFieldException e) {
			return null;
		}
	}
}
