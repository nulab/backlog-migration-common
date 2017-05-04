package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogIssueType
import com.nulabinc.backlog.migration.utils.Logging

/**
  * @author uchida
  */
class IssueTypeNameWrites @Inject()() extends Writes[String, BacklogIssueType] with Logging {

  override def writes(name: String): BacklogIssueType = {
    BacklogIssueType(optId = None, name, BacklogConstantValue.ISSUE_TYPE_COLOR.getStrValue, delete = true)
  }

}
