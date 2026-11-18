package service;

import model.User;
import model.UserRole;
import model.UserStatus;
import repository.UserRepository;
import util.DatabaseException;
import util.PasswordUtil;
import util.ValidationException;
import util.ValidationUtil;

import java.util.Optional;

/**
 * Handles secure authentication, self-registration (USER role enforced),
 * protected first-time Administrator setup, and PBKDF2 password updates.
 */
public class AuthenticationService {

    private final UserRepository userRepository;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasAnyAdmin() throws DatabaseException {
        return userRepository.countByRole(UserRole.ADMIN) > 0;
    }

    /**
     * Protected first-time Administrator creation mechanism.
     * Permanently locks once at least one ADMIN user exists in SQLite.
     */
    public synchronized User setupInitialAdmin(String fullName, String username, String email,
                                                String password, String confirmPassword)
            throws ValidationException, DatabaseException {
        if (hasAnyAdmin()) {
            throw new ValidationException(
                    "Initial administrator setup is locked because an administrator account already exists."
            );
        }

        String validName = ValidationUtil.validateFullName(fullName);
        String validUser = ValidationUtil.validateUsername(username);
        String validEmail = ValidationUtil.validateEmail(email);
        ValidationUtil.validatePasswordWithConfirmation(password, confirmPassword);

        if (userRepository.findByUsernameIgnoreCase(validUser).isPresent()) {
            throw new ValidationException("Username \"" + validUser + "\" is already taken.");
        }
        if (userRepository.findByEmailIgnoreCase(validEmail).isPresent()) {
            throw new ValidationException("Email \"" + validEmail + "\" is already registered.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        User admin = new User(
                validName,
                validUser,
                validEmail,
                hash,
                salt,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        return userRepository.save(admin);
    }

    /**
     * Public self-registration. Always assigns the USER role and ACTIVE status by default.
     */
    public User registerUser(String fullName, String username, String email,
                             String password, String confirmPassword)
            throws ValidationException, DatabaseException {
        String validName = ValidationUtil.validateFullName(fullName);
        String validUser = ValidationUtil.validateUsername(username);
        String validEmail = ValidationUtil.validateEmail(email);
        ValidationUtil.validatePasswordWithConfirmation(password, confirmPassword);

        if (userRepository.findByUsernameIgnoreCase(validUser).isPresent()) {
            throw new ValidationException("Username \"" + validUser + "\" is already registered. Please choose another.");
        }
        if (userRepository.findByEmailIgnoreCase(validEmail).isPresent()) {
            throw new ValidationException("Email \"" + validEmail + "\" is already associated with an account.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        // Enforce default USER role on public registration
        User newUser = new User(
                validName,
                validUser,
                validEmail,
                hash,
                salt,
                UserRole.USER,
                UserStatus.ACTIVE
        );
        return userRepository.save(newUser);
    }

    public User authenticate(String usernameOrEmail, String password)
            throws ValidationException, DatabaseException {
        String identifier = usernameOrEmail != null ? usernameOrEmail.trim() : "";
        if (identifier.isEmpty()) {
            throw new ValidationException("Username is required.");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required.");
        }

        Optional<User> found = userRepository.findByUsernameIgnoreCase(identifier);
        if (found.isEmpty() && identifier.contains("@")) {
            found = userRepository.findByEmailIgnoreCase(identifier);
        }
        if (found.isEmpty()) {
            throw new ValidationException("Invalid username or password.");
        }

        User user = found.get();
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash(), user.getPasswordSalt())) {
            throw new ValidationException("Invalid username or password.");
        }

        if (!user.isActive()) {
            throw new ValidationException("This account is currently INACTIVE. Please contact an Administrator.");
        }

        return user;
    }

    public User changePassword(User actor, String currentPassword, String newPassword, String confirmNewPassword)
            throws ValidationException, DatabaseException {
        if (actor == null) {
            throw new ValidationException("Authenticated session is required.");
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new ValidationException("Current password is required.");
        }
        User fresh = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ValidationException("User account no longer exists."));

        if (!PasswordUtil.verifyPassword(currentPassword, fresh.getPasswordHash(), fresh.getPasswordSalt())) {
            throw new ValidationException("Current password is incorrect.");
        }

        ValidationUtil.validatePasswordWithConfirmation(newPassword, confirmNewPassword);
        if (newPassword.equals(currentPassword)) {
            throw new ValidationException("New password must be different from your current password.");
        }

        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newPassword, newSalt);
        userRepository.updatePassword(fresh.getId(), newHash, newSalt);
        fresh.setPasswordHash(newHash);
        fresh.setPasswordSalt(newSalt);
        return fresh;
    }
}
