package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogCustomField
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.migration.common.utils.DateUtil
import com.nulabinc.backlog4j.CustomField
import com.nulabinc.backlog4j.internal.json.customFields._

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
private[common] class CustomFieldWrites @Inject()() extends Writes[CustomField, Option[BacklogCustomField]] with Logging {

  override def writes(customField: CustomField): Option[BacklogCustomField] = {
    customField match {
      case _: TextCustomField         => Some(toTextCustomField(customField))
      case _: TextAreaCustomField     => Some(toTextAreaCustomField(customField))
      case _: NumericCustomField      => Some(toNumericCustomField(customField))
      case _: DateCustomField         => Some(toDateCustomField(customField))
      case _: SingleListCustomField   => Some(toSingleListCustomField(customField))
      case _: MultipleListCustomField => Some(toMultipleListCustomField(customField))
      case _: CheckBoxCustomField     => Some(toCheckBoxCustomField(customField))
      case _: RadioCustomField        => Some(toRadioCustomField(customField))
      case _                          => None
    }
  }

  private[this] def toTextCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case textCustomField: TextCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = Option(textCustomField.getValue),
          values = Seq.empty[String]
        )
      case _ => throw new RuntimeException()
    }

  private[this] def toTextAreaCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case textAreaCustomField: TextAreaCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = Option(textAreaCustomField.getValue),
          values = Seq.empty[String]
        )
      case _ => throw new RuntimeException()
    }

  private[this] def toNumericCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case numericCustomField: NumericCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = Option(numericCustomField.getValue).map(_.toString),
          values = Seq.empty[String]
        )
      case _ => throw new RuntimeException()
    }

  private[this] def toDateCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case dateCustomField: DateCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = Option(dateCustomField.getValue).map(DateUtil.dateFormat),
          values = Seq.empty[String]
        )
      case _ => throw new RuntimeException()
    }

  private[this] def toSingleListCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case singleListCustomField: SingleListCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = Option(singleListCustomField.getValue).map(_.getName),
          values = Seq.empty[String]
        )
      case _ => throw new RuntimeException()
    }

  private[this] def toMultipleListCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case multipleListCustomField: MultipleListCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = None,
          values = multipleListCustomField.getValue.asScala.toSeq.map(_.getName)
        )
      case _ => throw new RuntimeException()
    }

  private[this] def toCheckBoxCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case checkBoxCustomField: CheckBoxCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = None,
          values = checkBoxCustomField.getValue.asScala.toSeq.map(_.getName)
        )
      case _ => throw new RuntimeException()
    }

  private[this] def toRadioCustomField(customField: CustomField): BacklogCustomField =
    customField match {
      case radioCustomField: RadioCustomField =>
        BacklogCustomField(
          name = customField.getName,
          fieldTypeId = customField.getFieldTypeId,
          optValue = Option(radioCustomField.getValue).map(_.getName),
          values = Seq.empty[String]
        )
      case _ => throw new RuntimeException()
    }

}
