package com.example.petstore.notification;

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
 * Reglas de arquitectura que Maven no puede ver.
 */
class ArchitectureTest {

    /**
     * Lombok solo actúa al compilar; la única huella que deja en el bytecode es la anotación
     * {@code @lombok.Generated}, que no se necesita en ejecución.
     */
    private static final String LOMBOK = "lombok..";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.example.petstore.notification");

    @Test
    void theDomainDependsOnNothingButJava() {
        classes().that().resideInAPackage("..notification.domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("..notification.domain..", "java..", LOMBOK)
                .check(CLASSES);
    }

    @Test
    void theApplicationDependsOnlyOnTheDomainAndJava() {
        classes().that().resideInAPackage("..notification.application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..notification.application..", "..notification.domain..", "java..", LOMBOK)
                .check(CLASSES);
    }

    @Test
    void outputPortsUseNoTechnicalTypes() {
        noClasses().that().resideInAPackage("..notification.domain.port..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..notification.infrastructure..", "org.springframework..", "jakarta.mail..")
                .check(CLASSES);
    }

    @Test
    void theRabbitMqMessageDoesNotLeaveTheListener() {
        noClasses().that().resideOutsideOfPackage("..infrastructure.rabbitmq.listener..")
                .should().dependOnClassesThat().haveSimpleName("PetAdoptedMessage")
                .check(CLASSES);
    }

    @Test
    void theOutputAdaptersDoNotKnowEachOther() {
        noClasses().that().resideInAPackage("..infrastructure.mail..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure.logging..", "..infrastructure.rabbitmq..", "..infrastructure.inmemory..")
                .check(CLASSES);
    }

    @Test
    void eachAdapterImplementsASinglePort() {
        classes().that().resideInAPackage("..notification.infrastructure..")
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
}
