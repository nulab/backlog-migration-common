package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.{BacklogCustomFieldSetting, BacklogCustomFieldTextProperty}
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.CustomField.FieldType

/**
  * @author uchida
  */
class CustomFieldSettingNameWrites @Inject()() extends Writes[String, BacklogCustomFieldSetting] with Logging {

  override def writes(name: String): BacklogCustomFieldSetting = {
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

}
