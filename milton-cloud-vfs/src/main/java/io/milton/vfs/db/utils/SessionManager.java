package io.milton.vfs.db.utils;

import io.milton.cloud.common.With;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 *
 * @author brad
 */
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    
    private static ThreadLocal<Session> tlSession = new ThreadLocal<>();

    public static Session session() {
        //return SessionFactoryUtils.getSession(sessionFactory, false);
        return tlSession.get();
    }

    public static Transaction beginTx() {
        return session().beginTransaction();
    }

    public static void commit(Transaction tx) {
        Session s = session();
        s.flush();
        tx.commit();
    }
    private static SessionFactory sessionFactory;

    public SessionManager(SessionFactory sf) {
        sessionFactory = sf;
    }

    public Session open() {
        Session session = SessionFactoryUtils.getSession(sessionFactory, true);
        tlSession.set(session);
        return session;
    }

    public void close() {
        Session s = tlSession.get();
        if (s != null) {
            SessionFactoryUtils.closeSession(s);
        }
        tlSession.remove();
    }

    public org.hibernate.Cache getCache() {
        return sessionFactory.getCache();
    }

    public <X> X doLocalTx(With<Session, X> callback) {
        Session s = sessionFactory.openSession();
        try {
            return callback.use(s);
        } catch(Exception e) {
            log.error("Exception thrown from callback", e);
            throw new RuntimeException(e);
        } finally {
            s.close();
        }
    }
}
