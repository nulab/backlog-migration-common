package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogIssueCategory

/**
 * @author uchida
 */
trait IssueCategoryService {

  def allIssueCategories(): Seq[BacklogIssueCategory]

  def add(backlogIssueCategory: BacklogIssueCategory): BacklogIssueCategory

  def remove(issueCategoryId: Long): Unit

}
