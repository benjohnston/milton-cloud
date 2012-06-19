/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.orgs;

import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.hibernate.Session;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.*;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Permission;
import io.milton.vfs.db.Profile;
import io.milton.principal.Principal;
import io.milton.http.exceptions.BadRequestException;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.Resource;
import io.milton.vfs.db.*;
import io.milton.vfs.db.utils.SessionManager;

/**
 * This is the root folder for the admin site. The admin site is used to setup
 * users and websites accessing the server
 *
 * @author brad
 */
public class OrganisationRootFolder extends OrganisationFolder implements RootFolder {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OrganisationRootFolder.class);
    private Map<String, PrincipalResource> childEntities = new HashMap<>();
    private final ApplicationManager applicationManager;
    private final Organisation organisation;
    private ResourceList children;

    public OrganisationRootFolder(Services services, ApplicationManager applicationManager, Organisation organisation) {
        super(null, organisation, services);
        this.applicationManager = applicationManager;
        this.organisation = organisation;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (method.equals(Method.PROPFIND)) { // force login for webdav browsing
            return services.getSecurityManager().getCurrentUser() != null;
        }
        return true;
    }


    @Override
    public Resource child(String childName) throws NotAuthorizedException, BadRequestException {
        Resource r = applicationManager.getPage(this, childName);
        if (r != null) {
            return r;
        }
        PrincipalResource p = findEntity(childName);
        if (p != null) {
            return p;
        }
        return Utils.childOf(getChildren(), childName);
    }

    @Override
    public PrincipalResource findEntity(String name) {
        PrincipalResource r = childEntities.get(name);
        if (r != null) {
            return r;
        }
        Session session = SessionManager.session();
        Profile u = services.getSecurityManager().getUserDao().getProfile(name, organisation, session);
        if (u == null) {
            return null;
        } else {
            UserResource ur = new UserResource(this, u, applicationManager);
            childEntities.put(name, ur);
            return ur;
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
        if (children == null) {
            children = new ResourceList();
            Profile user = getCurrentUser();
            if (user != null) {
                PrincipalResource r = findEntity(getCurrentUser().getName());
                if (r != null) {
                    children.add(r);
                }
            }
            if (organisation.getRepositories() != null) {
                for (Repository repo : organisation.getRepositories()) {
                    RepositoryFolder rf = new RepositoryFolder(this, repo, false);
                    children.add(rf);
                }
            }
            children.add(new OrganisationsFolder("organisations", this, services, organisation));
            
            services.getApplicationManager().addBrowseablePages(this, children);
        }
        return children;
    }

}