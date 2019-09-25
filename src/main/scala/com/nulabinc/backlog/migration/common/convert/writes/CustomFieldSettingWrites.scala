package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.internal.json.customFields._
import com.nulabinc.backlog4j.{CustomFieldSetting, IssueType}

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class CustomFieldSettingWrites @Inject()(propertyValue: PropertyValue) extends Writes[CustomFieldSetting, BacklogCustomFieldSetting] with Logging {

  override def writes(customFieldSetting: CustomFieldSetting): BacklogCustomFieldSetting = {
    val backlogCustomFieldSetting =
      BacklogCustomFieldSetting(
        optId = Some(customFieldSetting.getId),
        rawName = customFieldSetting.getName,
        description = customFieldSetting.getDescription,
        typeId = customFieldSetting.getFieldTypeId,
        required = customFieldSetting.isRequired,
        applicableIssueTypes = toApplicableIssueTypes(customFieldSetting.getApplicableIssueTypes.toIndexedSeq, propertyValue.issueTypes),
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

  private[this] def toApplicableIssueTypes(applicableIssueTypeIds: Seq[Long], issueTypes: Seq[IssueType]): Seq[String] = {
    def findIssueType(applicableIssueTypeId: Long): Option[IssueType] = {
      issueTypes.find(_.getId == applicableIssueTypeId)
    }
    applicableIssueTypeIds.flatMap(findIssueType).map(_.getName)
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
            items = setting.getItems.asScala.toSeq.map(toBacklogItem),
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
            items = setting.getItems.asScala.toSeq.map(toBacklogItem),
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
            items = setting.getItems.asScala.toSeq.map(toBacklogItem),
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
            items = setting.getItems.asScala.toSeq.map(toBacklogItem),
            allowAddItem = setting.isAllowAddItem,
            allowInput = setting.isAllowInput
          ))
      case _ => throw new RuntimeException
    }

  private[this] def toBacklogItem(listItemSetting: ListItemSetting): BacklogItem =
    BacklogItem(optId = Some(listItemSetting.getId), name = listItemSetting.getName)

}
