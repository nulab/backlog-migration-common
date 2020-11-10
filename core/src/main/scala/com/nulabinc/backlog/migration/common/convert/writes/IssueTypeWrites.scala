package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogIssueType
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.IssueType

/**
 * @author uchida
 */
class IssueTypeWrites @Inject() () extends Writes[IssueType, BacklogIssueType] with Logging {

  override def writes(issueType: IssueType): BacklogIssueType = {
    BacklogIssueType(
      optId = Some(issueType.getId),
      name = issueType.getName,
      issueType.getColor.getStrValue,
      delete = false
    )
  }

}
