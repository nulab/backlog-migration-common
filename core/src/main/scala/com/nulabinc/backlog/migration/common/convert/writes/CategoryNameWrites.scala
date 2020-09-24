package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogIssueCategory
import com.nulabinc.backlog.migration.common.utils.Logging

/**
  * @author uchida
  */
class CategoryNameWrites @Inject() () extends Writes[String, BacklogIssueCategory] with Logging {

  override def writes(name: String): BacklogIssueCategory = {
    BacklogIssueCategory(optId = None, name = name, delete = true)
  }

}
