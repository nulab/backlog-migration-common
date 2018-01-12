package com.nulabinc.backlog.migration

import java.io.InputStream

import com.google.inject.AbstractModule
import com.nulabinc.backlog.migration.common.domain.{BacklogCustomFieldSetting, BacklogIssue, BacklogUser}
import com.nulabinc.backlog.migration.common.service.{IssueService, PropertyResolver}
import com.nulabinc.backlog.migration.common.service.PropertyResolver
import com.nulabinc.backlog4j.Issue
import com.nulabinc.backlog4j.Issue.{PriorityType, ResolutionType}
import com.nulabinc.backlog4j.api.option.{GetIssuesCountParams, GetIssuesParams, ImportIssueParams}

/**
  * @author uchida
  */
class TestModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[IssueService]).to(classOf[TestIssueServiceImpl])
  }

}

class TestIssueServiceImpl extends IssueService with SimpleFixture {

  override def issueOfId(id: Long): BacklogIssue = {
    issue1.copy(optParentIssueId = None)
  }

  override def optIssueOfId(id: Long): Option[Issue] = {
    None
  }

  override def optIssueOfKey(key: String): Option[BacklogIssue] = ???

  override def issueOfKey(key: String): BacklogIssue = ???

  override def optIssueOfParams(projectId: Long, backlogIssue: BacklogIssue): Option[BacklogIssue] = ???

  override def allIssues(projectId: Long, offset: Int, count: Int, filter: Option[String]): Seq[Issue] = ???

  override def countIssues(projectId: Long, filter: Option[String]): Int = ???

  override def downloadIssueAttachment(issueId: Long, attachmentId: Long): Option[(String, InputStream)] = ???

  override def exists(projectId: Long, backlogIssue: BacklogIssue): Boolean = ???

  override def create(setCreateParam: (BacklogIssue) => ImportIssueParams)(backlogIssue: BacklogIssue): Either[Throwable, BacklogIssue] = ???

  override def createDummy(projectId: Long, propertyResolver: PropertyResolver): Issue = ???

  override def delete(issueId: Long): Unit = ???

  override def addIssuesParams(params: GetIssuesParams, filter: Option[String]): Unit = ???

  override def addIssuesCountParams(params: GetIssuesCountParams, filter: Option[String]): Unit = ???

  override def setCreateParam(projectId: Long,
                              propertyResolver: PropertyResolver,
                              toRemoteIssueId: (Long) => Option[Long],
                              postAttachment: (String) => Option[Long],
                              issueOfId: (Long) => BacklogIssue)(backlogIssue: BacklogIssue): ImportIssueParams = ???

  override def deleteAttachment(issueId: Long, attachmentId: Long, createdUserId: Long, created: String): Unit = ???
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
