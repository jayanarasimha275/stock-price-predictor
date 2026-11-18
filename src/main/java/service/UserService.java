package service;

import model.User;
import model.UserRole;
import model.UserStatus;
import repository.UserRepository;
import util.DatabaseException;
import util.PasswordUtil;
import util.ValidationException;
import util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Role-protected service for Administrator user management, search, activation, role assignment,
 * and dashboard analytics.
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void requireAdmin(User actor) throws ValidationException {
        if (actor == null || !actor.isAdmin() || !actor.isActive()) {
            throw new ValidationException("Access denied: Active Administrator privileges are required.");
        }
    }

    public List<User> getAllUsers(User actor) throws ValidationException, DatabaseException {
        requireAdmin(actor);
        return userRepository.findAll();
    }

    public List<User> searchUsers(User actor, String query) throws ValidationException, DatabaseException {
        requireAdmin(actor);
        return userRepository.search(query);
    }

    public List<User> getRecentRegistrations(User actor, int limit) throws ValidationException, DatabaseException {
        requireAdmin(actor);
        return userRepository.findRecent(limit);
    }

    public int getTotalUserCount(User actor) throws ValidationException, DatabaseException {
        requireAdmin(actor);
        return userRepository.countAll();
    }

    public User createUser(User actor, String fullName, String username, String email,
                           String password, String confirmPassword,
                           UserRole role, UserStatus status)
            throws ValidationException, DatabaseException {
        requireAdmin(actor);

        String validName = ValidationUtil.validateFullName(fullName);
        String validUser = ValidationUtil.validateUsername(username);
        String validEmail = ValidationUtil.validateEmail(email);
        ValidationUtil.validatePasswordWithConfirmation(password, confirmPassword);

        if (userRepository.findByUsernameIgnoreCase(validUser).isPresent()) {
            throw new ValidationException("Username \"" + validUser + "\" already exists.");
        }
        if (userRepository.findByEmailIgnoreCase(validEmail).isPresent()) {
            throw new ValidationException("Email \"" + validEmail + "\" is already registered.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        User user = new User(
                validName,
                validUser,
                validEmail,
                hash,
                salt,
                role != null ? role : UserRole.USER,
                status != null ? status : UserStatus.ACTIVE
        );
        return userRepository.save(user);
    }

    public User updateUser(User actor, int targetUserId, String fullName, String username, String email,
                           UserRole newRole, UserStatus newStatus)
            throws ValidationException, DatabaseException {
        requireAdmin(actor);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("Selected user no longer exists."));

        String validName = ValidationUtil.validateFullName(fullName);
        String validUser = ValidationUtil.validateUsername(username);
        String validEmail = ValidationUtil.validateEmail(email);

        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(validUser);
        if (byUsername.isPresent() && byUsername.get().getId() != targetUserId) {
            throw new ValidationException("Username \"" + validUser + "\" is already used by another account.");
        }
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(validEmail);
        if (byEmail.isPresent() && byEmail.get().getId() != targetUserId) {
            throw new ValidationException("Email \"" + validEmail + "\" is already used by another account.");
        }

        UserRole resolvedRole = newRole != null ? newRole : target.getRole();
        UserStatus resolvedStatus = newStatus != null ? newStatus : target.getStatus();

        guardAgainstAdminLockout(actor, target, resolvedRole, resolvedStatus);

        target.setFullName(validName);
        target.setUsername(validUser);
        target.setEmail(validEmail);
        target.setRole(resolvedRole);
        target.setStatus(resolvedStatus);
        userRepository.updateProfile(target);
        return target;
    }

    public void toggleStatus(User actor, int targetUserId, UserStatus nextStatus)
            throws ValidationException, DatabaseException {
        requireAdmin(actor);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("Selected user not found."));
        guardAgainstAdminLockout(actor, target, target.getRole(), nextStatus);
        userRepository.updateStatus(targetUserId, nextStatus);
    }

    public void changeRole(User actor, int targetUserId, UserRole nextRole)
            throws ValidationException, DatabaseException {
        requireAdmin(actor);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("Selected user not found."));
        guardAgainstAdminLockout(actor, target, nextRole, target.getStatus());
        target.setRole(nextRole);
        userRepository.updateProfile(target);
    }

    public void deleteUser(User actor, int targetUserId) throws ValidationException, DatabaseException {
        requireAdmin(actor);
        if (actor.getId() == targetUserId) {
            throw new ValidationException("You cannot delete your own currently logged-in administrator account.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ValidationException("Selected user not found."));
        if (target.isAdmin() && target.isActive() && userRepository.countActiveAdmins() <= 1) {
            throw new ValidationException("Cannot delete the only active administrator account.");
        }
        userRepository.deleteById(targetUserId);
    }

    private void guardAgainstAdminLockout(User actor, User target, UserRole nextRole, UserStatus nextStatus)
            throws ValidationException, DatabaseException {
        if (actor.getId() == target.getId()) {
            if (nextRole != UserRole.ADMIN) {
                throw new ValidationException("You cannot remove your own ADMIN privileges while logged in.");
            }
            if (nextStatus != UserStatus.ACTIVE) {
                throw new ValidationException("You cannot deactivate your own currently logged-in ADMIN account.");
            }
        }
        boolean wasActiveAdmin = target.isAdmin() && target.isActive();
        boolean willRemainActiveAdmin = nextRole == UserRole.ADMIN && nextStatus == UserStatus.ACTIVE;
        if (wasActiveAdmin && !willRemainActiveAdmin && userRepository.countActiveAdmins() <= 1) {
            throw new ValidationException("Cannot demote or deactivate the last active Administrator.");
        }
    }
}
