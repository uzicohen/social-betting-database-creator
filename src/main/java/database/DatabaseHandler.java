package database;

import java.util.List;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class DatabaseHandler {

	private final static Logger logger = Logger.getLogger(DatabaseHandler.class.getName());

	private static SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

	public static <T> void save(T entity) {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();
		session.save(entity);
		transaction.commit();
		session.close();
	}

	public static <T> void save(List<T> entities) {
		for (T e : entities) {
			save(e);
		}
	}

	public static void destroySessionFactory() {
		sessionFactory.close();
	}
}
