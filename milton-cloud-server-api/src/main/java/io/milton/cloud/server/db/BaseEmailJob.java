/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for entities which represent email jobs, such as a group email or
 * an email trigger
 *
 * This base class is not itself has no status information because in this
 * abstract sense it can't be sent.
 *
 * @author brad
 */
@Entity
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING, length = 2)
@Inheritance(strategy = InheritanceType.JOINED)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class BaseEmailJob implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(BaseEmailJob.class);
    
    public abstract void accept(EmailJobVisitor visitor);

    /**
     * Indicates if a job is 'active' in the sense that email items associated
     * with it should be sent. A job may be paused or canceled, in which case
     * any email items associated with the job should not be sent. However,
     * status fields are associated with concrete subclasses of BaseEmailJob
     *
     * @return
     */
    @Transient
    public abstract boolean isActive();
    private List<GroupRecipient> groupRecipients;
    private long id;
    private String type;
    private Organisation organisation;
    private Website themeSite; // if present, the email will use the email template from the website's live theme
    private String name;
    private String title;
    private String notes;
    private String subject;
    private String fromAddress;
    private String html;
    private String filterScriptXml; // if present will filter out recipients which do not evaluate to true
    private Boolean deleted;

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(optional = false)
    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    @Column(nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Column
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Column
    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @OneToMany(mappedBy = "job")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public List<GroupRecipient> getGroupRecipients() {
        return groupRecipients;
    }

    public void setGroupRecipients(List<GroupRecipient> groupRecipients) {
        this.groupRecipients = groupRecipients;
    }

    @ManyToOne
    public Website getThemeSite() {
        return themeSite;
    }

    public void setThemeSite(Website themeSite) {
        this.themeSite = themeSite;
    }

    @Column(length = 2048)
    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Column
    public String getFilterScriptXml() {
        return filterScriptXml;
    }

    public void setFilterScriptXml(String filterScriptXml) {
        this.filterScriptXml = filterScriptXml;
    }

    /**
     * Adds, but does not save, the group recipient
     */
    public void addGroupRecipient(Group g) {
        if (this.getGroupRecipients() == null) {
            setGroupRecipients(new ArrayList<GroupRecipient>());
        }
        GroupRecipient gr = new GroupRecipient();
        gr.setJob(this);
        gr.setRecipient(g);
        getGroupRecipients().add(gr);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Soft deletion flag
     *
     * @return
     */
    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public void delete(Session session) {
        long numItems = countItems(session);
        if (numItems > 0) {
            softDelete(session);
        } else {
            physicalDelete(session);
        }
    }

    public void softDelete(Session session) {
        log.info("softDelete: " + getId());
        setDeleted(true);
        setName(getName() + "-Deleted-" + System.currentTimeMillis());
        session.save(this);
    }

    public void physicalDelete(Session session) {
        log.info("physicalDelete: " + getId());
        if (groupRecipients != null) {
            for (GroupRecipient gr : groupRecipients) {
                session.delete(gr);
            }
            groupRecipients.clear();
        }
        for (EmailItem i : EmailItem.findByJob(this, null, null, session)) {
            i.delete(session);
        }
        session.delete(this);
    }

    public boolean deleted() {
        if (deleted == null) {
            return false;
        } else {
            return deleted.booleanValue();
        }
    }

    public List<EmailItem> listItems(Session session) {
        return EmailItem.findByJob(this, null, null, session);
    }

    public List<EmailItem> listItems(Boolean sortByDate, Session session) {
        return EmailItem.findByJob(this, null, sortByDate, session);
    }
    
    public long countIncompleteItems(Session session) {
        return EmailItem.countIncompleteByJob(this, session);
    }

    public long countItems(Session session) {
        return EmailItem.countByJob(this, session);
    }

    public long countFailedItems(Session session) {
        return EmailItem.countByJob(this, EmailItem.STATUS_FAILED, session);
    }
    
    public long countSuccessulItems(Session session) {
        return EmailItem.countByJob(this, EmailItem.STATUS_COMPLETE, session);
    }

    
    
    
}
