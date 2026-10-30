package com.training.ecommerce.visualizer.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Ranh giới giữa lõi visualizer ({@code core}) và adapter ecommerce: core không biết gì về domain cụ thể.
 */
@AnalyzeClasses(packages = "com.training.ecommerce.visualizer", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule coreKhongPhuThuocDomain = noClasses()
            .that().resideInAPackage("com.training.ecommerce.visualizer.core..")
            .should().dependOnClassesThat().resideInAPackage("com.training.ecommerce.domain..")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule coreKhongPhuThuocShared = noClasses()
            .that().resideInAPackage("com.training.ecommerce.visualizer.core..")
            .should().dependOnClassesThat().resideInAPackage("com.training.ecommerce.shared..")
            .allowEmptyShould(false);

    @ArchTest
    static final ArchRule coreKhongPhuThuocAdapterEcommerce = noClasses()
            .that().resideInAPackage("com.training.ecommerce.visualizer.core..")
            .should().dependOnClassesThat().resideInAPackage("com.training.ecommerce.visualizer.ecommerce..")
            .allowEmptyShould(false);
}
