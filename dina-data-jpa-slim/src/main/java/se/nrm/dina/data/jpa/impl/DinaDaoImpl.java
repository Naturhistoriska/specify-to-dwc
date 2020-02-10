package se.nrm.dina.data.jpa.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List; 
import java.util.stream.Stream; 
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;  
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query; 
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import se.nrm.dina.data.jpa.DinaDao;  
import se.nrm.dina.datamodel.EntityBean; 

/**
 * CRUD operations to database
 *
 * @author idali
 * @param <T>
 */
@Slf4j
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
//@Asynchronous 
@TransactionAttribute(NOT_SUPPORTED)
public class DinaDaoImpl<T extends EntityBean> implements DinaDao<T>, Serializable {
   
  private final String nrm = "nrm";
  private final String collectionMemberId = "collectionMemberID";
  private final String fromDateKey = "fromDate";
  private final String toDateKey = "toDate";
 
  @PersistenceContext(unitName = "jpaNrmPU")
  private EntityManager nrmEntityManager;

  @PersistenceContext(unitName = "jpaGnmPU")
  private EntityManager gnmEntityManager;
   
  private Query query;

  public DinaDaoImpl() {

  }

  private EntityManager getEntityManager(boolean isNrm) {
    return isNrm ? nrmEntityManager : gnmEntityManager;
  }

  private EntityManager getEntityManager(String institution) {
    return institution.contains(nrm) ? nrmEntityManager : gnmEntityManager;
  }

  @Override
  public Stream<T> findByCollectonId(int collectionId, String institution, Date fromDate, Date toDate) {
    query = getEntityManager(institution)
            .createQuery(QueryBuilder.getInstance()
                    .buildQuery(collectionId, fromDate, toDate))
                    .setParameter(collectionMemberId, collectionId);
            
    if(fromDate != null) {
      query.setParameter(fromDateKey, fromDate);
    }  
    
    if(toDate != null) {
      query.setParameter(toDateKey, toDate);
    }  
    return query.getResultStream();
  }
 
  @Override
  public Stream<T> findAll(Class<T> clazz, boolean isNrm) {
    log.info("findAll : {} -- {}", clazz, isNrm);

    query = getEntityManager(isNrm).createNamedQuery(clazz.getSimpleName() + ".findAll");
    return query.getResultStream();
  }

  @Override
  public List<Integer> findAllIds(int collectionMemberID, boolean isNrm) {
    return getEntityManager(isNrm).createNamedQuery("Collectionobject.findAllIds")
            .setParameter(collectionMemberId, collectionMemberID).getResultList();
  }

  @Override
  public Stream<T> findByCollectionCode(int collectionId, List<Integer> ids, boolean isNrm) { 
    query = getEntityManager(isNrm)
            .createQuery(QueryBuilder.getInstance()
                    .buildFindCollectionObjectsByCollectionCodeQuery(collectionId, ids != null))
            .setParameter(collectionMemberId, collectionId);
    if(ids != null) {
      query.setParameter("ids", ids);
    } 
    return query.getResultStream();
  }
  
  @Override
  public Stream<T> findGeographyParents(int geographyId, boolean isNrm) {
    query = getEntityManager(isNrm)
            .createQuery(QueryBuilder.getInstance().buildGeographyParentQuery())
            .setParameter("geographyId", geographyId);
    return query.getResultStream();
  }
  
  @Override
  public Stream<T> findTaxonParents(int taxonId, boolean isNrm) {
    query = getEntityManager(isNrm)
            .createQuery(QueryBuilder.getInstance().buildTaxonParentsQuery())
            .setParameter("taxonId", taxonId);
    return query.getResultStream();
  }
 

  @Override
  public int getCollectionCount(int collectionMemberID, boolean isNrm) {
    log.info("getCollectionCount: {} ", collectionMemberID);

    Number number;
    query = getEntityManager(isNrm).createNamedQuery("Collectionobject.findCount")
            .setParameter(collectionMemberId, collectionMemberID);

    try {
      number = (Number) query.getSingleResult();
    } catch (Exception e) {
      log.info(e.getMessage());
      return 0;
    }
    return number.intValue();
  }

  @Override
  public List<T> findSMTPCollectionEventData() { 
    return getEntityManager(true)
            .createQuery(QueryBuilder.getInstance().buildFindSMTPEventDataQuery())
            .getResultList();
  }

  @Override
  public List<T> findOverdueLoans() { 
    return nrmEntityManager
            .createQuery(QueryBuilder.getInstance().buildOverdueLoanQuery())
            .setParameter("currentDueDate", new Date())
            .getResultList();
  }

  public Throwable getRootCause(final Throwable throwable) {
    final List<Throwable> list = getThrowableList(throwable);
    return list.size() < 2 ? null : (Throwable) list.get(list.size() - 1);
  }

  public List<Throwable> getThrowableList(Throwable throwable) {
    final List<Throwable> list = new ArrayList<>();
    while (throwable != null && list.contains(throwable) == false) {
      list.add(throwable);
      throwable = ExceptionUtils.getCause(throwable);
    }
    return list;
  } 
    
  public void closeEntityManager() {
    log.info("disposesing entityManage....");
    if (gnmEntityManager.isOpen()) {
      log.info("closing...");
      gnmEntityManager.close();
    }
    if(nrmEntityManager.isOpen()) {
      nrmEntityManager.close();
    }
  }
}
