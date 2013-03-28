package de.adorsys.forge.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * 
 * @author sso
 *
 */
public class JAXRSApplication extends Application {
	private static final Set<Object> SERVICES = new HashSet<Object>();

	public JAXRSApplication() {
	}

	@Override
	public Set<Object> getSingletons() {
		return SERVICES;
	}

	public static Set<Object> getServices() {
		return SERVICES;
	}
}