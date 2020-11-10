package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogAttributeInfo
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.AttributeInfo

/**
 * @author uchida
 */
private[common] class AttributeInfoWrites @Inject() ()
    extends Writes[AttributeInfo, BacklogAttributeInfo]
    with Logging {

  override def writes(attributeInfo: AttributeInfo): BacklogAttributeInfo = {
    BacklogAttributeInfo(
      optId = Option(attributeInfo).map(_.getId),
      typeId = attributeInfo.getTypeId
    )
  }

}
