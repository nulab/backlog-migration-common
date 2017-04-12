package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogCustomFieldSetting
import com.nulabinc.backlog4j.api.option.{AddCustomFieldParams, UpdateCustomFieldParams}

/**
  * @author uchida
  */
trait CustomFieldSettingService {

  def allCustomFieldSettings(): Seq[BacklogCustomFieldSetting]

  def setAddParams(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddCustomFieldParams

  def add(setAddParam: BacklogCustomFieldSetting => AddCustomFieldParams)(backlogCustomFieldSetting: BacklogCustomFieldSetting)

  def setUpdateParams(propertyResolver: PropertyResolver)(backlogCustomFieldSetting: BacklogCustomFieldSetting): Option[UpdateCustomFieldParams]

  def update(setUpdateParams: BacklogCustomFieldSetting => Option[UpdateCustomFieldParams])(backlogCustomFieldSetting: BacklogCustomFieldSetting)

  def remove(customFieldSettingId: Long)

}
