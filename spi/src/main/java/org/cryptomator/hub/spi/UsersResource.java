package org.cryptomator.hub.spi;

import io.quarkus.oidc.UserInfo;
import org.cryptomator.hub.persistence.entities.UserDao;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/users")
@Produces(MediaType.TEXT_PLAIN)
public class UsersResource {

    @Inject
    UserInfo userInfo;

    @Inject
    UserDao userDao;

    @GET
    @Path("/me")
    @RolesAllowed("user")
    @NoCache
    public String me() {
        return userDao.get(userInfo.getString("sub")).getName();
    }

}