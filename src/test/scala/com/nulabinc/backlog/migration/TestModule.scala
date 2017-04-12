package com.nulabinc.backlog.migration

import com.google.inject.AbstractModule
import com.nulabinc.backlog.migration.domain.BacklogCustomFieldSetting
import com.nulabinc.backlog.migration.service.PropertyResolver
import com.nulabinc.backlog4j.Issue.{PriorityType, ResolutionType}

/**
  * @author uchida
  */
class TestModule extends AbstractModule {

  override def configure(): Unit = {}

}

class TestPropertyResolver extends PropertyResolver with SimpleFixture {

  override def optResolvedVersionId(name: String): Option[Long] = {
    if (name == versionName1) Some(versionId1)
    else if (name == versionName2) Some(versionId2)
    else if (name == versionName3) Some(versionId3)
    else if (name == versionName4) Some(versionId4)
    else None
  }

  override def optResolvedCustomFieldSetting(name: String): Option[BacklogCustomFieldSetting] = {
    if (name == textCustomFieldName) Some(textCustomFieldSetting)
    else if (name == textAreaCustomFieldName) Some(textAreaCustomFieldSetting)
    else if (name == numericCustomFieldName) Some(numericCustomFieldSetting)
    else if (name == dateCustomFieldName) Some(dateCustomFieldSetting)
    else if (name == singleListCustomFieldName) Some(singleListCustomFieldSetting)
    else if (name == multipleListCustomFieldName) Some(multipleListCustomFieldSetting)
    else if (name == checkBoxCustomFieldName) Some(checkBoxCustomFieldSetting)
    else if (name == radioCustomFieldName) Some(radioCustomFieldSetting)
    else None
  }

  override def optResolvedCategoryId(name: String): Option[Long] = {
    if (name == categoryName1) Some(categoryId1)
    else if (name == categoryName2) Some(categoryId2)
    else None
  }

  override def optResolvedIssueTypeId(name: String): Option[Long] = {
    if (issueTypeName == name) Some(issueTypeId) else None
  }

  override def tryDefaultIssueTypeId(): Long = {
    1
  }

  override def optResolvedUserId(userId: String): Option[Long] = {
    if (userIdString1 == userId) Some(userId1)
    else if (userIdString2 == userId) Some(userId2)
    else if (userIdString3 == userId) Some(userId3)
    else None
  }

  override def tryResolvedStatusId(name: String): Int = {
    1
  }

  override def optResolvedResolutionId(name: String): Option[Long] = {
    Some(ResolutionType.valueOf(name).getIntValue)
  }

  override def optResolvedPriorityId(name: String): Option[Long] = {
    Some(PriorityType.valueOf(name).getIntValue)
  }

}
