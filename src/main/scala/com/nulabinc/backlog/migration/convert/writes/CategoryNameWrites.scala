package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogIssueCategory
import com.nulabinc.backlog.migration.utils.Logging

/**
  * @author uchida
  */
class CategoryNameWrites @Inject()() extends Writes[String, BacklogIssueCategory] with Logging {

  override def writes(name: String): BacklogIssueCategory = {
    BacklogIssueCategory(optId = None, name = name, delete = true)
  }

}
