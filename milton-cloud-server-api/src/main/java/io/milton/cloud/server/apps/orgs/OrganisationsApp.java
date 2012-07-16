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

import io.milton.resource.CollectionResource;
import io.milton.resource.Resource;
import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.cloud.server.db.utils.OrganisationDao;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.vfs.db.utils.SessionManager;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class OrganisationsApp implements Application {

    private ApplicationManager applicationManager;
    private SpliffyResourceFactory resourceFactory;

    @Override
    public String getInstanceId() {
        return "organisations";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        applicationManager = _(ApplicationManager.class);
        this.resourceFactory = resourceFactory;
    }

    /**
     * For a root resource (ie where parent is null) the requestedname will be
     * the hostname
     *
     * @param parent
     * @param requestedName
     * @return
     */
    @Override
    public Resource getPage(Resource parent, String requestedName) {
        if (parent == null) {
            Website website = Website.findByDomainName(requestedName, SessionManager.session());
            if (website == null) {
                Organisation org = OrganisationDao.getRootOrg(SessionManager.session());
                if (org == null) {
                    throw new RuntimeException("No root organisation");
                }
                return new OrganisationRootFolder(applicationManager, org);
            }
        }
        return null;
    }

    @Override
    public void addBrowseablePages(CollectionResource parent, ResourceList children) {
    }

}
