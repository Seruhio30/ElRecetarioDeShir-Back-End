package com.elrecetariodeshir.backend.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapRunnerTests {

    @Test
    void createsEnabledAdminWithHashedPasswordAndDoesNotDuplicate() throws Exception {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setEnabled(true);
        properties.setUsername(" Sergio ");
        properties.setPassword("secret-password");

        InMemoryAdminUserRepository repository =
                new InMemoryAdminUserRepository();

        PasswordEncoder passwordEncoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        AdminBootstrapRunner runner =
                new AdminBootstrapRunner(
                        properties,
                        repository,
                        passwordEncoder);

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(repository.saved).isNotNull();
        assertThat(repository.saved.getUsername()).isEqualTo("sergio");
        assertThat(repository.saved.isEnabled()).isTrue();
        assertThat(repository.saved.getPasswordHash())
                .isNotEqualTo("secret-password");
        assertThat(passwordEncoder.matches(
                "secret-password",
                repository.saved.getPasswordHash()))
                .isTrue();

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(repository.saveCount).isEqualTo(1);
    }

    private static final class InMemoryAdminUserRepository
            implements AdminUserRepository {

        private AdminUser saved;
        private int saveCount;

        @Override
        public boolean existsByUsernameIgnoreCase(String username) {
            return saved != null
                    && saved.getUsername().equalsIgnoreCase(username);
        }

        @Override
        public <S extends AdminUser> S save(S entity) {
            saved = entity;
            saveCount++;
            return entity;
        }

        @Override
        public java.util.Optional<AdminUser> findByUsernameIgnoreCase(
                String username) {

            if (saved != null
                    && saved.getUsername().equalsIgnoreCase(username)) {
                return java.util.Optional.of(saved);
            }

            return java.util.Optional.empty();
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> S saveAndFlush(S entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> java.util.List<S> saveAllAndFlush(
                Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch(Iterable<AdminUser> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AdminUser getOne(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AdminUser getById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AdminUser getReferenceById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> java.util.List<S> findAll(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> java.util.List<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> java.util.Optional<S> findOne(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> org.springframework.data.domain.Page<S> findAll(
                org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> long count(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> boolean exists(
                org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser, R> R findBy(
                org.springframework.data.domain.Example<S> example,
                java.util.function.Function<
                        org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>,
                        R> queryFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<AdminUser> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<AdminUser> findAllById(Iterable<Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(AdminUser entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll(Iterable<? extends AdminUser> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<AdminUser> findAll(
                org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.springframework.data.domain.Page<AdminUser> findAll(
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Optional<AdminUser> findById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends AdminUser> java.util.List<S> saveAll(
                Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }
    }
}
