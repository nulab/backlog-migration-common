package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogIssueCategory

/**
  * @author uchida
  */
trait IssueCategoryService {

  def allIssueCategories(): Seq[BacklogIssueCategory]

  def add(backlogIssueCategory: BacklogIssueCategory): BacklogIssueCategory

  def remove(issueCategoryId: Long)

}
