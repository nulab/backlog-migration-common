package com.nulabinc.backlog.migration.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.domain.BacklogIssueCategory
import com.nulabinc.backlog.migration.convert.Backlog4jConverters
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.api.option.AddCategoryParams
import com.nulabinc.backlog4j.{BacklogClient, Category}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueCategoryServiceImpl @Inject()(@Named("projectKey") projectKey: String, backlog: BacklogClient) extends IssueCategoryService with Logging {

  override def allIssueCategories(): Seq[BacklogIssueCategory] =
    backlog.getCategories(projectKey).asScala.map(Backlog4jConverters.Category.apply)

  override def add(backlogIssueCategory: BacklogIssueCategory): BacklogIssueCategory = {
    val params = new AddCategoryParams(projectKey, backlogIssueCategory.name)
    Backlog4jConverters.Category(backlog.addCategory(params))
  }

  override def remove(issueCategoryId: Long) = {
    backlog.removeCategory(projectKey, issueCategoryId)
  }

}
