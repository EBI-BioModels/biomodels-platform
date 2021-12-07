/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import net.biomodels.jummp.core.user.RegistrationException
import net.biomodels.jummp.core.user.RoleNotFoundException
import net.biomodels.jummp.core.user.UserCommand
import net.biomodels.jummp.core.user.UserInvalidException
import net.biomodels.jummp.core.user.UserManagementException
import net.biomodels.jummp.core.user.UserNotFoundException
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import org.springframework.security.authentication.BadCredentialsException
import net.biomodels.jummp.core.user.UserCodeInvalidException
import net.biomodels.jummp.core.user.UserCodeExpiredException

/**
 * @short Interface describing the UserService.
 *
 * This service is meant for any kind of user management, such as changing password
 * and administrative tasks like enabling/disabling users, etc.
 *
 * This interface is required to access the UserService from within Java classes.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
interface IUserService {
    /**
     * Changes the password of the currently logged in user.
     * This method resets the password expired field of the user.
     * The change password functionality only supports passwords stored in the database
     * and not users authenticated through LDAP.
     * @param oldPassword The old password for verification
     * @param newPassword The new password to be used
     * @throws BadCredentialsException if @p oldPassword is incorrect
     */
    void changePassword(String oldPassword, String newPassword) throws BadCredentialsException
    /**
     * Edit the non-security related parts of a user.
     *
     * This method might be used by an administrator or by the user itself to change the
     * parts of the user object which are not security related.
     * @param user The User with the updated fields
     * @throws net.biomodels.jummp.core.user.UserInvalidException If the modified user does not validate
     */
    User editUser(User user) throws UserInvalidException
    /**
     *
     * @return The current (security sanitized) user
     */
    User getCurrentUser()
    /**
     * Retrieves a User object for the given @p username.
     * The returned object is not sanitized and includes any security relevant data.
     * This method is only for admin purpose.
     * @param username The login identifier of the user to be retrieved
     * @return The user
     * @throws UserNotFoundException Thrown if there is no User for @p username
     */
    User getUser(Long id) throws UserNotFoundException
    /**
     * Retrieves a User object for the given @p username.
     * The returned object is sanitized to not include any security relevant data.
     * @param username The login identifier of the user to be retrieved
     * @return The (security sanitized) user
     * @throws UserNotFoundException Thrown if there is no User for @p username
     */
    User getUser(String username) throws UserNotFoundException
    /**
     * Indicates whether the current user is a curator.
     *
     * @return false if the user is not logged in or if they don't have ROLE_CURATOR. true otherwise
     */
    boolean isLoggedInUserACurator()

    /**
     * Checks that the current user is an admin or not.
     *
     * @return true if the current user has ROLE_ADMIN, false otherwise
     */
    boolean isLoggedInUserAAdmin()
    /**
     * Retrieves list of users.
     * This method is only for administrative purpose. It does not sanitize the
     * returned Users, that is it includes all (also security relevant) elements.
     * The method only exists in a paginated version
     * @param offset Offset in the list
     * @param count Number of Users to return, Maximum is 100
     * @return List of Users ordered by Id
     */
    List<User> getAllUsers(Integer offset, Integer count)
    /**
     * Enables/Disables the user identified by @p userId
     * @param userId The unique id of the user
     * @param enable if @c true the user is enabled, if @c false the user is disabled
     * @return @c true, if the enable state was changed, @c false if the user was already in @p enable state
     * @throws UserNotFoundException If the user specified by @p userId does not exist
     */
    Boolean enableUser(Long userId, Boolean enable) throws UserNotFoundException
    /**
     * (Un)Locks the account for user identified by @p userId
     * @param userId The unique id of the user
     * @param lock if @c true the account is locked, if @c false the account is unlocked
     * @return @c true, if the account locked state was changed, @c false if the user was already in @p lock state
     * @throws UserNotFoundException If the user specified by @p userId does not exist
     */
    Boolean lockAccount(Long userId, Boolean lock) throws UserNotFoundException
    /**
     * (Un)Expires the account for user identified by @p userId
     * @param userId The unique id of the user
     * @param expire if @c true the account is expired, if @c false the account is un-expired
     * @return @c true, if the account expired state was changed, @c false if the user was already in @p expire state
     * @throws UserNotFoundException If the user specified by @p userId does not exist
     */
    Boolean expireAccount(Long userId, Boolean expire) throws UserNotFoundException
    /**
     * (Un)Expires the password for user identified by @p userId
     * @param userId The unique id of the user
     * @param expire if @c true the password is expired, if @c false the password is un-expired
     * @return @c true, if the password expired state was changed, @c false if the password was already in @p expire state
     * @throws UserNotFoundException If the user specified by @p userId does not exist
     */
    Boolean expirePassword(Long userId, Boolean expire) throws UserNotFoundException
    /**
     * Registers a new user.
     * If a new user registers himself the account will be initially disabled. A verification mail is
     * sent to either the new user or to a specific administration mail address. After navigating to the verification
     * URL the account will be enabled.
     * If an administrator creates the account the account will be enabled, but the password is expired as
     * an administrator cannot set the password. The user will be able to create a password after verification.
     * In case LDAP is used as an authentication backend, it is not possible to save a password, that is an invalid
     * password ("*") is stored in the database.
     * @param user The new User to register
     * @return Id of new created user
     * @throws RegistrationException In case a user with same name already exists
     * @throws UserInvalidException In case the new user does not validate
     * @see validateRegistration
     */
    Long register(User user) throws RegistrationException, UserInvalidException
    /**
     * Validates the registration code of a new user.
     * This method validates the validation Code and enables the user identified by @p username.
     * The registration code is invalidated at the same time.
     * In case the validation is not correct an exception is thrown
     * @param username The name of the new user
     * @param code The validation code
     * @throws UserManagementException Thrown in case that the validation cannot be performed
     * @see register
     */
    void validateRegistration(String username, String code) throws UserManagementException
    /**
     * Validates the registration code of a new user registered by an administrator.
     * In opposite to @link validateAdministration the user's account is already enabled but
     * does not yet have a valid password and the password is set to expired.
     * This method allows the registered user to set himself a password (in case LDAP is not used).
     * The expiration of the password is removed.
     * @param username The name of the new user
     * @param code The validation code
     * @param password The new password, may be @c null in that case an invalid password is set
     * @throws UserManagementException Thrown in case that the validation cannot be performed
     */
    void validateAdminRegistration(String username, String code, String password) throws UserManagementException
    /**
     * Overloaded method for convenience for not setting a password.
     * @param username The name of the new user
     * @param code The validation code
     * @throws UserManagementException Thrown in case that the validation cannot be performed
     */
    void validateAdminRegistration(String username, String code) throws UserManagementException
    /**
     * Request a new password for user identified by @p username.
     * This method generates a unique token to reset the password and sends it to
     * the user's email address.
     * The password itself is unchanged and not reset.
     * @param username The login id of the user whose password should be reset.
     * @throws UserNotFoundException Thrown if there is no user with @p username
     */
    void requestPassword(String username) throws UserNotFoundException
    /**
     * Resets the password of user identified by @p username with @p password in case the @p code is valid.
     * In case the password was expired prior to the reset, the password will no longer be expired
     * @param code The Password Reset Code
     * @param username The Login Id of the User
     * @param password The new Password
     * @throws UserNotFoundException Thrown in case user is not found
     * @throws UserCodeInvalidException Thrown in case the code is not valid
     * @throws UserCodeExpiredException Thrown in case the code expired
     */
    void resetPassword(String code, String username, String password) throws UserNotFoundException, UserCodeInvalidException, UserCodeExpiredException
    /**
     * Retrieves all available roles in this Jummp Instance.
     * As this is an admin method it does not provide a paginated version
     * @return List of all Roles
     */
    List<Role> getAllRoles()
    /**
     * Retrieves the Roles for the User identified by @p id.
     *
     * In case there is no user with @p id an empty list is returned.
     * @param id The user id
     * @return List of Roles assigned to the user
     */
    List<Role> getRolesForUser(Long id)
    /**
     * Adds a Role to the user.
     * If the user already has the role, the user is not changed and no feedback for this situation is
     * provided. The method only ensures that the user has the role after execution.
     * @param userId The id of the user who should receive a new role
     * @param roleId The id or the role to be added to the user
     * @throws UserNotFoundException In case there is no user with @p userId
     * @throws RoleNotFoundException In case there is no role with @p roleId
     */
    void addRoleToUser(Long userId, Long roleId) throws UserNotFoundException, RoleNotFoundException
    /**
     * Removes a Role from the user.
     * If the user does not have the role, the user is not changed and no feedback for this situation is
     * provided. The method only ensures that the user does not have the role after execution.
     * @param userId The id of the user from whom the role should be removed
     * @param roleId The id or the role to be removed from the user
     * @throws UserNotFoundException In case there is no user with @p userId
     * @throws RoleNotFoundException In case there is no role with @p roleId
     */
    void removeRoleFromUser(Long userId, Long roleId) throws UserNotFoundException, RoleNotFoundException
    /**
     * Retrieves the Role identified by the @p authority.
     * @param authority The authority specifying the Role
     * @throws RoleNotFoundException If there is no such Role
     */
    Role getRoleByAuthority(String authority) throws RoleNotFoundException
/**
 * Instantiates a new user object for the initial user (admin).
 * The values are handed over by the setup controller in form of strings.
 * Some values are hard coded, these are always the same.
 * Triggers the persistAdminWithRoles() method.
 * @param username The name of the user submitted
 * @param passwd The password of the new user
 * @param userRealName The real name
 * @param email The e-mail address handed over
 * @return boolean Indicating whether everything worked well
 */
    boolean createAdmin(UserCommand user)
/**
 * Persists the newly instantiated admin user.
 * Triggers the createRolesForAdmin() method.
 * @param person The instantiated user
 * @return boolean True if user could be persisted, false otherwise
 */
    boolean persistAdminWithRoles(User person)
/**
 * Creates the roles to the new user and triggers the addRoleToUser() method twice
 * which.adds a role to a given user.
 * @param user The created and persisted user
 * @return boolean Indicating whether assigning could be applied
 */
    boolean createRolesForAdmin(User user)
}
