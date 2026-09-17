package com.sly.coffer.data.backup;

import android.net.Uri;

import com.sly.coffer.data.backup.pojo.AccessibilityRuleKeywordGroupPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRulePojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccessibilityRuleTransferPojo;
import com.sly.coffer.data.backup.pojo.AccountPojo;
import com.sly.coffer.data.backup.pojo.AccountTagRefPojo;
import com.sly.coffer.data.backup.pojo.AccountTransferPojo;
import com.sly.coffer.data.backup.pojo.BudgetPojo;
import com.sly.coffer.data.backup.pojo.BudgetTagRefPojo;
import com.sly.coffer.data.backup.pojo.MediaPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleGroupRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRulePojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTagRefPojo;
import com.sly.coffer.data.backup.pojo.NotificationRuleTransferPojo;
import com.sly.coffer.data.backup.pojo.TagGroupPojo;
import com.sly.coffer.data.backup.pojo.TagPojo;
import com.sly.coffer.data.save.db.converters.DateTimeConverter;
import com.sly.coffer.data.save.db.converters.UriConverter;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleKeywordGroupEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccessibilityRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.AccountEntity;
import com.sly.coffer.data.save.db.entities.AccountTagRefEntity;
import com.sly.coffer.data.save.db.entities.AccountTransferEntity;
import com.sly.coffer.data.save.db.entities.BudgetEntity;
import com.sly.coffer.data.save.db.entities.BudgetTagRefEntity;
import com.sly.coffer.data.save.db.entities.MediaEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleGroupRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTagRefEntity;
import com.sly.coffer.data.save.db.entities.NotificationRuleTransferEntity;
import com.sly.coffer.data.save.db.entities.TagEntity;
import com.sly.coffer.data.save.db.entities.TagGroupEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EntityPojoMapper {
    EntityPojoMapper INSTANCE = Mappers.getMapper(EntityPojoMapper.class);

    @Named("longToTime")
    default LocalDateTime longToTime(long timeMillis) {
        return DateTimeConverter.toLocalDateTime(timeMillis);
    }

    @Named("timeToLong")
    default long timeToLong(LocalDateTime time) {
        return DateTimeConverter.fromLocalDateTime(time);
    }

    @Named("longToDate")
    default LocalDate longToDate(long timeMillis) {
        return DateTimeConverter.toLocalDate(timeMillis);
    }

    @Named("dateToLong")
    default long dateToLong(LocalDate date) {
        return DateTimeConverter.fromLocalDate(date);
    }

    @Named("strToUri")
    default Uri strToUri(String string) {
        return UriConverter.toUri(string);
    }

    @Named("uriToStr")
    default String uriToStr(Uri uri) {
        return UriConverter.fromUri(uri);
    }

    @Mapping(target = "dateTime", source = "dateTime", qualifiedByName = "longToTime")
    AccountEntity toAccountEntity(AccountPojo pojo);

    List<AccountEntity> toAccountEntityList(List<AccountPojo> pojoList);

    @Mapping(target = "dateTime", source = "dateTime", qualifiedByName = "timeToLong")
    AccountPojo toAccountPojo(AccountEntity entity);

    List<AccountPojo> toAccountPojoList(List<AccountEntity> entityList);

    AccountTagRefEntity toAccountTagRefEntity(AccountTagRefPojo pojo);

    List<AccountTagRefEntity> toAccountTagRefEntityList(List<AccountTagRefPojo> pojoList);

    AccountTagRefPojo toAccountTagRefPojo(AccountTagRefEntity entity);

    List<AccountTagRefPojo> toAccountTagRefPojoList(List<AccountTagRefEntity> entityList);

    AccountTransferEntity toAccountTransferEntity(AccountTransferPojo pojo);

    List<AccountTransferEntity> toAccountTransferEntityList(List<AccountTransferPojo> pojoList);

    AccountTransferPojo toAccountTransferPojo(AccountTransferEntity entity);

    List<AccountTransferPojo> toAccountTransferPojoList(List<AccountTransferEntity> entityList);

    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "longToDate")
    BudgetEntity toBudgetEntity(BudgetPojo pojo);

    List<BudgetEntity> toBudgetEntityList(List<BudgetPojo> pojoList);

    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "dateToLong")
    BudgetPojo toBudgetPojo(BudgetEntity entity);

    List<BudgetPojo> toBudgetPojoList(List<BudgetEntity> entityList);

    BudgetTagRefEntity toBudgetTagRefEntity(BudgetTagRefPojo pojo);

    List<BudgetTagRefEntity> toBudgetTagRefEntityList(List<BudgetTagRefPojo> pojoList);

    BudgetTagRefPojo toBudgetTagRefPojo(BudgetTagRefEntity entity);

    List<BudgetTagRefPojo> toBudgetTagRefPojoList(List<BudgetTagRefEntity> entityList);

    @Mapping(target = "fileUri", source = "fileUri", qualifiedByName = "strToUri")
    MediaEntity toMediaEntity(MediaPojo pojo);

    List<MediaEntity> toMediaEntityList(List<MediaPojo> pojoList);

    @Mapping(target = "fileUri", source = "fileUri", qualifiedByName = "uriToStr")
    MediaPojo toMediaPojo(MediaEntity entity);

    List<MediaPojo> toMediaPojoList(List<MediaEntity> entityList);

    NotificationRuleEntity toNotificationRuleEntity(NotificationRulePojo pojo);

    List<NotificationRuleEntity> toNotificationRuleEntityList(List<NotificationRulePojo> pojoList);

    NotificationRulePojo toNotificationRulePojo(NotificationRuleEntity entity);

    List<NotificationRulePojo> toNotificationRulePojoList(List<NotificationRuleEntity> entityList);

    NotificationRuleTagRefEntity toNotificationRuleTagRefEntity(NotificationRuleTagRefPojo pojo);

    List<NotificationRuleTagRefEntity> toNotificationRuleTagRefEntityList(List<NotificationRuleTagRefPojo> pojoList);

    NotificationRuleTagRefPojo toNotificationRuleTagRefPojo(NotificationRuleTagRefEntity entity);

    List<NotificationRuleTagRefPojo> toNotificationRuleTagRefPojoList(List<NotificationRuleTagRefEntity> entityList);

    NotificationRuleTransferEntity toNotificationRuleTransferEntity(NotificationRuleTransferPojo pojo);

    List<NotificationRuleTransferEntity> toNotificationRuleTransferEntityList(List<NotificationRuleTransferPojo> pojoList);

    NotificationRuleTransferPojo toNotificationRuleTransferPojo(NotificationRuleTransferEntity entity);

    List<NotificationRuleTransferPojo> toNotificationRuleTransferPojoList(List<NotificationRuleTransferEntity> entityList);

    NotificationRuleGroupEntity toNotificationRuleGroupEntity(NotificationRuleGroupPojo pojo);

    List<NotificationRuleGroupEntity> toNotificationRuleGroupEntityList(List<NotificationRuleGroupPojo> pojoList);

    NotificationRuleGroupPojo toNotificationRuleGroupPojo(NotificationRuleGroupEntity entity);

    List<NotificationRuleGroupPojo> toNotificationRuleGroupPojoList(List<NotificationRuleGroupEntity> entityList);

    NotificationRuleGroupRefEntity toNotificationRuleGroupRefEntity(NotificationRuleGroupRefPojo pojo);

    List<NotificationRuleGroupRefEntity> toNotificationRuleGroupRefEntityList(List<NotificationRuleGroupRefPojo> pojoList);

    NotificationRuleGroupRefPojo toNotificationRuleGroupRefPojo(NotificationRuleGroupRefEntity entity);

    List<NotificationRuleGroupRefPojo> toNotificationRuleGroupRefPojoList(List<NotificationRuleGroupRefEntity> entityList);

    TagEntity toTagEntity(TagPojo pojo);

    List<TagEntity> toTagEntityList(List<TagPojo> pojoList);

    TagPojo toTagPojo(TagEntity entity);

    List<TagPojo> toTagPojoList(List<TagEntity> entityList);

    TagGroupEntity toTagGroupEntity(TagGroupPojo pojo);

    List<TagGroupEntity> toTagGroupEntityList(List<TagGroupPojo> pojoList);

    TagGroupPojo toTagGroupPojo(TagGroupEntity entity);

    List<TagGroupPojo> toTagGroupPojoList(List<TagGroupEntity> entityList);

    AccessibilityRuleEntity toAccessibilityRuleEntity(AccessibilityRulePojo pojo);

    List<AccessibilityRuleEntity> toAccessibilityRuleEntityList(List<AccessibilityRulePojo> pojoList);

    AccessibilityRulePojo toAccessibilityRulePojo(AccessibilityRuleEntity entity);

    List<AccessibilityRulePojo> toAccessibilityRulePojoList(List<AccessibilityRuleEntity> entityList);

    AccessibilityRuleTagRefEntity toAccessibilityRuleTagRefEntity(AccessibilityRuleTagRefPojo pojo);

    List<AccessibilityRuleTagRefEntity> toAccessibilityRuleTagRefEntityList(List<AccessibilityRuleTagRefPojo> pojoList);

    AccessibilityRuleTagRefPojo toAccessibilityRuleTagRefPojo(AccessibilityRuleTagRefEntity entity);

    List<AccessibilityRuleTagRefPojo> toAccessibilityRuleTagRefPojoList(List<AccessibilityRuleTagRefEntity> entityList);

    AccessibilityRuleTransferEntity toAccessibilityRuleTransferEntity(AccessibilityRuleTransferPojo pojo);

    List<AccessibilityRuleTransferEntity> toAccessibilityRuleTransferEntityList(List<AccessibilityRuleTransferPojo> pojoList);

    AccessibilityRuleTransferPojo toAccessibilityRuleTransferPojo(AccessibilityRuleTransferEntity entity);

    List<AccessibilityRuleTransferPojo> toAccessibilityRuleTransferPojoList(List<AccessibilityRuleTransferEntity> entityList);

    AccessibilityRuleKeywordGroupEntity toAccessibilityRuleKeywordGroupEntity(AccessibilityRuleKeywordGroupPojo pojo);

    List<AccessibilityRuleKeywordGroupEntity> toAccessibilityRuleKeywordGroupEntityList(List<AccessibilityRuleKeywordGroupPojo> pojoList);

    AccessibilityRuleKeywordGroupPojo toAccessibilityRuleKeywordGroupPojo(AccessibilityRuleKeywordGroupEntity entity);

    List<AccessibilityRuleKeywordGroupPojo> toAccessibilityRuleKeywordGroupPojoList(List<AccessibilityRuleKeywordGroupEntity> entityList);
}
