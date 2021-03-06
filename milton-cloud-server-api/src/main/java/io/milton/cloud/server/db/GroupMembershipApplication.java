/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.db;

import io.milton.vfs.db.Group;
import io.milton.vfs.db.NvSet;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.DbUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Restrictions;

/**
 * For closed groups, when a user applies for membership (ie by creating a new
 * account on the group's website) we will create a GroupMembershipApplication record
 * instead of a GroupMembership
 * 
 * This will prompt the admins to consider the request and accept or reject it
 *
 * @author brad
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupMembershipApplication implements Serializable {
    
    public static List<GroupMembershipApplication> findByAdminOrg(Organisation adminOrg, Session session) {
        Criteria crit = session.createCriteria(GroupMembershipApplication.class);
        crit.setCacheable(true);
        crit.add(Restrictions.eq("adminOrg", adminOrg));
        return DbUtils.toList(crit, GroupMembershipApplication.class);
    }
    
    private Long id;
    private Organisation adminOrg; // the org which will approve the membership
    private Organisation withinOrg;
    private Website website; // just to track what website the application came in on
    private Profile member;
    private Group groupEntity;
    private Date createdDate;
    private Date modifiedDate;
    private NvSet fields;
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    public NvSet getFields() {
        return fields;
    }

    public void setFields(NvSet fields) {
        this.fields = fields;
    }
    
    

    @ManyToOne(optional=false)    
    public Organisation getWithinOrg() {
        return withinOrg;
    }

    public void setWithinOrg(Organisation withinOrg) {
        this.withinOrg = withinOrg;
    }

    @ManyToOne(optional=false)
    public Organisation getAdminOrg() {
        return adminOrg;
    }

    public void setAdminOrg(Organisation adminOrg) {
        this.adminOrg = adminOrg;
    }

    @ManyToOne
    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    
    
    
    @ManyToOne(optional=false)
    public Profile getMember() {
        return member;
    }

    public void setMember(Profile member) {
        this.member = member;
    }

    @ManyToOne(optional=false)
    public Group getGroupEntity() {
        return groupEntity;
    }

    public void setGroupEntity(Group group) {
        this.groupEntity = group;
    }
    
    
    
    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Column(nullable=false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    
}
