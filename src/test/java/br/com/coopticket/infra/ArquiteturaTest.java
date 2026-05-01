package br.com.coopticket.infra;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "br.com.coopticket", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    // ── Posicionamento de classes por estereótipo ──────────────────────────

    @ArchTest
    static final ArchRule controllers_devem_residir_em_pacote_controller =
            classes()
                    .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().resideInAPackage("..controller..")
                    .as("@RestController deve residir em pacote ..controller..");

    @ArchTest
    static final ArchRule services_devem_residir_em_pacote_service =
            classes()
                    .that().areAnnotatedWith("org.springframework.stereotype.Service")
                    .should().resideInAPackage("..service..")
                    .as("@Service deve residir em pacote ..service..");

    @ArchTest
    static final ArchRule repositories_devem_residir_em_pacote_repository =
            classes()
                    .that().areAnnotatedWith("org.springframework.stereotype.Repository")
                    .or().implement("org.springframework.data.repository.Repository")
                    .should().resideInAPackage("..repository..")
                    .as("Repositórios devem residir em pacote ..repository..");

    @ArchTest
    static final ArchRule entidades_jpa_devem_residir_em_pacote_domain =
            classes()
                    .that().areAnnotatedWith("jakarta.persistence.Entity")
                    .should().resideInAPackage("..domain..")
                    .as("@Entity deve residir em pacote ..domain..");

    // ── Isolamento de camadas ──────────────────────────────────────────────

    @ArchTest
    static final ArchRule controllers_nao_acessam_repositories_diretamente =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .as("Controllers não devem depender de repositories diretamente — use o service do módulo");

    // ── Isolamento entre módulos (sem acesso cruzado a repositories) ───────

    @ArchTest
    static final ArchRule auth_nao_acessa_repository_de_usuario =
            noClasses()
                    .that().resideInAPackage("br.com.coopticket.auth..")
                    .should().dependOnClassesThat().resideInAPackage("br.com.coopticket.usuario.repository..")
                    .as("Módulo auth não deve acessar repository de usuario diretamente");

    @ArchTest
    static final ArchRule usuario_nao_acessa_repository_de_auth =
            noClasses()
                    .that().resideInAPackage("br.com.coopticket.usuario..")
                    .should().dependOnClassesThat().resideInAPackage("br.com.coopticket.auth.repository..")
                    .as("Módulo usuario não deve acessar repository de auth diretamente");

    // ── Ausência de ciclos entre módulos ───────────────────────────────────

    @ArchTest
    static final SliceRule modulos_livres_de_ciclos =
            slices()
                    .matching("br.com.coopticket.(*)..")
                    .namingSlices("módulo '$1'")
                    .should().beFreeOfCycles()
                    .as("Módulos não devem ter dependências cíclicas entre si");
}
