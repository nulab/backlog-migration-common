package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogAttributeInfo
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.AttributeInfo

/**
  * @author uchida
  */
class AttributeInfoWrites @Inject()() extends Writes[AttributeInfo, BacklogAttributeInfo] with Logging {

  override def writes(attributeInfo: AttributeInfo): BacklogAttributeInfo = {
    BacklogAttributeInfo(optId = Option(attributeInfo).map(_.getId), typeId = attributeInfo.getTypeId)
  }

}
