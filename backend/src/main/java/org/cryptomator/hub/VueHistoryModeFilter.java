package org.cryptomator.hub;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A global http filter which redirects all 404-responses to the frontend app root. Necessary for using <a href="https://v3.router.vuejs.org/guide/essentials/history-mode.html#example-server-configurations">history mode in the vue router</a>
 * <p>
 * Implemention taken from <a href="https://quarkus.io/blog/quarkus-and-web-ui-development-mode/#handle-angular-routes">https://quarkus.io/blog/quarkus-and-web-ui-development-mode/#handle-angular-routes</a>
 */
@WebFilter(urlPatterns = "/*")
public class VueHistoryModeFilter extends HttpFilter {

	@ConfigProperty(name = "quarkus.resteasy.path")
	String apiPathPrefix;

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		chain.doFilter(request, response);

		// exclude requests to the ReST API from filtering:
		String contextRelativePath = request.getRequestURI().substring(request.getContextPath().length());
		if (response.getStatus() == 404 && !contextRelativePath.startsWith(apiPathPrefix)) {
			try {
				response.setStatus(200);
				request.getRequestDispatcher("/").forward(request, response);
			} finally {
				response.getOutputStream().close();
			}
		}
	}

}
