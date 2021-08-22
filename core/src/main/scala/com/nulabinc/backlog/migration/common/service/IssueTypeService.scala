package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogIssueType

/**
 * @author
 *   uchida
 */
trait IssueTypeService {

  def allIssueTypes(): Seq[BacklogIssueType]

  def add(issueType: BacklogIssueType): BacklogIssueType

  def remove(issueTypeId: Long, defaultIssueTypeId: Long): Unit

}
