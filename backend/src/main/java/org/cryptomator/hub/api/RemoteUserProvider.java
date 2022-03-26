package org.cryptomator.hub.api;

import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import java.util.stream.Stream;

public interface RemoteUserProvider {

	/**
	 * Get all remote users including groups
	 * @return Stream of users
	 */
	Stream<User> users();

	/**
	 * Get all remote users including groups
	 * @return Stream of users
	 */
	Stream<User> usersIncludingGroups();

	/**
	 * Search for remote users excluding groups
	 * @param querry A String contained in username, first or last name, or email
	 * @return Stream of users without groups
	 */
	Stream<User> searchUser(String querry);

	/**
	 * Get all remote groups without members
	 * @return Stream of groups
	 */
	Stream<Group> groups();

	/**
	 * Get all remote groups including members
	 * @return Stream of groups
	 */
	Stream<Group> groupsIncludingMembers();

	/**
	 * Search for remote group excluding members
	 * @param groupname A String contained in groupname
	 * @return Stream of groups without members
	 */
	Stream<Group> searchGroup(String groupname);

}
