/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.formatter.tests;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.ui.formatter.IScriptFormatter;
import org.eclipse.dltk.utils.TextUtils;
import org.osgi.framework.Bundle;

public class ScriptedTest extends AbstractFormatterTest {

	public static interface IScriptedTestContext {
		Bundle getResourceBundle();

		String getCharset();

		IScriptFormatter createFormatter(Map preferences);

		String validateOptionName(String name);

		String validateOptionValue(String name, String value);
	}

	private String input;
	private String expected;

	protected void runTest() throws Throwable {
		final String output = format(input);
		assertEquals(expected, output);
	}

	private IScriptedTestContext context = null;
	private Map preferences = null;

	protected Map getDefaultPreferences() {
		return preferences;
	}

	protected IScriptFormatter createFormatter(Map preferences) {
		return context.createFormatter(preferences);
	}

	protected static char[] readResource(IScriptedTestContext context,
			String resourceName) throws IOException {
		final URL resource = context.getResourceBundle().getResource(
				resourceName);
		assertNotNull(resourceName + " is not found", resource); //$NON-NLS-1$
		return Util.getInputStreamAsCharArray(resource.openStream(), -1,
				context.getCharset());
	}

	private static final String TEST_MARKER = "====";
	private static final String RESPONSE_MARKER = "==";
	private static final String OPTION_MARKER = "==>";

	public TestSuite createScriptedSuite(IScriptedTestContext context,
			String resourceName) {
		return createScriptedSuite(context, getClass().getName(), resourceName,
				0);
	}

	/**
	 * @param resourceName
	 * @return
	 */
	public TestSuite createScriptedSuite(IScriptedTestContext context,
			String suiteName, String resourceName, int beginTestIndex) {
		final TestSuite suite = new TestSuite(suiteName);
		try {
			Map preferences = null;
			final String content = new String(readResource(context,
					resourceName));
			final String[] lines = TextUtils.splitLines(content);
			String testName = "START";
			int testIndex = 0;
			int testBegin = 0;
			int responseBegin = -1;
			int i = 0;
			while (i < lines.length) {
				final String line = lines[i++];
				if (line.startsWith(TEST_MARKER)
						|| line.startsWith(OPTION_MARKER)) {
					final int testEnd = i - 1;
					if (testEnd > testBegin) {
						if (responseBegin < 0) {
							throw new IllegalArgumentException(
									"No response marker - next test started on line "
											+ testEnd);
						}
						if (testIndex >= beginTestIndex) {
							suite.addTest(createTest(context, preferences,
									testName, lines, testBegin, responseBegin,
									testEnd));
						}
						++testIndex;
					}
					testBegin = i;
					responseBegin = -1;
					if (line.startsWith(TEST_MARKER)) {
						testName = line.substring(TEST_MARKER.length()).trim();
					} else if (line.startsWith(OPTION_MARKER)) {
						final Matcher matcher = OPTION_PATTERN.matcher(line
								.substring(OPTION_MARKER.length()));
						if (matcher.matches()) {
							if (preferences == null) {
								preferences = new HashMap();
							}
							final String optionName = context
									.validateOptionName(matcher.group(1));
							if (optionName == null)
								throw new IllegalArgumentException(
										"Invalid option name: " + line);
							final String optionValue = context
									.validateOptionValue(optionName, matcher
											.group(2));
							if (optionValue == null)
								throw new IllegalArgumentException(
										"Invalid option value: " + line);
							preferences.put(optionName, optionValue);
						} else {
							// TODO log error
						}
					}
				} else if (line.startsWith(RESPONSE_MARKER)) {
					if (responseBegin >= 0) {
						throw new IllegalArgumentException(
								"Multiple response markers: line " + (i - 1)
										+ ", previous on line " + responseBegin);
					}
					responseBegin = i;
				}
			}
			if (lines.length > testBegin) {
				if (responseBegin < 0) {
					throw new IllegalArgumentException(
							"No response marker in last test");
				}
				if (testIndex >= beginTestIndex) {
					suite.addTest(createTest(context, preferences, testName,
							lines, testBegin, responseBegin, lines.length));
				}
			}
		} catch (final Throwable e) {
			suite.addTest(new TestCase(e.getClass().getName()) { //$NON-NLS-1$
						protected void runTest() throws Throwable {
							throw e;
						}
					});
		}
		return suite;
	}

	private static final Pattern OPTION_PATTERN = Pattern
			.compile("\\s*([\\w\\.]+)\\s*=\\s*(\\S+)\\s*");

	private ScriptedTest createTest(IScriptedTestContext context,
			Map preferences, String testName, String[] lines, int testBegin,
			int responseBegin, final int testEnd) throws Exception {
		final String input = joinLines(lines, testBegin, responseBegin - 1);
		final String expected = joinLines(lines, responseBegin, testEnd);
		ScriptedTest test = (ScriptedTest) getClass().newInstance();
		test.setName(testName);
		test.input = input;
		test.expected = expected;
		test.context = context;
		test.preferences = preferences != null ? new HashMap(preferences)
				: preferences;
		return test;
	}

}