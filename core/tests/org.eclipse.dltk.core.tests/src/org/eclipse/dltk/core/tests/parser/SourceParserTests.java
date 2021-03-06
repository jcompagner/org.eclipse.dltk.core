package org.eclipse.dltk.core.tests.parser;

import org.eclipse.dltk.ast.parser.ISourceParser;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKContributedExtension;
import org.eclipse.dltk.core.tests.model.AbstractModelTests;
import org.eclipse.dltk.core.tests.model.TestConstants;
import org.eclipse.dltk.core.tests.model.TestSourceParser;

public class SourceParserTests extends AbstractModelTests {

	private static final String PARSER_NAME = "Test Source Parser";
	private static final String PARSER_ID = "org.eclipse.dltk.core.tests.sourceParser";
	private static final int PARSER_PRIORITY = 1;

	public SourceParserTests(String name) {
		super("org.eclipse.dltk.core.tests", name);
	}

	public void testGetSourceParser() {
		ISourceParser parser = null;

		parser = DLTKLanguageManager.getSourceParser(TestConstants.NATURE_ID);

		assertNotNull(parser);
		assertTrue((parser instanceof TestSourceParser));

		/*
		 * these tests are 'dependent' upon the two test tests above working -
		 * this could be broken out into its own top level test
		 */
		testParserConfig(parser);
	}

	private void testParserConfig(ISourceParser parser) {
		final IDLTKContributedExtension extension = (IDLTKContributedExtension) parser;
		// these are configured to the same value in plugin.xml
		assertEquals(PARSER_NAME, extension.getName());
		assertEquals(PARSER_NAME, extension.getDescription());

		assertEquals(PARSER_ID, extension.getId());
		assertEquals(TestConstants.NATURE_ID, extension.getNatureId());

		assertEquals(PARSER_PRIORITY, extension.getPriority());
	}

}
