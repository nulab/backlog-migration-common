package com.nulabinc.backlog.migration.common.conf

import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.Project.IssueTypeColor
import com.nulabinc.backlog4j.User.RoleType

/**
 * @author
 *   uchida
 */
object BacklogConstantValue {

  val USER_PASSWORD: String = "password"
  val USER_ROLE: Int        = RoleType.User.getIntValue

  // Project Setting
  val ISSUE_TYPE_COLOR: IssueTypeColor = IssueTypeColor.Color1

  // Wiki Home Name
  val WIKI_HOME_NAME: String = "Home"

  object CustomField {
    val Text         = FieldType.Text.getIntValue
    val TextArea     = FieldType.TextArea.getIntValue
    val Numeric      = FieldType.Numeric.getIntValue
    val Date         = FieldType.Date.getIntValue
    val SingleList   = FieldType.SingleList.getIntValue
    val MultipleList = FieldType.MultipleList.getIntValue
    val CheckBox     = FieldType.CheckBox.getIntValue
    val Radio        = FieldType.Radio.getIntValue
  }

  object ChangeLog {
    val NOTIFICATIONINFO_TYPE_ISSUE_CREATE: String = "issue.create"

    val SUMMARY: String         = "summary"
    val DESCRIPTION: String     = "description"
    val COMPONENT: String       = "component"
    val VERSION: String         = "version"
    val MILESTONE: String       = "milestone"
    val STATUS: String          = "status"
    val ASSIGNER: String        = "assigner"
    val ISSUE_TYPE: String      = "issueType"
    val START_DATE: String      = "startDate"
    val LIMIT_DATE: String      = "limitDate"
    val PRIORITY: String        = "priority"
    val RESOLUTION: String      = "resolution"
    val ESTIMATED_HOURS: String = "estimatedHours"
    val ACTUAL_HOURS: String    = "actualHours"
    val PARENT_ISSUE: String    = "parentIssue"
    val NOTIFICATION: String    = "notification"
    val ATTACHMENT: String      = "attachment"
    val COMMIT: String          = "commit"
  }

}
