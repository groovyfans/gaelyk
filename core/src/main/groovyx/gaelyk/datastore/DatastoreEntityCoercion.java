package groovyx.gaelyk.datastore;

import groovy.lang.GString;
import groovyx.gaelyk.extensions.DatastoreExtensions;

import com.google.appengine.api.datastore.Entities;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;

public class DatastoreEntityCoercion {

    public static Entity convert(DatastoreEntity<?> dsEntity){
        if(dsEntity == null) return null;
        
        String kind = dsEntity.getClass().getSimpleName();
        Entity entity = null;
        
        if(dsEntity.hasDatastoreKey()){
            Object key = dsEntity.getDatastoreKey();
            if(key instanceof CharSequence && key != null){
                if (dsEntity.hasDatastoreParent()) {
                    entity = new Entity(kind, key.toString(), dsEntity.getDatastoreParent());
                } else {
                    entity = new Entity(kind, key.toString());
                }
            } else if (key instanceof Number && key != null && ((Number) key).longValue() != 0) {
                if (dsEntity.hasDatastoreParent()) {
                    entity = new Entity(kind, ((Number) key).longValue(), dsEntity.getDatastoreParent());
                } else {
                    entity = new Entity(kind, ((Number) key).longValue());
                }
            } else if(dsEntity.hasDatastoreParent()){
                entity = new Entity(kind, dsEntity.getDatastoreParent());
            }
        }
        
        if(entity == null){
            entity = new Entity(kind);
        }
        
        for(String propertyName : dsEntity.getDatastoreIndexedProperties()){
            entity.setProperty(propertyName, transformValueForStorage(dsEntity.getProperty(propertyName)));
        }
        for(String propertyName : dsEntity.getDatastoreUnindexedProperties()){
            entity.setUnindexedProperty(propertyName, transformValueForStorage(dsEntity.getProperty(propertyName)));
        }
        
        return entity;
    }
    
    private static Object transformValueForStorage(Object value) {
        Object newValue = (value instanceof GString || value instanceof Enum<?>) ? value.toString() : value;
        // if we store a string longer than 500 characters
        // it needs to be wrapped in a Text instance
        if (newValue instanceof String && ((String)newValue).length() > 500) {
            newValue = new Text((String) newValue);
        }
        return newValue;
    }

    @SuppressWarnings("unchecked") public static <E extends DatastoreEntity<?>> E convert(Entity en, Class<E> dsEntityClass) throws InstantiationException, IllegalAccessException{
        E dsEntity = dsEntityClass.newInstance();
        if(dsEntity.hasDatastoreKey()){
            if(dsEntity.hasDatastoreNumericKey()){
                ((DatastoreEntity<Long>)dsEntity).setDatastoreKey(en.getKey().getId());
            } else {
                ((DatastoreEntity<String>)dsEntity).setDatastoreKey(en.getKey().getName());
            }
        }
        if (dsEntity.hasDatastoreParent()) {
            dsEntity.setDatastoreParent(en.getKey().getParent());
        }
        if (dsEntity.hasDatastoreVersion()) {
            try {
                dsEntity.setDatastoreVersion(Entities.getVersionProperty(DatastoreExtensions.get(Entities.createEntityGroupKey(en.getKey()))));                
            } catch (Exception e) {
                dsEntity.setDatastoreVersion(0); 
            }
        }
        for(String propertyName : dsEntity.getDatastoreIndexedProperties()){
            setEntityProperty(en, dsEntity, propertyName);
        }
        for(String propertyName : dsEntity.getDatastoreUnindexedProperties()){
            setEntityProperty(en, dsEntity, propertyName);
        }
        return dsEntity;
    }

    private static <E extends DatastoreEntity<?>> void setEntityProperty(Entity en, E dsEntity, String propertyName) {
        Object value = en.getProperty(propertyName);
        if (value instanceof Text) {
            dsEntity.setProperty(propertyName, ((Text) value).getValue());
        } else {
            dsEntity.setProperty(propertyName, value);
        }
    }
    
}
