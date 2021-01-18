package com.nulabinc.backlog.migration.common.domain

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.domain.support.{Identifier, Undefined}

/**
 * @author uchida
 */
class BacklogProjectKey(projectKey: String) extends Identifier[String] {

  def value = projectKey

}
object BacklogProjectKey {
  val undefined = new BacklogProjectKey("") with Undefined

  def apply(value: String): BacklogProjectKey = new BacklogProjectKey(value)
}

class BacklogTextFormattingRule(textFormattingRule: String) extends Identifier[String] {

  def value = textFormattingRule

}
object BacklogTextFormattingRule {
  val undefined = new BacklogTextFormattingRule("") with Undefined

  def apply(value: String): BacklogTextFormattingRule =
    new BacklogTextFormattingRule(value)
}

class BacklogProjectId(projectId: Long) extends Identifier[Long] {

  def value = projectId

}
object BacklogProjectId {
  val undefined = new BacklogProjectId(0) with Undefined

  def apply(value: Long): BacklogProjectId = new BacklogProjectId(value)
}

case class BacklogProject(
    optId: Option[Long],
    name: String,
    key: String,
    isChartEnabled: Boolean,
    isSubtaskingEnabled: Boolean,
    textFormattingRule: String
) {
  def id: Long =
    optId match {
      case Some(id) => id
      case _        => throw new RuntimeException("Project id is empty.")
    }
}

case class BacklogProjectWrapper(project: BacklogProject)

case class BacklogSharedFile(dir: String, name: String)

case class BacklogUser(
    optId: Option[Long],
    optUserId: Option[String],
    optPassword: Option[String],
    name: String,
    optMailAddress: Option[String],
    roleType: Int
) {
  def id: Long =
    optId match {
      case Some(id) => id
      case _        => throw new RuntimeException("User id is empty.")
    }
}

case class BacklogGroup(name: String, members: Seq[BacklogUser])

case class BacklogGroupsWrapper(groups: Seq[BacklogGroup])

case class BacklogProjectUsersWrapper(users: Seq[BacklogUser])

case class BacklogIssueType(
    optId: Option[Long],
    name: String,
    color: String,
    delete: Boolean
)

case class BacklogIssueTypesWrapper(issueTypes: Seq[BacklogIssueType])

case class BacklogIssueCategory(
    optId: Option[Long],
    name: String,
    delete: Boolean
)

case class BacklogIssueCategoriesWrapper(
    issueCategories: Seq[BacklogIssueCategory]
)

case class BacklogCustomField(
    name: String,
    fieldTypeId: Int,
    optValue: Option[String],
    values: Seq[String]
)

case class BacklogOperation(
    optCreatedUser: Option[BacklogUser],
    optCreated: Option[String],
    optUpdatedUser: Option[BacklogUser],
    optUpdated: Option[String]
)

case class BacklogNotification(
    optUser: Option[BacklogUser],
    optSenderUser: Option[BacklogUser]
)

case class BacklogIssueSummary(value: String, original: String)

trait BacklogEvent

case class BacklogIssue(
    eventType: String,
    id: Long,
    optIssueKey: Option[String],
    summary: BacklogIssueSummary,
    optParentIssueId: Option[Long],
    description: String,
    optStartDate: Option[String],
    optDueDate: Option[String],
    optEstimatedHours: Option[Float],
    optActualHours: Option[Float],
    optIssueTypeName: Option[String],
    status: BacklogStatus,
    categoryNames: Seq[String],
    versionNames: Seq[String],
    milestoneNames: Seq[String],
    priorityName: String,
    optAssignee: Option[BacklogUser],
    attachments: Seq[BacklogAttachment],
    sharedFiles: Seq[BacklogSharedFile],
    customFields: Seq[BacklogCustomField],
    notifiedUsers: Seq[BacklogUser],
    operation: BacklogOperation
) extends BacklogEvent {
  def findIssueIndex: Option[Int] = BacklogIssue.findIssueIndex(optIssueKey)
}

object BacklogIssue {
  def findIssueIndex(optIssueKey: Option[String]): Option[Int] = {
    import scala.util.matching.Regex

    optIssueKey.map { issueKey =>
      val pattern: Regex      = """^[0-9A-Z_]+-(\d+)$""".r
      val pattern(issueIndex) = issueKey
      issueIndex.toInt
    }
  }
}

case class BacklogComment(
    eventType: String,
    optIssueId: Option[Long],
    optContent: Option[String],
    changeLogs: Seq[BacklogChangeLog],
    notifications: Seq[BacklogNotification],
    optCreatedUser: Option[BacklogUser],
    optCreated: Option[String]
) extends BacklogEvent {

  def statusChangeLogs: Seq[BacklogChangeLog] =
    changeLogs.filter(_.field == BacklogConstantValue.ChangeLog.STATUS)
}

object BacklogComment {
  def statusComment(
      optIssueId: Option[Long],
      optOriginalValue: Option[BacklogStatusName],
      optNewValue: Option[BacklogStatusName],
      optCreatedUser: Option[BacklogUser],
      optCreated: Option[String]
  ): BacklogComment =
    BacklogComment(
      eventType = "comment",
      optIssueId = optIssueId,
      optContent = None,
      changeLogs = Seq(
        BacklogChangeLog(
          field = BacklogConstantValue.ChangeLog.STATUS,
          optOriginalValue = optOriginalValue.map(_.trimmed),
          optNewValue = optNewValue.map(_.trimmed),
          optAttachmentInfo = None,
          optAttributeInfo = None,
          optNotificationInfo = None,
          mustDeleteAttachment = false
        )
      ),
      notifications = Seq(),
      optCreatedUser = optCreatedUser,
      optCreated = optCreated
    )
}

case class BacklogAttachment(optId: Option[Long], name: String)

case class BacklogAttributeInfo(optId: Option[Long], typeId: String)

case class BacklogChangeLog(
    field: String,
    optOriginalValue: Option[String],
    optNewValue: Option[String],
    optAttachmentInfo: Option[BacklogAttachment],
    optAttributeInfo: Option[BacklogAttributeInfo],
    optNotificationInfo: Option[String],
    mustDeleteAttachment: Boolean = false
)

case class BacklogVersion(
    optId: Option[Long],
    name: String,
    description: String,
    optStartDate: Option[String],
    optReleaseDueDate: Option[String],
    delete: Boolean
)

case class BacklogVersionsWrapper(versions: Seq[BacklogVersion])

case class BacklogWiki(
    optId: Option[Long],
    name: String,
    optContent: Option[String],
    attachments: Seq[BacklogAttachment],
    sharedFiles: Seq[BacklogSharedFile],
    tags: Seq[BacklogWikiTag],
    optCreatedUser: Option[BacklogUser],
    optCreated: Option[String],
    optUpdatedUser: Option[BacklogUser],
    optUpdated: Option[String]
) {

  private val tagFormat: BacklogWikiTag => String = tag => s"[${tag.name}]"

  def id: Long =
    optId match {
      case Some(id) => id
      case _        => throw new RuntimeException("Wiki id is empty.")
    }

  def nameWithTags: BacklogWiki = {
    val head = if (tags.nonEmpty) tags.map(tagFormat).mkString + " " else ""
    this.copy(
      name = head + this.name
    )
  }

}

case class BacklogWikiTag(id: Long, name: String)

case class BacklogCustomFieldSetting(
    optId: Option[Long],
    private val rawName: String,
    description: String,
    typeId: Int,
    required: Boolean,
    applicableIssueTypes: Seq[String],
    delete: Boolean,
    property: BacklogCustomFieldProperty
) {
  val name: String = rawName.trim

//  def isSame(other: BacklogCustomFieldSetting): Boolean =
//    name == other.name &&
//    description == other.description &&
//    typeId == other.typeId &&
//    required == other.required &&
//    applicableIssueTypes.forall(str => other.applicableIssueTypes.contains(str))
}

case class BacklogCustomFieldSettings(
    settings: Seq[BacklogCustomFieldSetting]
) {

  def filterNotExist(
      items: Seq[BacklogCustomFieldSetting]
  ): Seq[BacklogCustomFieldSetting] =
    items.filterNot(item => settings.exists(_.name == item.name))
//    items.filterNot(item => settings.exists(_.isSame(item)))

//  def find(setting: BacklogCustomFieldSetting): Option[BacklogCustomFieldSetting] =
//    settings.find(_.isSame(setting))

  def findByName(name: String): Option[BacklogCustomFieldSetting] =
    settings.find(_.name == name.trim)

  def exists(name: String): Boolean =
    findByName(name).isDefined

}

object BacklogCustomFieldSettings {
  val empty: BacklogCustomFieldSettings = BacklogCustomFieldSettings(Seq())
}

trait BacklogCustomFieldProperty

case class BacklogCustomFieldTextProperty(typeId: Int) extends BacklogCustomFieldProperty

case class BacklogCustomFieldNumericProperty(
    typeId: Int,
    optInitialValue: Option[Float],
    optUnit: Option[String],
    optMin: Option[Float],
    optMax: Option[Float]
) extends BacklogCustomFieldProperty

case class BacklogCustomFieldDateProperty(
    typeId: Int,
    optInitialDate: Option[BacklogCustomFieldInitialDate],
    optMin: Option[String],
    optMax: Option[String]
) extends BacklogCustomFieldProperty

case class BacklogCustomFieldInitialDate(
    typeId: Long,
    optDate: Option[String],
    optShift: Option[Int]
)

case class BacklogCustomFieldMultipleProperty(
    typeId: Int,
    items: Seq[BacklogItem],
    allowAddItem: Boolean,
    allowInput: Boolean
) extends BacklogCustomFieldProperty

case class BacklogItem(optId: Option[Long], name: String)

case class BacklogSpace(spaceKey: String, name: String, created: String)

case class BacklogEnvironment(name: String, spaceId: Long)
