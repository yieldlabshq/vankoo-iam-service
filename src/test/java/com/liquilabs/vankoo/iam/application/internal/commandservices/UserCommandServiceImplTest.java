package com.liquilabs.vankoo.iam.application.internal.commandservices;

import com.liquilabs.vankoo.iam.application.internal.outboundservices.hashing.HashingService;
import com.liquilabs.vankoo.iam.application.internal.outboundservices.tokens.SecretTokenService;
import com.liquilabs.vankoo.iam.application.internal.outboundservices.tokens.TokenService;
import com.liquilabs.vankoo.iam.domain.exceptions.EmailAlreadyInUseException;
import com.liquilabs.vankoo.iam.domain.exceptions.InvalidCredentialsException;
import com.liquilabs.vankoo.iam.domain.exceptions.InvalidPasswordResetTokenException;
import com.liquilabs.vankoo.iam.domain.model.aggregates.PasswordResetToken;
import com.liquilabs.vankoo.iam.domain.model.aggregates.User;
import com.liquilabs.vankoo.iam.domain.model.commands.RequestPasswordResetCommand;
import com.liquilabs.vankoo.iam.domain.model.commands.ResetPasswordCommand;
import com.liquilabs.vankoo.iam.domain.model.commands.SignInCommand;
import com.liquilabs.vankoo.iam.domain.model.commands.SignUpCommand;
import com.liquilabs.vankoo.iam.domain.model.entities.Role;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Email;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.Password;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.TokenDigest;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.PasswordResetTokenRepository;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test, no Spring context: {@code handle(...)} is called directly and
 * the repositories, hashing and token services are mocked.
 */
@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    private static final int RESET_EXPIRATION_MINUTES = 30;
    private static final Email EMAIL = new Email("carlos@vankoo.pe");
    private static final Password RAW_PASSWORD = new Password("S3cret!pass");
    private static final String HASHED = "$2a$10$hashed";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private HashingService hashingService;

    @Mock
    private TokenService tokenService;

    @Mock
    private SecretTokenService secretTokenService;

    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserCommandServiceImpl(
                userRepository,
                roleRepository,
                passwordResetTokenRepository,
                hashingService,
                tokenService,
                secretTokenService,
                RESET_EXPIRATION_MINUTES);
    }

    @Test
    void signUp_storesTheHashedPasswordWithTheRequestedRole() {
        var mypeRole = new Role(RoleName.ROLE_MYPE);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(roleRepository.findAllByNameIn(List.of(RoleName.ROLE_MYPE))).thenReturn(List.of(mypeRole));
        when(hashingService.encode(RAW_PASSWORD.password())).thenReturn(HASHED);

        var user = service.handle(new SignUpCommand(EMAIL, RAW_PASSWORD, List.of(mypeRole))).orElseThrow();

        assertEquals(EMAIL, user.getEmail());
        assertEquals(HASHED, user.getPassword().password());
        assertTrue(user.getRoles().contains(mypeRole));
        verify(userRepository).save(user);
    }

    @Test
    void signUp_withoutRolesFallsBackToTheUserRole() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(roleRepository.findAllByNameIn(List.of(RoleName.ROLE_USER)))
                .thenReturn(List.of(new Role(RoleName.ROLE_USER)));
        when(hashingService.encode(any())).thenReturn(HASHED);

        var user = service.handle(new SignUpCommand(EMAIL, RAW_PASSWORD, List.of())).orElseThrow();

        assertEquals(List.of(RoleName.ROLE_USER), user.getRoles().stream().map(Role::getName).toList());
    }

    @Test
    void signUp_rejectsAnEmailAlreadyInUse() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class,
                () -> service.handle(new SignUpCommand(EMAIL, RAW_PASSWORD, List.of())));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_failsWhenTheRolesTableWasNeverSeeded() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(roleRepository.findAllByNameIn(any())).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> service.handle(new SignUpCommand(EMAIL, RAW_PASSWORD, List.of(new Role(RoleName.ROLE_MYPE)))));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signIn_returnsTheUserAndATokenWithItsRoles() {
        var user = new User(EMAIL, new Password(HASHED), List.of(new Role(RoleName.ROLE_INVESTOR)));
        when(userRepository.findByEmailWithRoles(EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches(RAW_PASSWORD.password(), HASHED)).thenReturn(true);
        when(tokenService.generateToken(user.getId().id().toString(), EMAIL.email(), List.of("ROLE_INVESTOR")))
                .thenReturn("jwt-token");

        var result = service.handle(new SignInCommand(EMAIL, RAW_PASSWORD)).orElseThrow();

        assertEquals(user, result.getLeft());
        assertEquals("jwt-token", result.getRight());
    }

    @Test
    void signIn_rejectsAnUnknownEmail() {
        when(userRepository.findByEmailWithRoles(EMAIL)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.handle(new SignInCommand(EMAIL, RAW_PASSWORD)));
    }

    @Test
    void signIn_rejectsAWrongPasswordWithTheSameException() {
        var user = new User(EMAIL, new Password(HASHED));
        when(userRepository.findByEmailWithRoles(EMAIL)).thenReturn(Optional.of(user));
        when(hashingService.matches(RAW_PASSWORD.password(), HASHED)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> service.handle(new SignInCommand(EMAIL, RAW_PASSWORD)));
        verify(tokenService, never()).generateToken(any(), any(), any());
    }

    @Test
    void requestPasswordReset_replacesPreviousLinksAndStoresOnlyTheDigest() {
        var user = new User(EMAIL, new Password(HASHED));
        when(secretTokenService.generateSecret()).thenReturn("plain-secret");
        when(secretTokenService.digestOf("plain-secret")).thenReturn("digest-of-secret");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        var before = new Date();

        service.handle(new RequestPasswordResetCommand(EMAIL));

        verify(passwordResetTokenRepository).deleteAllByUserId(user.getId());
        var saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(saved.capture());
        assertEquals(new TokenDigest("digest-of-secret"), saved.getValue().getTokenDigest());
        assertEquals(user.getId(), saved.getValue().getUserId());
        assertTrue(saved.getValue().getExpiresAt().after(before));
    }

    @Test
    void requestPasswordReset_forAnUnknownEmailDoesNothingVisible() {
        when(secretTokenService.generateSecret()).thenReturn("plain-secret");
        when(secretTokenService.digestOf("plain-secret")).thenReturn("digest-of-secret");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.handle(new RequestPasswordResetCommand(EMAIL));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).deleteAllByUserId(any());
    }

    @Test
    void resetPassword_changesThePasswordAndConsumesTheLink() {
        var user = new User(EMAIL, new Password(HASHED));
        var resetToken = new PasswordResetToken(
                user.getId(), new TokenDigest("digest"), new Date(System.currentTimeMillis() + 60_000));
        when(secretTokenService.digestOf("plain-secret")).thenReturn("digest");
        when(passwordResetTokenRepository.findByTokenDigest(new TokenDigest("digest"))).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(hashingService.encode("N3w!pass")).thenReturn("$2a$10$new");

        service.handle(new ResetPasswordCommand("plain-secret", new Password("N3w!pass")));

        assertEquals("$2a$10$new", user.getPassword().password());
        assertNotNull(resetToken.getConsumedAt());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    void resetPassword_rejectsAnUnknownLink() {
        when(secretTokenService.digestOf("plain-secret")).thenReturn("digest");
        when(passwordResetTokenRepository.findByTokenDigest(new TokenDigest("digest"))).thenReturn(Optional.empty());

        assertThrows(InvalidPasswordResetTokenException.class,
                () -> service.handle(new ResetPasswordCommand("plain-secret", new Password("N3w!pass"))));
    }

    @Test
    void resetPassword_rejectsAnExpiredLinkWithTheSameException() {
        var expired = new PasswordResetToken(
                new User(EMAIL, new Password(HASHED)).getId(),
                new TokenDigest("digest"),
                new Date(System.currentTimeMillis() - 60_000));
        when(secretTokenService.digestOf("plain-secret")).thenReturn("digest");
        when(passwordResetTokenRepository.findByTokenDigest(new TokenDigest("digest"))).thenReturn(Optional.of(expired));

        assertThrows(InvalidPasswordResetTokenException.class,
                () -> service.handle(new ResetPasswordCommand("plain-secret", new Password("N3w!pass"))));
        assertFalse(expired.isUsable(new Date()));
        verify(userRepository, never()).save(any());
    }
}
