package com.nulabinc.backlog.migration.convert

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.domain.BacklogJsonProtocol._
import com.nulabinc.backlog.migration.domain.{BacklogCustomFieldSetting, _}
import com.nulabinc.backlog.migration.utils.{DateUtil, FileUtil, Logging}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.internal.json.customFields._
import com.nulabinc.backlog4j.{Notification, _}
import spray.json._

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
object Backlog4jConverters extends Logging {

  object Comment {
    def apply(comment: IssueComment): BacklogComment = {
      BacklogComment(
        eventType = "comment",
        optIssueId = None,
        optContent = Option(comment.getContent),
        changeLogs = comment.getChangeLog.asScala.map(toBacklogChangeLog),
        notifications = comment.getNotifications.asScala.map(toBacklogNotification),
        isCreateIssue = false,
        optCreatedUser = Option(comment.getCreatedUser).map(User.apply),
        optCreated = Option(comment.getCreated).map(DateUtil.isoFormat)
      )
    }

    private[this] def toBacklogNotification(notification: Notification): BacklogNotification = {
      BacklogNotification(optUser = Option(notification.getUser).map(User.apply), optSenderUser = Option(notification.getSender).map(User.apply))
    }

    private[this] def toBacklogChangeLog(changeLog: ChangeLog): BacklogChangeLog =
      BacklogChangeLog(
        field = changeLog.getField,
        optOriginalValue = Option(changeLog.getOriginalValue).map(DateUtil.formatIfNeeded),
        optNewValue = Option(changeLog.getNewValue).map(DateUtil.formatIfNeeded),
        optAttachmentInfo = Option(changeLog.getAttachmentInfo).map(toBacklogAttachmentInfo),
        optAttributeInfo = Option(changeLog.getAttributeInfo).map(toBacklogAttributeInfo),
        optNotificationInfo = Option(changeLog.getNotificationInfo).map(_.getType)
      )

    private[this] def toBacklogAttributeInfo(attributeInfo: AttributeInfo): BacklogAttributeInfo =
      BacklogAttributeInfo(optId = Option(attributeInfo).map(_.getId), typeId = attributeInfo.getTypeId)

    private[this] def toBacklogAttachmentInfo(attachmentInfo: AttachmentInfo): BacklogAttachment =
      BacklogAttachment(optId = Option(attachmentInfo).map(_.getId), name = FileUtil.normalize(attachmentInfo.getName))
  }

  object Project {
    def apply(project: Project): BacklogProject =
      BacklogProject(
        optId = Some(project.getId),
        name = project.getName,
        key = project.getProjectKey,
        isChartEnabled = project.isChartEnabled,
        isSubtaskingEnabled = project.isSubtaskingEnabled,
        textFormattingRule = project.getTextFormattingRule.getStrValue
      )
  }

  object Group {
    def apply(group: Group): BacklogGroup = {
      BacklogGroup(group.getName, group.getMembers.asScala.map(User.apply))
    }
  }

  object Groups {
    def apply(groups: Seq[Group]): String = {
      val backlogGroups: Seq[BacklogGroup] = groups.map(apply)
      BacklogGroupsWrapper(backlogGroups).toJson.prettyPrint
    }

    def apply(group: Group): BacklogGroup =
      BacklogGroup(group.getName, group.getMembers.asScala.map(User.apply))
  }

  object User {
    def apply(user: User): BacklogUser = {
      BacklogUser(
        optId = Some(user.getId),
        optUserId = Option(user.getUserId),
        optPassword = None,
        name = user.getName,
        optMailAddress = Option(user.getMailAddress),
        roleType = user.getRoleType.getIntValue
      )
    }

    def apply(name: String): BacklogUser = {
      BacklogUser(None, None, None, name, None, BacklogConstantValue.USER_ROLE)
    }
  }

  object Version {
    def apply(version: Version): BacklogVersion =
      BacklogVersion(
        optId = Some(version.getId),
        name = version.getName,
        description = Option(version.getDescription).getOrElse(""),
        optStartDate = Option(version.getStartDate).map(DateUtil.dateFormat),
        optReleaseDueDate = Option(version.getReleaseDueDate).map(DateUtil.dateFormat),
        delete = false
      )

    def apply(name: String): BacklogVersion =
      BacklogVersion(
        optId = None,
        name = name,
        description = "",
        optStartDate = None,
        optReleaseDueDate = None,
        delete = true
      )
  }

  object Category {
    def apply(category: Category): BacklogIssueCategory =
      BacklogIssueCategory(optId = Some(category.getId), name = category.getName, delete = false)

    def apply(name: String): BacklogIssueCategory =
      BacklogIssueCategory(optId = None, name = name, delete = true)
  }

  object IssueType {
    def apply(issueType: IssueType): BacklogIssueType =
      BacklogIssueType(optId = Some(issueType.getId), name = issueType.getName, issueType.getColor.getStrValue, delete = false)

    def apply(name: String) =
      BacklogIssueType(optId = None, name, BacklogConstantValue.ISSUE_TYPE_COLOR.getStrValue, delete = true)
  }

  object CustomFieldSetting {

    def apply(customFieldSetting: CustomFieldSetting)(issueTypes: Seq[BacklogIssueType]): BacklogCustomFieldSetting = {
      val backlogCustomFieldSetting =
        BacklogCustomFieldSetting(
          optId = Some(customFieldSetting.getId),
          name = customFieldSetting.getName,
          description = customFieldSetting.getDescription,
          typeId = customFieldSetting.getFieldTypeId,
          required = customFieldSetting.isRequired,
          applicableIssueTypes = toApplicableIssueTypes(customFieldSetting.getApplicableIssueTypes, issueTypes),
          property = BacklogCustomFieldTextProperty(customFieldSetting.getFieldTypeId),
          delete = false
        )
      customFieldSetting.getFieldType match {
        case FieldType.Text =>
          getBacklogCustomFieldTextSetting(backlogCustomFieldSetting, customFieldSetting)
        case FieldType.TextArea =>
          getBacklogCustomFieldTextAreaSetting(backlogCustomFieldSetting, customFieldSetting)
        case FieldType.Numeric =>
          getBacklogCustomFieldNumericSetting(backlogCustomFieldSetting, customFieldSetting)
        case FieldType.Date =>
          getBacklogCustomFieldDateSetting(backlogCustomFieldSetting, customFieldSetting)
        case FieldType.SingleList =>
          getBacklogCustomFieldSingleListSetting(backlogCustomFieldSetting, customFieldSetting)
        case FieldType.MultipleList =>
          getBacklogCustomFieldMultipleListSetting(backlogCustomFieldSetting, customFieldSetting)
        case FieldType.CheckBox =>
          getBacklogCustomFieldCheckBoxSetting(backlogCustomFieldSetting, customFieldSetting)
        case FieldType.Radio =>
          getBacklogCustomFieldRadioSetting(backlogCustomFieldSetting, customFieldSetting)
      }
    }

    def apply(name: String) = {
      BacklogCustomFieldSetting(
        optId = None,
        name = name,
        description = "",
        typeId = FieldType.Text.getIntValue,
        required = false,
        applicableIssueTypes = Seq.empty[String],
        property = BacklogCustomFieldTextProperty(FieldType.Text.getIntValue),
        delete = true
      )
    }

    private[this] def toApplicableIssueTypes(applicableIssueTypeIds: Seq[Long], issueTypes: Seq[BacklogIssueType]): Seq[String] = {
      def findIssueType(applicableIssueTypeId: Long): Option[BacklogIssueType] = {
        issueTypes.find(issueType =>
          issueType.optId match {
            case Some(id) => applicableIssueTypeId == id
            case _        => false
        })
      }

      applicableIssueTypeIds.flatMap(findIssueType).map(_.name)
    }

    private[this] def getBacklogCustomFieldTextSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                       customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: TextCustomFieldSetting =>
          backlogCustomFieldSetting.copy(property = BacklogCustomFieldTextProperty(setting.getFieldTypeId))
        case _ => throw new RuntimeException
      }

    private[this] def getBacklogCustomFieldTextAreaSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                           customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: TextAreaCustomFieldSetting =>
          backlogCustomFieldSetting.copy(property = BacklogCustomFieldTextProperty(setting.getFieldTypeId))
        case _ => throw new RuntimeException
      }

    private[this] def getBacklogCustomFieldNumericSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                          customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: NumericCustomFieldSetting =>
          backlogCustomFieldSetting.copy(
            property = BacklogCustomFieldNumericProperty(
              setting.getFieldTypeId,
              optInitialValue = Option(setting.getInitialValue).map(_.floatValue()),
              optUnit = Option(setting.getUnit),
              optMin = Option(setting.getMin).map(_.floatValue()),
              optMax = Option(setting.getMax).map(_.floatValue())
            ))
        case _ => throw new RuntimeException
      }

    private[this] def getBacklogCustomFieldDateSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                       customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: DateCustomFieldSetting =>
          val initialDate = Option(setting.getInitialDate).map { initialDate =>
            BacklogCustomFieldInitialDate(
              typeId = initialDate.getId,
              optDate = Option(initialDate.getDate).map(DateUtil.dateFormat),
              optShift =
                if (setting.getInitialDate.getShift == 0) None
                else Some(initialDate.getShift)
            )
          }
          backlogCustomFieldSetting.copy(
            property = BacklogCustomFieldDateProperty(
              setting.getFieldTypeId,
              optInitialDate = initialDate,
              optMin = Option(setting.getMin).map(DateUtil.dateFormat),
              optMax = Option(setting.getMax).map(DateUtil.dateFormat)
            ))
        case _ => throw new RuntimeException
      }

    private[this] def getBacklogCustomFieldSingleListSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                             customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: SingleListCustomFieldSetting =>
          backlogCustomFieldSetting.copy(
            property = BacklogCustomFieldMultipleProperty(
              typeId = setting.getFieldTypeId,
              items = setting.getItems.asScala.map(toBacklogItem),
              allowAddItem = setting.isAllowAddItem,
              allowInput = false
            ))
        case _ => throw new RuntimeException
      }

    private[this] def getBacklogCustomFieldMultipleListSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                               customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: MultipleListCustomFieldSetting =>
          backlogCustomFieldSetting.copy(
            property = BacklogCustomFieldMultipleProperty(
              typeId = setting.getFieldTypeId,
              items = setting.getItems.asScala.map(toBacklogItem),
              allowAddItem = setting.isAllowAddItem,
              allowInput = false
            ))
        case _ => throw new RuntimeException
      }

    private[this] def getBacklogCustomFieldCheckBoxSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                           customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: CheckBoxCustomFieldSetting =>
          backlogCustomFieldSetting.copy(
            property = BacklogCustomFieldMultipleProperty(
              typeId = setting.getFieldTypeId,
              items = setting.getItems.asScala.map(toBacklogItem),
              allowAddItem = setting.isAllowAddItem,
              allowInput = setting.isAllowInput
            ))
        case _ => throw new RuntimeException
      }

    private[this] def getBacklogCustomFieldRadioSetting(backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                        customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting =
      customFieldSetting match {
        case setting: RadioCustomFieldSetting =>
          backlogCustomFieldSetting.copy(
            property = BacklogCustomFieldMultipleProperty(
              typeId = setting.getFieldTypeId,
              items = setting.getItems.asScala.map(toBacklogItem),
              allowAddItem = setting.isAllowAddItem,
              allowInput = setting.isAllowInput
            ))
        case _ => throw new RuntimeException
      }

    private[this] def toBacklogItem(listItemSetting: ListItemSetting): BacklogItem =
      BacklogItem(optId = Some(listItemSetting.getId), name = listItemSetting.getName)
  }

  object Attachment {
    def apply(attachment: Attachment): BacklogAttachment =
      BacklogAttachment(
        optId = Some(attachment.getId),
        name = FileUtil.normalize(attachment.getName)
      )
  }

  object Space {
    def apply(space: Space): BacklogSpace = {
      BacklogSpace(
        spaceKey = space.getSpaceKey,
        name = space.getName,
        created = DateUtil.isoFormat(space.getCreated)
      )
    }
  }

  object Environment {
    def apply(environment: Environment): BacklogEnvironment = {
      BacklogEnvironment(
        name = environment.getName,
        spaceId = environment.getSpaceId
      )
    }
  }

}
