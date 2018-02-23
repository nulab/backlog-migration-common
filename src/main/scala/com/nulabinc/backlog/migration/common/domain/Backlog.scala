package com.nulabinc.backlog.migration.common.domain

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.domain.support.{Identifier, Undefined}
import com.nulabinc.backlog.migration.common.utils.DateUtil
import com.nulabinc.backlog4j.CustomField.FieldType
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.json.{DefaultJsonProtocol, JsNumber, JsValue, RootJsonFormat, _}

import scala.math.BigDecimal

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

class BacklogProjectId(projectId: Long) extends Identifier[Long] {

  def value = projectId

}
object BacklogProjectId {
  val undefined = new BacklogProjectId(0) with Undefined

  def apply(value: Long): BacklogProjectId = new BacklogProjectId(value)
}

case class BacklogProject(optId: Option[Long],
                          name: String,
                          key: String,
                          isChartEnabled: Boolean,
                          isSubtaskingEnabled: Boolean,
                          textFormattingRule: String) {
  def id: Long =
    optId match {
      case Some(id) => id
      case _        => throw new RuntimeException("Project id is empty.")
    }
}

case class BacklogProjectWrapper(project: BacklogProject)

case class BacklogSharedFile(dir: String, name: String)

case class BacklogUser(optId: Option[Long],
                       optUserId: Option[String],
                       optPassword: Option[String],
                       name: String,
                       optMailAddress: Option[String],
                       roleType: Int) {
  def id: Long =
    optId match {
      case Some(id) => id
      case _        => throw new RuntimeException("User id is empty.")
    }
}

case class BacklogGroup(name: String, members: Seq[BacklogUser])

case class BacklogGroupsWrapper(groups: Seq[BacklogGroup])

case class BacklogProjectUsersWrapper(users: Seq[BacklogUser])

case class BacklogIssueType(optId: Option[Long], name: String, color: String, delete: Boolean)

case class BacklogIssueTypesWrapper(issueTypes: Seq[BacklogIssueType])

case class BacklogIssueCategory(optId: Option[Long], name: String, delete: Boolean)

case class BacklogIssueCategoriesWrapper(issueCategories: Seq[BacklogIssueCategory])

case class BacklogCustomField(name: String, fieldTypeId: Int, optValue: Option[String], values: Seq[String])

case class BacklogOperation(optCreatedUser: Option[BacklogUser],
                            optCreated: Option[String],
                            optUpdatedUser: Option[BacklogUser],
                            optUpdated: Option[String])

case class BacklogNotification(optUser: Option[BacklogUser], optSenderUser: Option[BacklogUser])

case class BacklogIssueSummary(value: String, original: String)

trait BacklogEvent

case class BacklogIssue(eventType: String,
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
                        statusName: String,
                        categoryNames: Seq[String],
                        versionNames: Seq[String],
                        milestoneNames: Seq[String],
                        priorityName: String,
                        optAssignee: Option[BacklogUser],
                        attachments: Seq[BacklogAttachment],
                        sharedFiles: Seq[BacklogSharedFile],
                        customFields: Seq[BacklogCustomField],
                        notifiedUsers: Seq[BacklogUser],
                        operation: BacklogOperation)
    extends BacklogEvent {

  def offsetDays(offset: Int): BacklogIssue = {

    def parse(str: String, offset: Int): String = {
      val dateTime = DateTime.parse(str)
      DateUtil.dateFormat(dateTime.plusDays(offset).toDate)
    }

    def parseDateTime(str: String, offset: Int): String = {
      val format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
      val dateTime = DateTime.parse(str, format)
      DateUtil.isoFormat(dateTime.plusDays(offset).toDate)
    }

    this.copy(
      optStartDate = this.optStartDate.map(s => parse(s, offset)),
      optDueDate = this.optDueDate.map(s => parse(s, offset)),
      operation = this.operation.copy(
        optCreated = this.operation.optCreated.map(s => parseDateTime(s, offset)),
        optUpdated = this.operation.optUpdated.map(s => parseDateTime(s, offset))
      )
    )
  }

}

case class BacklogComment(eventType: String,
                          optIssueId: Option[Long],
                          optContent: Option[String],
                          changeLogs: Seq[BacklogChangeLog],
                          notifications: Seq[BacklogNotification],
                          optCreatedUser: Option[BacklogUser],
                          optCreated: Option[String])
    extends BacklogEvent {

  def offsetDays(offset: Int): BacklogComment = {

    def parse(str: String, offset: Int): String = {
      val format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
      val dateTime = DateTime.parse(str, format)
      DateUtil.isoFormat(dateTime.plusDays(offset).toDate)
    }

    this.copy(
      changeLogs = this.changeLogs.map(c => c.offsetDays(offset)),
      optCreated = this.optCreated.map(s => parse(s, offset))
    )
  }
}

case class BacklogAttachment(optId: Option[Long], name: String)

case class BacklogAttributeInfo(optId: Option[Long], typeId: String)

case class BacklogChangeLog(field: String,
                            optOriginalValue: Option[String],
                            optNewValue: Option[String],
                            optAttachmentInfo: Option[BacklogAttachment],
                            optAttributeInfo: Option[BacklogAttributeInfo],
                            optNotificationInfo: Option[String],
                            mustDeleteAttachment: Boolean = false) {

  def offsetDays(offset: Int): BacklogChangeLog = {

    def parse(str: String, offset: Int): String = {
      val dateTime = DateTime.parse(str)
      DateUtil.dateFormat(dateTime.plusDays(offset).toDate)
    }
    this.field match {
      case BacklogConstantValue.ChangeLog.START_DATE | BacklogConstantValue.ChangeLog.LIMIT_DATE =>
        this.copy(
          optOriginalValue = this.optOriginalValue.map(s => parse(s, offset)),
          optNewValue = this.optNewValue.map(s => parse(s, offset))
        )
      case _ => this
    }
  }
}

case class BacklogVersion(optId: Option[Long],
                          name: String,
                          description: String,
                          optStartDate: Option[String],
                          optReleaseDueDate: Option[String],
                          delete: Boolean)

case class BacklogVersionsWrapper(versions: Seq[BacklogVersion])

case class BacklogWiki(optId: Option[Long],
                       name: String,
                       optContent: Option[String],
                       attachments: Seq[BacklogAttachment],
                       sharedFiles: Seq[BacklogSharedFile],
                       optCreatedUser: Option[BacklogUser],
                       optCreated: Option[String],
                       optUpdatedUser: Option[BacklogUser],
                       optUpdated: Option[String]) {
  def id: Long =
    optId match {
      case Some(id) => id
      case _        => throw new RuntimeException("Wiki id is empty.")
    }
}

case class BacklogCustomFieldSetting(optId: Option[Long],
                                     name: String,
                                     description: String,
                                     typeId: Int,
                                     required: Boolean,
                                     applicableIssueTypes: Seq[String],
                                     delete: Boolean,
                                     property: BacklogCustomFieldProperty)

trait BacklogCustomFieldProperty

case class BacklogCustomFieldTextProperty(typeId: Int) extends BacklogCustomFieldProperty

case class BacklogCustomFieldNumericProperty(typeId: Int,
                                             optInitialValue: Option[Float],
                                             optUnit: Option[String],
                                             optMin: Option[Float],
                                             optMax: Option[Float])
    extends BacklogCustomFieldProperty

case class BacklogCustomFieldDateProperty(
    typeId: Int,
    optInitialDate: Option[BacklogCustomFieldInitialDate],
    optMin: Option[String],
    optMax: Option[String]
) extends BacklogCustomFieldProperty

case class BacklogCustomFieldInitialDate(typeId: Long, optDate: Option[String], optShift: Option[Int])

case class BacklogCustomFieldMultipleProperty(typeId: Int, items: Seq[BacklogItem], allowAddItem: Boolean, allowInput: Boolean)
    extends BacklogCustomFieldProperty

case class BacklogItem(optId: Option[Long], name: String)

case class BacklogCustomFieldSettingsWrapper(backlogCustomFieldSettings: Seq[BacklogCustomFieldSetting])

case class BacklogSpace(spaceKey: String, name: String, created: String)

case class BacklogEnvironment(name: String, spaceId: Long)

object BacklogJsonProtocol extends DefaultJsonProtocol {

  implicit object BacklogEventObjectFormat extends RootJsonFormat[BacklogEvent] {
    def write(eventObject: BacklogEvent) =
      eventObject match {
        case issue: BacklogIssue     => issue.toJson
        case comment: BacklogComment => comment.toJson
      }

    def read(value: JsValue): BacklogEvent = {
      value.asJsObject.fields("eventType") match {
        case JsString("issue")   => value.convertTo[BacklogIssue]
        case JsString("comment") => value.convertTo[BacklogComment]
        case _                   => throw new RuntimeException
      }
    }
  }

  implicit val BacklogItemFormat = jsonFormat2(BacklogItem)

  implicit object BacklogCustomFieldPropertyFormat extends RootJsonFormat[BacklogCustomFieldProperty] {
    def write(customFieldProperty: BacklogCustomFieldProperty) =
      customFieldProperty match {
        case property: BacklogCustomFieldTextProperty     => property.toJson
        case property: BacklogCustomFieldNumericProperty  => property.toJson
        case property: BacklogCustomFieldDateProperty     => property.toJson
        case property: BacklogCustomFieldMultipleProperty => property.toJson
      }

    def read(value: JsValue): BacklogCustomFieldProperty = {
      val Text         = BigDecimal(FieldType.Text.getIntValue)
      val TextArea     = BigDecimal(FieldType.TextArea.getIntValue)
      val Numeric      = BigDecimal(FieldType.Numeric.getIntValue)
      val Date         = BigDecimal(FieldType.Date.getIntValue)
      val SingleList   = BigDecimal(FieldType.SingleList.getIntValue)
      val MultipleList = BigDecimal(FieldType.MultipleList.getIntValue)
      val CheckBox     = BigDecimal(FieldType.CheckBox.getIntValue)
      val Radio        = BigDecimal(FieldType.Radio.getIntValue)
      value.asJsObject.fields("typeId") match {
        case JsNumber(Text)         => value.convertTo[BacklogCustomFieldTextProperty]
        case JsNumber(TextArea)     => value.convertTo[BacklogCustomFieldTextProperty]
        case JsNumber(Numeric)      => value.convertTo[BacklogCustomFieldNumericProperty]
        case JsNumber(Date)         => value.convertTo[BacklogCustomFieldDateProperty]
        case JsNumber(SingleList)   => value.convertTo[BacklogCustomFieldMultipleProperty]
        case JsNumber(MultipleList) => value.convertTo[BacklogCustomFieldMultipleProperty]
        case JsNumber(CheckBox)     => value.convertTo[BacklogCustomFieldMultipleProperty]
        case JsNumber(Radio)        => value.convertTo[BacklogCustomFieldMultipleProperty]
        case _                      => throw new RuntimeException
      }
    }
  }
  implicit val BacklogUserFormat                        = jsonFormat6(BacklogUser)
  implicit val BacklogNotificationFormat                = jsonFormat2(BacklogNotification)
  implicit val BacklogOperationFormat                   = jsonFormat4(BacklogOperation)
  implicit val BacklogAttachmentFormat                  = jsonFormat2(BacklogAttachment)
  implicit val BacklogProjectFormat                     = jsonFormat6(BacklogProject)
  implicit val BacklogProjectWrapperFormat              = jsonFormat1(BacklogProjectWrapper)
  implicit val BacklogSharedFileFormat                  = jsonFormat2(BacklogSharedFile)
  implicit val BacklogGroupFormat                       = jsonFormat2(BacklogGroup)
  implicit val BacklogGroupsWrapperFormat               = jsonFormat1(BacklogGroupsWrapper)
  implicit val BacklogProjectUsersWrapperFormat         = jsonFormat1(BacklogProjectUsersWrapper)
  implicit val BacklogIssueTypeFormat                   = jsonFormat4(BacklogIssueType)
  implicit val BacklogIssueTypesWrapperFormat           = jsonFormat1(BacklogIssueTypesWrapper)
  implicit val BacklogIssueCategoryWrapperFormat        = jsonFormat3(BacklogIssueCategory)
  implicit val BacklogIssueCategoriesWrapperFormat      = jsonFormat1(BacklogIssueCategoriesWrapper)
  implicit val BacklogCustomFieldFormat                 = jsonFormat4(BacklogCustomField)
  implicit val BacklogAttributeInfoFormat               = jsonFormat2(BacklogAttributeInfo)
  implicit val BacklogChangeLogFormat                   = jsonFormat7(BacklogChangeLog)
  implicit val BacklogCommentFormat                     = jsonFormat7(BacklogComment)
  implicit val BacklogIssueSummaryFormat                = jsonFormat2(BacklogIssueSummary)
  implicit val BacklogIssueFormat                       = jsonFormat22(BacklogIssue)
  implicit val BacklogWikiFormat                        = jsonFormat9(BacklogWiki)
  implicit val BacklogCustomFieldInitialDateFormat      = jsonFormat3(BacklogCustomFieldInitialDate)
  implicit val BacklogCustomFieldTextPropertyFormat     = jsonFormat1(BacklogCustomFieldTextProperty)
  implicit val BacklogCustomFieldNumericPropertyFormat  = jsonFormat5(BacklogCustomFieldNumericProperty)
  implicit val BacklogCustomFieldDatePropertyFormat     = jsonFormat4(BacklogCustomFieldDateProperty)
  implicit val BacklogCustomFieldMultiplePropertyFormat = jsonFormat4(BacklogCustomFieldMultipleProperty)
  implicit val BacklogCustomFieldSettingFormat          = jsonFormat8(BacklogCustomFieldSetting)
  implicit val BacklogCustomFieldSettingsWrapperFormat  = jsonFormat1(BacklogCustomFieldSettingsWrapper)
  implicit val BacklogVersionFormat                     = jsonFormat6(BacklogVersion)
  implicit val BacklogVersionsWrapperFormat             = jsonFormat1(BacklogVersionsWrapper)
  implicit val BacklogSpaceFormat                       = jsonFormat3(BacklogSpace)
  implicit val BacklogEnvironmentFormat                 = jsonFormat2(BacklogEnvironment)

}
