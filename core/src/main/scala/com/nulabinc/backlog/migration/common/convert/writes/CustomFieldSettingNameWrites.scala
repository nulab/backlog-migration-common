package com.nulabinc.backlog.migration.common.convert.writes

import com.nulabinc.backlog.migration.common.domain.{
  BacklogCustomFieldSetting,
  BacklogCustomFieldTextProperty
}
import com.nulabinc.backlog4j.CustomField.FieldType

object CustomFieldSettingNameWrites {
  def writes(name: String): BacklogCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = None,
      rawName = name,
      description = "",
      typeId = FieldType.Text.getIntValue,
      required = false,
      applicableIssueTypes = Seq(),
      property = BacklogCustomFieldTextProperty(FieldType.Text.getIntValue),
      delete = true
    )
}
