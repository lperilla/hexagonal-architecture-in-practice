package com.example.petstore.adoption;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de arquitectura que Maven no puede ver: Maven impide que un módulo use otro que no declara,
 * pero no controla los paquetes ni el código generado.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.petstore.adoption");

    /**
     * Lombok solo actúa al compilar; la única huella que deja en el bytecode es la anotación
     * {@code @lombok.Generated}, que no se necesita en ejecución. El dominio ni siquiera lo usa.
     */
    private static final String LOMBOK = "lombok..";

    @Test
    void theDomainDependsOnNothingButJava() {
        classes().that().resideInAPackage("..adoption.domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("..adoption.domain..", "java..")
                .check(CLASSES);
    }

    @Test
    void eachAdapterImplementsASinglePort() {
        classes().that().resideInAPackage("..adoption.infrastructure..")
                .should(implementAtMostOnePort())
                .check(CLASSES);
    }

    /**
     * Un adaptador, un puerto: cada clase hace una sola cosa y se puede sustituir por separado.
     */
    private static ArchCondition<JavaClass> implementAtMostOnePort() {
        return new ArchCondition<>("implementar como mucho un puerto") {
            @Override
            public void check(JavaClass adapter, ConditionEvents events) {
                long ports = adapter.getRawInterfaces().stream()
                        .filter(port -> port.getPackageName().endsWith(".domain.port"))
                        .count();
                if (ports > 1) {
                    events.add(SimpleConditionEvent.violated(adapter,
                            adapter.getName() + " implementa " + ports + " puertos"));
                }
            }
        };
    }

    @Test
    void theApplicationDependsOnlyOnTheDomainAndJava() {
        classes().that().resideInAPackage("..adoption.application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..adoption.application..", "..adoption.domain..", "java..", LOMBOK)
                .check(CLASSES);
    }

    @Test
    void outputPortsUseNoTechnicalTypes() {
        noClasses().that().resideInAPackage("..adoption.domain.port..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adoption.infrastructure..", "..adoption.api..", "..adoption.soap..", "org.springframework..")
                .check(CLASSES);
    }

    @Test
    void theGeneratedRestContractIsOnlyUsedByTheRestAdapter() {
        noClasses().that().resideOutsideOfPackages("..adoption.api.contract..", "..adoption.api.rest..")
                .should().dependOnClassesThat().resideInAPackage("..adoption.api.contract..")
                .check(CLASSES);
    }

    @Test
    void theGeneratedSoapContractIsOnlyUsedByTheSoapAdapter() {
        noClasses().that().resideOutsideOfPackages("..adoption.soap.contract..", "..adoption.api.soap..")
                .should().dependOnClassesThat().resideInAPackage("..adoption.soap.contract..")
                .check(CLASSES);
    }

    @Test
    void theRestAdapterDoesNotKnowOtherAdapters() {
        noClasses().that().resideInAPackage("..adoption.api.rest..")
                .should().dependOnClassesThat().resideInAnyPackage("..adoption.infrastructure..", "..adoption.api.soap..")
                .check(CLASSES);
    }

    @Test
    void theSoapAdapterDoesNotKnowOtherAdapters() {
        noClasses().that().resideInAPackage("..adoption.api.soap..")
                .should().dependOnClassesThat().resideInAnyPackage("..adoption.infrastructure..", "..adoption.api.rest..")
                .check(CLASSES);
    }

    @Test
    void theInfrastructureDoesNotKnowTheApis() {
        noClasses().that().resideInAPackage("..adoption.infrastructure..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adoption.api.rest..", "..adoption.api.soap..",
                        "..adoption.api.contract..", "..adoption.soap.contract..")
                .check(CLASSES);
    }
}
