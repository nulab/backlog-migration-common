package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogCustomFieldSetting
import com.nulabinc.backlog4j.api.option.{AddCustomFieldParams, UpdateCustomFieldParams}

/**
  * @author uchida
  */
trait CustomFieldSettingService {

  def allCustomFieldSettings(): Seq[BacklogCustomFieldSetting]

  def setAddParams(backlogCustomFieldSetting: BacklogCustomFieldSetting): AddCustomFieldParams

  def add(setAddParam: BacklogCustomFieldSetting => AddCustomFieldParams)(backlogCustomFieldSetting: BacklogCustomFieldSetting): Unit

  def setUpdateParams(propertyResolver: PropertyResolver)(backlogCustomFieldSetting: BacklogCustomFieldSetting): Option[UpdateCustomFieldParams]

  def update(setUpdateParams: BacklogCustomFieldSetting => Option[UpdateCustomFieldParams])(backlogCustomFieldSetting: BacklogCustomFieldSetting): Unit

  def remove(customFieldSettingId: Long): Unit

}
