package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogIssueCategory
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.Category

/**
  * @author uchida
  */
private[common] class CategoryWrites @Inject()() extends Writes[Category, BacklogIssueCategory] with Logging {

  override def writes(category: Category): BacklogIssueCategory = {
    BacklogIssueCategory(optId = Some(category.getId), name = category.getName, delete = false)
  }

}
