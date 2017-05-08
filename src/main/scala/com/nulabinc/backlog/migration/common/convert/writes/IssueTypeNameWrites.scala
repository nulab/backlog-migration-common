package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogIssueType
import com.nulabinc.backlog.migration.common.utils.Logging

/**
  * @author uchida
  */
class IssueTypeNameWrites @Inject()() extends Writes[String, BacklogIssueType] with Logging {

  override def writes(name: String): BacklogIssueType = {
    BacklogIssueType(optId = None, name, BacklogConstantValue.ISSUE_TYPE_COLOR.getStrValue, delete = true)
  }

}
