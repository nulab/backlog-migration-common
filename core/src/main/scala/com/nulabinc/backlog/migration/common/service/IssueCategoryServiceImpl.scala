package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.CategoryWrites
import com.nulabinc.backlog.migration.common.domain.{BacklogIssueCategory, BacklogProjectKey}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.api.option.AddCategoryParams

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class IssueCategoryServiceImpl @Inject() (implicit
    val categoryWrites: CategoryWrites,
    projectKey: BacklogProjectKey,
    backlog: BacklogAPIClient
) extends IssueCategoryService
    with Logging {

  override def allIssueCategories(): Seq[BacklogIssueCategory] =
    backlog.getCategories(projectKey.value).asScala.toSeq.map(Convert.toBacklog(_))

  override def add(
      backlogIssueCategory: BacklogIssueCategory
  ): BacklogIssueCategory = {
    val params =
      new AddCategoryParams(projectKey.value, backlogIssueCategory.name)
    Convert.toBacklog(backlog.addCategory(params))
  }

  override def remove(issueCategoryId: Long) = {
    backlog.removeCategory(projectKey.value, issueCategoryId)
  }

}
