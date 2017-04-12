package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogIssueType

/**
  * @author uchida
  */
trait IssueTypeService {

  def allIssueTypes(): Seq[BacklogIssueType]

  def add(issueType: BacklogIssueType): BacklogIssueType

  def remove(issueTypeId: Long, defaultIssueTypeId: Long)

}
