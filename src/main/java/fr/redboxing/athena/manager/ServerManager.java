package fr.redboxing.athena.manager;

import fr.redboxing.athena.database.DatabaseManager;
import fr.redboxing.athena.database.entities.Server;
import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

public class ServerManager {
    public static Optional<Server> getServerByAddress(String address) {
        try(Session session = DatabaseManager.getSessionFactory().openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Server> criteriaQuery = builder.createQuery(Server.class);
            Root<Server> root = criteriaQuery.from(Server.class);
            criteriaQuery.select(root);

            CriteriaQuery<Server> query = criteriaQuery.where(builder.equal(root.get("address"), address));
            Optional<Server> server = session.createQuery(query).uniqueResultOptional();
            session.getTransaction().commit();

            return server;
        }
    }

    public static int getTotalServerCounts() {
        try(Session session = DatabaseManager.getSessionFactory().openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
            Root<Server> root = criteriaQuery.from(Server.class);
            criteriaQuery.select(builder.count(root));

            Long count = session.createQuery(criteriaQuery).uniqueResult();
            session.getTransaction().commit();

            return count.intValue();
        }
    }

    public static List<String> getAllAddress() {
        try(Session session = DatabaseManager.getSessionFactory().openSession()) {
            session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<String> criteriaQuery = builder.createQuery(String.class);
            Root<Server> root = criteriaQuery.from(Server.class);
            criteriaQuery.select(root.get("address"));

            List<String> addresses = session.createQuery(criteriaQuery).getResultList();
            session.getTransaction().commit();

            return addresses;
        }
    }
}
