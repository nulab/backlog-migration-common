package com.nulabinc.backlog.migration.common.formatters

import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.domain.exports.{
  DeletedExportedBacklogStatus,
  ExistingExportedBacklogStatus,
  ExportedBacklogIssue,
  ExportedBacklogStatus
}
import com.nulabinc.backlog4j.CustomField.FieldType
import spray.json._

import scala.math.BigDecimal

object BacklogJsonProtocol extends DefaultJsonProtocol {

  // id
  class IdFormat[A]() extends RootJsonFormat[Id[A]] {
    override def read(json: JsValue): Id[A] = json match {
      case JsNumber(idVal) => Id(idVal.toLong)
      case _ =>
        throw DeserializationException(s"Expected a js number got ${json.prettyPrint}")
    }

    override def write(obj: Id[A]): JsValue = JsNumber(obj.value)
  }
  implicit val backlogStatusIdFormat: IdFormat[BacklogStatus] = new IdFormat[BacklogStatus]

  implicit val BacklogItemFormat           = jsonFormat2(BacklogItem)
  implicit val BacklogUserFormat           = jsonFormat6(BacklogUser)
  implicit val BacklogNotificationFormat   = jsonFormat2(BacklogNotification)
  implicit val BacklogOperationFormat      = jsonFormat4(BacklogOperation)
  implicit val BacklogAttachmentFormat     = jsonFormat2(BacklogAttachment)
  implicit val BacklogProjectFormat        = jsonFormat6(BacklogProject)
  implicit val BacklogProjectWrapperFormat = jsonFormat1(BacklogProjectWrapper)
  implicit val BacklogSharedFileFormat     = jsonFormat2(BacklogSharedFile)
  implicit val BacklogGroupFormat          = jsonFormat2(BacklogGroup)
  implicit val BacklogGroupsWrapperFormat  = jsonFormat1(BacklogGroupsWrapper)
  implicit val BacklogProjectUsersWrapperFormat = jsonFormat1(
    BacklogProjectUsersWrapper
  )
  implicit val BacklogIssueTypeFormat = jsonFormat4(BacklogIssueType)
  implicit val BacklogIssueTypesWrapperFormat = jsonFormat1(
    BacklogIssueTypesWrapper
  )
  implicit val BacklogIssueCategoryWrapperFormat = jsonFormat3(
    BacklogIssueCategory
  )
  implicit val BacklogIssueCategoriesWrapperFormat = jsonFormat1(
    BacklogIssueCategoriesWrapper
  )
  implicit val BacklogCustomFieldFormat   = jsonFormat4(BacklogCustomField)
  implicit val BacklogAttributeInfoFormat = jsonFormat2(BacklogAttributeInfo)
  implicit val BacklogChangeLogFormat     = jsonFormat7(BacklogChangeLog)
  implicit val BacklogCommentFormat       = jsonFormat7(BacklogComment.apply)
  implicit val BacklogIssueSummaryFormat  = jsonFormat2(BacklogIssueSummary)

  // Status
  implicit object BacklogStatusNameFormat extends JsonFormat[BacklogStatusName] {
    override def read(json: JsValue): BacklogStatusName =
      json match {
        case JsString(value) => BacklogStatusName(value)
        case other =>
          deserializationError(s"Invalid BacklogStatusName input: $other")
      }
    override def write(obj: BacklogStatusName): JsValue =
      JsString(obj.trimmed)
  }

  implicit val BacklogDefaultStatusFormat: RootJsonFormat[BacklogDefaultStatus] =
    jsonFormat(BacklogDefaultStatus.apply, "id", "name", "displayOrder")
  implicit val BacklogCustomStatusFormat: RootJsonFormat[BacklogCustomStatus] =
    jsonFormat(BacklogCustomStatus.apply, "id", "name", "displayOrder", "color")

  implicit object BacklogStatusFormat extends RootJsonFormat[BacklogStatus] {
    override def read(json: JsValue): BacklogStatus =
      json.asJsObject.getFields("type", "value") match {
        case Seq(JsString(statusType), status) =>
          statusType match {
            case "default" => status.convertTo[BacklogDefaultStatus]
            case "custom"  => status.convertTo[BacklogCustomStatus]
            case other =>
              deserializationError(s"Invalid custom status type. Input: $other")
          }
        case other =>
          deserializationError(
            s"Invalid BacklogStatus JSON format. Input: $other"
          )
      }

    override def write(obj: BacklogStatus): JsValue =
      obj match {
        case s: BacklogDefaultStatus =>
          JsObject("type" -> JsString("default"), "value" -> s.toJson)
        case s: BacklogCustomStatus =>
          JsObject("type" -> JsString("custom"), "value" -> s.toJson)
      }
  }

  // Issue
  implicit val BacklogIssueFormat = jsonFormat22(BacklogIssue.apply _)

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

  implicit val BacklogWikiTagFormat = jsonFormat2(BacklogWikiTag)
  implicit val BacklogCustomFieldInitialDateFormat = jsonFormat3(
    BacklogCustomFieldInitialDate
  )
  implicit val BacklogCustomFieldTextPropertyFormat = jsonFormat1(
    BacklogCustomFieldTextProperty
  )
  implicit val BacklogCustomFieldNumericPropertyFormat = jsonFormat5(
    BacklogCustomFieldNumericProperty
  )
  implicit val BacklogCustomFieldDatePropertyFormat = jsonFormat4(
    BacklogCustomFieldDateProperty
  )
  implicit val BacklogCustomFieldMultiplePropertyFormat = jsonFormat4(
    BacklogCustomFieldMultipleProperty
  )

  implicit object BacklogCustomFieldPropertyFormat
      extends RootJsonFormat[BacklogCustomFieldProperty] {
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
        case JsNumber(Text) => value.convertTo[BacklogCustomFieldTextProperty]
        case JsNumber(TextArea) =>
          value.convertTo[BacklogCustomFieldTextProperty]
        case JsNumber(Numeric) =>
          value.convertTo[BacklogCustomFieldNumericProperty]
        case JsNumber(Date) => value.convertTo[BacklogCustomFieldDateProperty]
        case JsNumber(SingleList) =>
          value.convertTo[BacklogCustomFieldMultipleProperty]
        case JsNumber(MultipleList) =>
          value.convertTo[BacklogCustomFieldMultipleProperty]
        case JsNumber(CheckBox) =>
          value.convertTo[BacklogCustomFieldMultipleProperty]
        case JsNumber(Radio) =>
          value.convertTo[BacklogCustomFieldMultipleProperty]
        case _ => throw new RuntimeException
      }
    }
  }

  implicit val BacklogCustomFieldSettingFormat: RootJsonFormat[BacklogCustomFieldSetting] =
    jsonFormat(
      BacklogCustomFieldSetting.apply,
      "id",
      "name",
      "description",
      "typeId",
      "required",
      "applicableIssueTypes",
      "delete",
      "property"
    )

  implicit val BacklogCustomFieldSettingsFormat: RootJsonFormat[BacklogCustomFieldSettings] =
    jsonFormat(BacklogCustomFieldSettings.apply, "backlogCustomFieldSettings")

  implicit val BacklogWikiFormat: RootJsonFormat[BacklogWiki] =
    jsonFormat(
      BacklogWiki.apply,
      "optId",
      "name",
      "optContent",
      "attachments",
      "sharedFiles",
      "tags",
      "optCreatedUser",
      "optCreated",
      "optUpdatedUser",
      "optUpdated"
    )

  implicit val BacklogVersionFormat = jsonFormat6(BacklogVersion)
  implicit val BacklogVersionsWrapperFormat = jsonFormat1(
    BacklogVersionsWrapper
  )
  implicit val BacklogSpaceFormat       = jsonFormat3(BacklogSpace)
  implicit val BacklogEnvironmentFormat = jsonFormat2(BacklogEnvironment)

  implicit val existingBacklogStatusFormat =
    jsonFormat(ExistingExportedBacklogStatus.apply _, "status")
  implicit val deletedBacklogStatusFormat =
    jsonFormat(DeletedExportedBacklogStatus.apply _, "status")

  // Export and Import
  implicit object ExportedBacklogStatusFormat extends RootJsonFormat[ExportedBacklogStatus] {
    override def read(json: JsValue): ExportedBacklogStatus =
      json.asJsObject.getFields("type", "value") match {
        case Seq(JsString(valueType), status) =>
          valueType match {
            case "existing" => status.convertTo[ExistingExportedBacklogStatus]
            case "deleted"  => status.convertTo[DeletedExportedBacklogStatus]
            case other =>
              deserializationError(s"Invalid status type. input: $other")
          }
        case other =>
          deserializationError(s"Invalid status json. input: $other")
      }

    override def write(obj: ExportedBacklogStatus): JsValue =
      obj match {
        case s: ExistingExportedBacklogStatus =>
          JsObject(
            "type"  -> JsString("existing"),
            "value" -> s.toJson
          )
        case s: DeletedExportedBacklogStatus =>
          JsObject(
            "type"  -> JsString("deleted"),
            "value" -> s.toJson
          )
      }
  }

  implicit val exportedBacklogIssueFormat = jsonFormat21(ExportedBacklogIssue)
}
