package com.nulabinc.backlog.migration.common.service

import java.io.InputStream

import com.nulabinc.backlog.migration.common.client.params.ImportIssueParams
import com.nulabinc.backlog.migration.common.domain.BacklogIssue
import com.nulabinc.backlog4j.Issue
import com.nulabinc.backlog4j.api.option.{GetIssuesCountParams, GetIssuesParams}

/**
  * @author uchida
  */
trait IssueService {

  def issueOfId(id: Long): BacklogIssue

  def optIssueOfId(id: Long): Option[Issue]

  def optIssueOfKey(key: String): Option[BacklogIssue]

  def issueOfKey(key: String): BacklogIssue

  def optIssueOfParams(projectId: Long, backlogIssue: BacklogIssue): Option[BacklogIssue]

  def allIssues(projectId: Long, offset: Int, count: Int, filter: Option[String]): Seq[Issue]

  def countIssues(projectId: Long, filter: Option[String]): Int

  def downloadIssueAttachment(issueId: Long, attachmentId: Long): Option[(String, InputStream)]

  def exists(projectId: Long, backlogIssue: BacklogIssue): Boolean

  def create(setCreateParam: BacklogIssue => ImportIssueParams)(backlogIssue: BacklogIssue): Either[Throwable, BacklogIssue]

  def setCreateParam(projectId: Long,
                     propertyResolver: PropertyResolver,
                     toRemoteIssueId: Long => Option[Long],
                     postAttachment: String => Option[Long],
                     issueOfId: Long => BacklogIssue)(backlogIssue: BacklogIssue): ImportIssueParams

  def createDummy(projectId: Long, propertyResolver: PropertyResolver): Issue

  def delete(issueId: Long): Unit

  def deleteAttachment(issueId: Long, attachmentId: Long, createdUserId: Long, created: String): Unit

  def addIssuesParams(params: GetIssuesParams, filter: Option[String]): Unit

  def addIssuesCountParams(params: GetIssuesCountParams, filter: Option[String]): Unit

}
