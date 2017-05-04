package com.nulabinc.backlog.migration.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.convert.Convert
import com.nulabinc.backlog.migration.convert.writes.CategoryWrites
import com.nulabinc.backlog.migration.domain.BacklogIssueCategory
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.backlog4j.api.option.AddCategoryParams

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueCategoryServiceImpl @Inject()(implicit val categoryWrites: CategoryWrites,
                                         @Named("projectKey") projectKey: String,
                                         backlog: BacklogClient)
    extends IssueCategoryService
    with Logging {

  override def allIssueCategories(): Seq[BacklogIssueCategory] =
    backlog.getCategories(projectKey).asScala.map(Convert.toBacklog(_))

  override def add(backlogIssueCategory: BacklogIssueCategory): BacklogIssueCategory = {
    val params = new AddCategoryParams(projectKey, backlogIssueCategory.name)
    Convert.toBacklog(backlog.addCategory(params))
  }

  override def remove(issueCategoryId: Long) = {
    backlog.removeCategory(projectKey, issueCategoryId)
  }

}
