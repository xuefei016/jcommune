SET @adminUserName := 'admin';
SET @adminGroupName := 'Administrators';
SET @forumComponentName := 'JTalks Sample Forum';
SET @forumComponentType := 'FORUM';
SET @aclClass :='COMPONENT';

ALTER TABLE JC_USER_DETAILS ADD UNIQUE (USER_ID);

insert ignore into COMPONENTS (CMP_ID, COMPONENT_TYPE, UUID, `NAME`, DESCRIPTION) VALUES (2, @forumComponentType, (SELECT UUID() FROM dual), @forumComponentName, 'Available users: admin/admin');

-- 'FROM COMPONENTS' are not used, but query mast contain 'FROM dual' clause
--  @see <a href="http://dev.mysql.com">http://dev.mysql.com/doc/refman/5.0/en/select.html/a>.
insert into GROUPS (UUID, `NAME`, DESCRIPTION) select (SELECT UUID() FROM dual), 'Moderators', 'General group for all moderators' from dual where not exists (select GROUP_ID from GROUPS where `NAME`='Moderators');

INSERT INTO GROUPS (UUID, `NAME`, DESCRIPTION)
  SELECT (SELECT UUID() FROM dual), @adminGroupName, 'Administrators group.' FROM dual
    WHERE NOT EXISTS (SELECT gr.GROUP_ID FROM GROUPS gr WHERE gr.NAME=@adminGroupName);

-- IGNORE can be used here because USERNAME is unique column, so if table contain user with username='Admin', record
--  will not be added.
INSERT IGNORE INTO USERS (UUID, FIRST_NAME, LAST_NAME, USERNAME, ENCODED_USERNAME, EMAIL, PASSWORD, ROLE, SALT)
  VALUES((SELECT UUID() FROM dual), @adminUserName, @adminUserName, @adminUserName, @adminUserName, 'admin@jtalks.org', MD5('admin'), 'ADMIN_ROLE', '');

insert ignore into JC_USER_DETAILS (USER_ID, REGISTRATION_DATE, POST_COUNT) values ((select ID from USERS where USERNAME = 'admin'), NOW(), 0);

-- Adding created Admin to Administrators group(created at this migration or common migration) ).
SET @admin_group_id := (select GROUP_ID from GROUPS where `NAME`='Administrators');
insert into GROUP_USER_REF (GROUP_ID, USER_ID) select @admin_group_id, ID from USERS where USERNAME = 'admin' and not exists (select * from GROUP_USER_REF where GROUP_ID = @admin_group_id and USER_ID = USERS.ID);

-- Adding record with added component class.
set @component_acl_class=1;
set @group_acl_class=2;
set @branch_acl_class=3;
insert ignore into acl_class values (@branch_acl_class,'BRANCH'), (@group_acl_class,'GROUP'), (@component_acl_class,'COMPONENT');

SET @acl_sid_group := (SELECT GROUP_CONCAT('usergroup:', CONVERT(GROUP_ID, char(19))) FROM GROUPS g WHERE g.NAME = @adminGroupName);
SET @acl_sid_user := (SELECT GROUP_CONCAT('user:', CONVERT(ID, char(19))) FROM USERS u WHERE u.USERNAME = @adminUserName);
SET @object_id_identity := (SELECT component.CMP_ID FROM COMPONENTS component WHERE component.COMPONENT_TYPE = @forumComponentType);

-- Adding record to acl_sid table, this record wires sid and user id.
INSERT INTO acl_sid (principal, sid)
  select 1, @acl_sid_user from dual where not exists (select acl_sid.sid from acl_sid where sid = @acl_sid_user);

SET @acl_sid_id_user := (SELECT sid.id FROM acl_sid sid WHERE sid.sid = @acl_sid_user);

-- Adding record to acl_sid table, this record wires sid and group id.
INSERT IGNORE INTO acl_sid (principal, sid)
  VALUES(0, @acl_sid_group);

SET @acl_sid_id_group := (SELECT sid.id FROM acl_sid sid WHERE sid.sid = @acl_sid_group);

SET @acl_class_id :=(SELECT class.id FROM acl_class class WHERE class.class = @aclClass);

INSERT IGNORE INTO acl_object_identity (object_id_class, object_id_identity, owner_sid, entries_inheriting)
  SELECT @acl_class_id, @object_id_identity, @acl_sid_id_user, 1 FROM dual;

SET @acl_object_identity_id := (SELECT aoi.id FROM acl_object_identity aoi
WHERE aoi.object_id_class = @acl_class_id
      AND aoi.object_id_identity = @object_id_identity);

SET @ace_order_max := (SELECT MAX(ae.ace_order) FROM acl_entry ae);
SET @ace_order := (CASE WHEN  @ace_order_max is null THEN 0 ELSE @ace_order_max+1 END);

INSERT IGNORE INTO acl_entry (acl_object_identity, sid, ace_order, mask, granting, audit_success, audit_failure)
  SELECT @acl_object_identity_id, @acl_sid_id_group, @ ace_order, 16, 1, 0 , 0 FROM dual;