package com.nulabinc.backlog.migration.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.convert.Convert
import com.nulabinc.backlog.migration.convert.writes.CustomFieldSettingWrites
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.api.option._
import com.nulabinc.backlog4j.internal.json.customFields._
import com.nulabinc.backlog4j.{BacklogAPIException, BacklogClient}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class CustomFieldSettingServiceImpl @Inject()(implicit val customFieldSettingWrites: CustomFieldSettingWrites,
                                              @Named("projectKey") projectKey: String,
                                              backlog: BacklogClient)
    extends CustomFieldSettingService
    with Logging {

  override def allCustomFieldSettings(): Seq[BacklogCustomFieldSetting] =
    try {
      backlog.getCustomFields(projectKey).asScala.map(Convert.toBacklog(_))
    } catch {
      case api: BacklogAPIException if (api.getMessage.contains("current plan is not customField available.")) =>
        logger.warn(api.getMessage, api)
        Seq.empty[BacklogCustomFieldSetting]
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[BacklogCustomFieldSetting]
    }

  override def remove(customFieldSettingId: Long) = {
    backlog.removeCustomField(projectKey, customFieldSettingId)
  }

  override def add(setAddParams: BacklogCustomFieldSetting => AddCustomFieldParams)(backlogCustomFieldSetting: BacklogCustomFieldSetting) = {
    addCustomFieldSetting(setAddParams(backlogCustomFieldSetting))
  }

  override def setAddParams(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddCustomFieldParams =
    backlogCustomFieldSetting.typeId match {
      case BacklogConstantValue.CustomField.Text         => addTextCustomField(backlogCustomFieldSetting)
      case BacklogConstantValue.CustomField.TextArea     => addTextAreaCustomField(backlogCustomFieldSetting)
      case BacklogConstantValue.CustomField.Numeric      => addNumericCustomField(backlogCustomFieldSetting)
      case BacklogConstantValue.CustomField.Date         => addDateCustomField(backlogCustomFieldSetting)
      case BacklogConstantValue.CustomField.SingleList   => addSingleListCustomField(backlogCustomFieldSetting)
      case BacklogConstantValue.CustomField.MultipleList => addMultipleListCustomField(backlogCustomFieldSetting)
      case BacklogConstantValue.CustomField.CheckBox     => addCheckBoxCustomField(backlogCustomFieldSetting)
      case BacklogConstantValue.CustomField.Radio        => addRadioCustomField(backlogCustomFieldSetting)
    }

  private[this] def addCustomFieldSetting(params: AddCustomFieldParams) = {
    try {
      params match {
        case textParams: AddTextCustomFieldParams                 => Some(backlog.addTextCustomField(textParams))
        case textAreaParams: AddTextAreaCustomFieldParams         => Some(backlog.addTextAreaCustomField(textAreaParams))
        case numericParams: AddNumericCustomFieldParams           => Some(backlog.addNumericCustomField(numericParams))
        case dateParams: AddDateCustomFieldParams                 => Some(backlog.addDateCustomField(dateParams))
        case singleListParams: AddSingleListCustomFieldParams     => Some(backlog.addSingleListCustomField(singleListParams))
        case multipleListParams: AddMultipleListCustomFieldParams => Some(backlog.addMultipleListCustomField(multipleListParams))
        case checkboxParams: AddCheckBoxCustomFieldParams         => Some(backlog.addCheckBoxCustomField(checkboxParams))
        case radioParams: AddRadioCustomFieldParams               => Some(backlog.addRadioCustomField(radioParams))
      }
    } catch {
      case api: BacklogAPIException if (api.getMessage.contains("current plan is not customField available.")) =>
        logger.warn(api.getMessage, api)
        None
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
  }

  private[this] def addTextCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddTextCustomFieldParams = {
    val params = new AddTextCustomFieldParams(projectKey, backlogCustomFieldSetting.name)
    params.description(backlogCustomFieldSetting.description)
    params
  }

  private[this] def addTextAreaCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddTextAreaCustomFieldParams = {
    val params = new AddTextAreaCustomFieldParams(projectKey, backlogCustomFieldSetting.name)
    params.description(backlogCustomFieldSetting.description)
    params
  }

  private[this] def addNumericCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddNumericCustomFieldParams = {
    val params = new AddNumericCustomFieldParams(projectKey, backlogCustomFieldSetting.name)
    params.description(backlogCustomFieldSetting.description)
    params
  }

  private[this] def addDateCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddDateCustomFieldParams = {
    val params = new AddDateCustomFieldParams(projectKey, backlogCustomFieldSetting.name)
    params.description(backlogCustomFieldSetting.description)
    params
  }

  private[this] def addSingleListCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddSingleListCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params = new AddSingleListCustomFieldParams(
          projectKey,
          backlogCustomFieldSetting.name
        )
        params.description(backlogCustomFieldSetting.description)
        params.items(property.items.map(_.name).asJava)
        params
      case _ => throw new RuntimeException()
    }

  private[this] def addMultipleListCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddMultipleListCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params = new AddMultipleListCustomFieldParams(
          projectKey,
          backlogCustomFieldSetting.name
        )
        params.description(backlogCustomFieldSetting.description)
        params.items(property.items.map(_.name).asJava)
        params
      case _ => throw new RuntimeException()
    }

  private[this] def addRadioCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddRadioCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params: AddRadioCustomFieldParams =
          new AddRadioCustomFieldParams(projectKey, backlogCustomFieldSetting.name)
        params.description(backlogCustomFieldSetting.description)
        params.items(property.items.map(_.name).asJava)
        params
      case _ => throw new RuntimeException()
    }

  private[this] def addCheckBoxCustomField(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddCheckBoxCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params = new AddCheckBoxCustomFieldParams(
          projectKey,
          backlogCustomFieldSetting.name
        )
        params.description(backlogCustomFieldSetting.description)
        params.items(property.items.map(_.name).asJava)
        params
      case _ => throw new RuntimeException()
    }

  override def setUpdateParams(propertyResolver: PropertyResolver)(
      backlogCustomFieldSetting: BacklogCustomFieldSetting): Option[UpdateCustomFieldParams] = {
    for {
      customFieldSetting   <- propertyResolver.optResolvedCustomFieldSetting(backlogCustomFieldSetting.name)
      customFieldSettingId <- customFieldSetting.optId
    } yield {
      backlogCustomFieldSetting.typeId match {
        case BacklogConstantValue.CustomField.Text => updateTextCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case BacklogConstantValue.CustomField.TextArea =>
          updateTextAreaCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case BacklogConstantValue.CustomField.Numeric => updateNumericCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case BacklogConstantValue.CustomField.Date    => updateDateCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case BacklogConstantValue.CustomField.SingleList =>
          updateSingleListCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case BacklogConstantValue.CustomField.MultipleList =>
          updateMultipleListCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case BacklogConstantValue.CustomField.CheckBox =>
          updateCheckBoxCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case BacklogConstantValue.CustomField.Radio => updateRadioCustomField(customFieldSettingId, backlogCustomFieldSetting, propertyResolver)
        case _                                      => throw new RuntimeException()
      }
    }
  }

  override def update(setUpdateParams: BacklogCustomFieldSetting => Option[UpdateCustomFieldParams])(
      backlogCustomFieldSetting: BacklogCustomFieldSetting) = {
    updateCustomFieldSetting(setUpdateParams(backlogCustomFieldSetting))
  }

  private[this] def updateCustomFieldSetting(optParams: Option[UpdateCustomFieldParams]) = {
    try {
      optParams match {
        case Some(textParams: UpdateTextCustomFieldParams)                 => Some(backlog.updateTextCustomField(textParams))
        case Some(textAreaParams: UpdateTextAreaCustomFieldParams)         => Some(backlog.updateTextAreaCustomField(textAreaParams))
        case Some(numericParams: UpdateNumericCustomFieldParams)           => Some(backlog.updateNumericCustomField(numericParams))
        case Some(dateParams: UpdateDateCustomFieldParams)                 => Some(backlog.updateDateCustomField(dateParams))
        case Some(singleListParams: UpdateSingleListCustomFieldParams)     => Some(backlog.updateSingleListCustomField(singleListParams))
        case Some(multipleListParams: UpdateMultipleListCustomFieldParams) => Some(backlog.updateMultipleListCustomField(multipleListParams))
        case Some(checkboxParams: UpdateCheckBoxCustomFieldParams)         => Some(backlog.updateCheckBoxCustomField(checkboxParams))
        case Some(radioParams: UpdateRadioCustomFieldParams)               => Some(backlog.updateRadioCustomField(radioParams))
        case _                                                             => None
      }
    } catch {
      case api: BacklogAPIException if (api.getMessage.contains("current plan is not customField available.")) =>
        logger.warn(api.getMessage, api)
        None
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
  }

  private[this] def updateTextCustomField(customFiledId: Long,
                                          backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                          propertyResolver: PropertyResolver): UpdateTextCustomFieldParams = {
    val params = new UpdateTextCustomFieldParams(projectKey, customFiledId)
    setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
    params
  }

  private[this] def updateTextAreaCustomField(customFiledId: Long,
                                              backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                              propertyResolver: PropertyResolver): UpdateTextAreaCustomFieldParams = {
    val params = new UpdateTextAreaCustomFieldParams(projectKey, customFiledId)
    setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
    params
  }

  private[this] def updateNumericCustomField(customFiledId: Long,
                                             backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                             propertyResolver: PropertyResolver): UpdateNumericCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldNumericProperty =>
        val params = new UpdateNumericCustomFieldParams(projectKey, customFiledId)
        setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
        for { min <- property.optMin } yield {
          params.min(min)
        }
        for { max <- property.optMax } yield {
          params.max(max)
        }
        for { initialValue <- property.optInitialValue } yield {
          params.initialValue(initialValue)
        }
        for { unit <- property.optUnit } yield {
          params.unit(unit)
        }
        params
      case _ => throw new RuntimeException()
    }

  private[this] def updateDateCustomField(customFiledId: Long,
                                          backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                          propertyResolver: PropertyResolver): UpdateDateCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldDateProperty =>
        val params = new UpdateDateCustomFieldParams(projectKey, customFiledId)
        setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
        params.initialValueType(DateCustomFieldSetting.InitialValueType.Today)
        for { min <- property.optMin } yield {
          params.min(min)
        }
        for { max <- property.optMax } yield {
          params.max(max)
        }
        for { initialDate <- property.optInitialDate } yield {
          val Today = DateCustomFieldSetting.InitialValueType.Today.getIntValue
          val TodayPlusShiftDays =
            DateCustomFieldSetting.InitialValueType.TodayPlusShiftDays.getIntValue
          val FixedDate =
            DateCustomFieldSetting.InitialValueType.FixedDate.getIntValue
          initialDate.typeId match {
            case Today =>
              params.initialValueType(
                DateCustomFieldSetting.InitialValueType.Today
              )
            case TodayPlusShiftDays =>
              params.initialValueType(
                DateCustomFieldSetting.InitialValueType.TodayPlusShiftDays
              )
              for { shift <- initialDate.optShift } yield {
                params.initialShift(shift)
              }
            case FixedDate =>
              params.initialValueType(
                DateCustomFieldSetting.InitialValueType.FixedDate
              )
              for { date <- initialDate.optDate } yield {
                params.initialDate(date)
              }
          }
        }
        params
      case _ => throw new RuntimeException()
    }

  private[this] def updateSingleListCustomField(customFiledId: Long,
                                                backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                propertyResolver: PropertyResolver): UpdateSingleListCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params = new UpdateSingleListCustomFieldParams(projectKey, customFiledId)
        setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
        params.allowAddItem(property.allowAddItem)
        params.allowInput(property.allowInput)
        params
      case _ => throw new RuntimeException()
    }

  private[this] def updateMultipleListCustomField(customFiledId: Long,
                                                  backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                                  propertyResolver: PropertyResolver): UpdateMultipleListCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params = new UpdateMultipleListCustomFieldParams(projectKey, customFiledId)
        setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
        params.allowAddItem(property.allowAddItem)
        params.allowInput(property.allowInput)
        params
      case _ => throw new RuntimeException()
    }

  private[this] def updateCheckBoxCustomField(customFiledId: Long,
                                              backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                              propertyResolver: PropertyResolver): UpdateCheckBoxCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params = new UpdateCheckBoxCustomFieldParams(projectKey, customFiledId)
        setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
        params.allowAddItem(property.allowAddItem)
        params.allowInput(property.allowInput)
        params
      case _ => throw new RuntimeException()
    }

  private[this] def updateRadioCustomField(customFiledId: Long,
                                           backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                           propertyResolver: PropertyResolver): UpdateRadioCustomFieldParams =
    backlogCustomFieldSetting.property match {
      case property: BacklogCustomFieldMultipleProperty =>
        val params = new UpdateRadioCustomFieldParams(projectKey, customFiledId)
        setUpdateCustomFieldParams(params, backlogCustomFieldSetting, propertyResolver)
        params.allowAddItem(property.allowAddItem)
        params.allowInput(property.allowInput)
        params
      case _ => throw new RuntimeException()
    }

  private[this] def setUpdateCustomFieldParams(params: UpdateCustomFieldParams,
                                               backlogCustomFieldSetting: BacklogCustomFieldSetting,
                                               propertyResolver: PropertyResolver) = {
    params.required(backlogCustomFieldSetting.required)
    params.applicableIssueTypes(
      backlogCustomFieldSetting.applicableIssueTypes.flatMap(propertyResolver.optResolvedIssueTypeId).map(Long.box).asJava
    )
  }

}
