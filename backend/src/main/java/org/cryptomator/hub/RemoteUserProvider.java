package org.cryptomator.hub;

import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import java.util.List;

public interface RemoteUserProvider {

	/**
	 * Gets a single user with the given id
	 * @param id the user id
	 * @return the user or null, if a user with the given id does not exists
	 */
	User user(String id);

	/**
	 * Get all remote users
	 * @return List of users
	 */
	List<User> users();

	/**
	 * Search for remote users excluding groups
	 * @param querry A String contained in username, first or last name, or email
	 * @return List of users without groups
	 */
	List<User> searchUser(String querry);

	/**
	 * Get all remote groups
	 * @return List of groups
	 */
	List<Group> groups();

	/**
	 * Search for remote group excluding members
	 * @param groupname A String contained in groupname
	 * @return List of groups without members
	 */
	List<Group> searchGroup(String groupname);

}
