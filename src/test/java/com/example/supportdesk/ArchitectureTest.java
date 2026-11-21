package com.example.supportdesk;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Turns the conventions in {@code CLAUDE.md} into build failures, so they hold for every
 * change, whoever (or whatever) wrote it.
 */
@AnalyzeClasses(packages = "com.example.supportdesk", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	/**
	 * Classes are package-private unless another class needs them. Everything lives in one
	 * package, so the only public types are the Boot entry point and the triage API whose
	 * {@code Triage} result the controller and demo runner hand back. A new public class
	 * means updating this list on purpose, not by accident.
	 */
	@ArchTest
	static final ArchRule classesArePackagePrivateByDefault = classes()
		.that().areTopLevelClasses()
		.and().doNotHaveSimpleName("SupportDeskApplication")
		.and().doNotHaveSimpleName("TriageService")
		.should().notBePublic()
		.because("CLAUDE.md: classes are package-private unless another class needs them");

	@ArchTest
	static final ArchRule logsThroughCommonsLogging = noClasses()
		.should().dependOnClassesThat().resideInAnyPackage("org.slf4j..", "org.apache.logging.log4j..", "java.util.logging..")
		.because("CLAUDE.md: use org.apache.commons.logging.Log / LogFactory, as the existing code does");

	/**
	 * The triage stage decides; the draft stage writes. Triage must not reach for the chat
	 * model, or the confidence gate stops being the only thing that routes a ticket.
	 */
	@ArchTest
	static final ArchRule triageDoesNotDependOnTheChatModel = noClasses()
		.that().haveSimpleNameStartingWith("Triage")
		.should().dependOnClassesThat().resideInAnyPackage("org.springframework.ai.chat..", "org.springframework.ai.anthropic..")
		.because("triage is Jev plus plain code; only the draft stage calls the chat model");

	/** Field injection hides dependencies from the unit tests that mock them. */
	@ArchTest
	static final ArchRule noFieldInjection = noClasses()
		.should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.beans.factory.annotation.Autowired")
		.because("constructor injection keeps every collaborator visible to the mocks in the unit tests");

}
