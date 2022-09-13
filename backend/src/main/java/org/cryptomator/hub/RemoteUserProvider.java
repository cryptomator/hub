package org.cryptomator.hub;

import org.cryptomator.hub.entities.Group;
import org.cryptomator.hub.entities.User;

import java.util.List;

public interface RemoteUserProvider {

	/**
	 * Get all remote users
	 * @return List of users
	 */
	List<User> users();

	/**
	 * Get all remote groups
	 * @return List of groups
	 */
	List<Group> groups();

}
