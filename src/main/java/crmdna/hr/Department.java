package crmdna.hr;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class Department {

    public static DepartmentProp create(String client, String displayName, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_DEPARTMENT);
        ensureNotNull(displayName, "displayName cannot be null");

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());

        List<Key<DepartmentEntity>> keys =
                ofy(client).load().type(DepartmentEntity.class).filter("name", name).keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a department with name [" + displayName + "]");

        String key = getUniqueKey(client, name);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a department (in cache) with name [" + displayName + "]");

        DepartmentEntity entity = new DepartmentEntity();
        entity.departmentId = Sequence.getNext(client, SequenceType.DEPARTMENT);
        entity.name = name;
        entity.displayName = displayName;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    private static String getUniqueKey(String namespace, String name) {
        return namespace + "_" + SequenceType.DEPARTMENT + "_" + name;
    }

    public static DepartmentEntity safeGet(String client, long departmentId) {

        Client.ensureValid(client);

        DepartmentEntity entity =
                ofy(client).load().type(DepartmentEntity.class).id(departmentId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Department id  [" + departmentId + "] does not exist");

        return entity;
    }

    public static DepartmentEntity safeGetByName(String client, String name) {

        Client.ensureValid(client);
        ensureNotNull(name);

        name = Utils.removeSpaceUnderscoreBracketAndHyphen(name.toLowerCase());
        List<DepartmentEntity> entities =
                ofy(client).load().type(DepartmentEntity.class).filter("name", name).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Department [" + name + "] does not exist");

        if (entities.size() > 1)
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Found [" + entities.size() + "] matches for department [" + name
                            + "]. Please specify Id");
        return entities.get(0);
    }

    public static DepartmentProp rename(String client, long departmentid, String newDisplayName,
                                        String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_DEPARTMENT);
        ensureNotNull(newDisplayName);

        DepartmentEntity entity = safeGet(client, departmentid);

        String newName = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());

        if (entity.name.equals(newName)) {
            // ideally should be inside a transaction
            entity.displayName = newDisplayName;
            ofy(client).save().entity(entity).now();
            return entity.toProp();
        }

        List<Key<DepartmentEntity>> keys =
                ofy(client).load().type(DepartmentEntity.class).filter("name", newName).keys().list();
        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a department with name [" + newDisplayName + "]");

        String key = getUniqueKey(client, newDisplayName);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a department with name [" + newDisplayName + "]");

        // ideally should be inside a transaction
        entity.name = newName;
        entity.displayName = newDisplayName;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static List<DepartmentProp> getAll(String client) {
        Client.ensureValid(client);

        List<DepartmentEntity> entities =
                ofy(client).load().type(DepartmentEntity.class).order("name").list();

        List<DepartmentProp> props = new ArrayList<>();
        for (DepartmentEntity entity : entities)
            props.add(entity.toProp());

        return props;
    }

    public static Map<Long, DepartmentEntity> get(String client, Iterable<Long> ids) {

        Map<Long, DepartmentEntity> map = ofy(client).load().type(DepartmentEntity.class).ids(ids);

        return map;
    }

    public static void delete(String client, long groupId, String login) {

        throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                "This functionality is not implemented yet");

        // GroupEntity groupEntity = safeGet(client, groupId);

        // ofy(client).delete().entity(groupEntity).now();
    }
}
