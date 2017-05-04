package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogIssueCategory
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.Category

/**
  * @author uchida
  */
class CategoryWrites @Inject()() extends Writes[Category, BacklogIssueCategory] with Logging {

  override def writes(category: Category): BacklogIssueCategory = {
    BacklogIssueCategory(optId = Some(category.getId), name = category.getName, delete = false)
  }

}
