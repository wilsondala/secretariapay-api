package com.secretariapay.api.config;

import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.enums.UserRole;
import com.secretariapay.api.entity.enums.UserStatus;
import com.secretariapay.api.repository.UserRepository;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TestUsersBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestUsersBootstrap.class);

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${secretariapay.test-users.enabled:false}")
    private boolean enabled;

    @Value("${secretariapay.test-users.password:Admin@123456}")
    private String defaultPassword;

    public TestUsersBootstrap(UserRepository userRepository, InstitutionRepository institutionRepository) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        Institution institution = institutionRepository.findAll().stream().findFirst().orElse(null);

        List<TestUserDefinition> users = List.of(
                new TestUserDefinition("Administrador Global", "admin@secretariapay.com", UserRole.ADMIN_GLOBAL),
                new TestUserDefinition("Administrador IMETRO", "admin.imetro@secretariapay.com", UserRole.ADMIN_IMETRO),
                new TestUserDefinition("Direção", "direcao@secretariapay.com", UserRole.DIRECAO),
                new TestUserDefinition("Financeiro", "financeiro@secretariapay.com", UserRole.FINANCEIRO),
                new TestUserDefinition("Tesouraria", "tesouraria@secretariapay.com", UserRole.TESOURARIA),
                new TestUserDefinition("Secretaria", "secretaria@secretariapay.com", UserRole.SECRETARIA),
                new TestUserDefinition("Operador de Atendimento", "atendimento@secretariapay.com", UserRole.OPERADOR_ATENDIMENTO),
                new TestUserDefinition("Coordenação DCR", "dcr.coordenacao@secretariapay.com", UserRole.DCR_COORDENACAO),
                new TestUserDefinition("Operador DCR", "dcr.operador@secretariapay.com", UserRole.DCR_OPERADOR),
                new TestUserDefinition("Tecnologia da Informação", "tic@secretariapay.com", UserRole.TIC),
                new TestUserDefinition("Auditoria", "auditoria@secretariapay.com", UserRole.AUDITORIA)
        );

        String encodedPassword = passwordEncoder.encode(defaultPassword);

        for (TestUserDefinition definition : users) {
            User user = userRepository.findByEmailIgnoreCase(definition.email()).orElseGet(User::new);
            user.setFullName(definition.fullName())
                    .setEmail(definition.email().toLowerCase())
                    .setPasswordHash(encodedPassword)
                    .setRole(definition.role())
                    .setStatus(UserStatus.ACTIVE)
                    .setInstitution(institution);
            userRepository.save(user);
        }

        log.warn("Usuários de teste SecretáriaPay criados/atualizados. Desative SECRETARIAPAY_TEST_USERS_ENABLED após validar os acessos.");
    }

    private record TestUserDefinition(String fullName, String email, UserRole role) {
    }
}
