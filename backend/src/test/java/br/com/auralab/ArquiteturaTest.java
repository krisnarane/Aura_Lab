package br.com.auralab;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArquiteturaTest {
  @Test
  void controllersEMapperNaoAcessamRepositories() {
    var classes = new ClassFileImporter().importPackages("br.com.auralab");
    noClasses()
        .that()
        .resideInAPackage("..api..")
        .or()
        .haveSimpleNameEndingWith("Mapper")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..repository..")
        .check(classes);
  }
}
