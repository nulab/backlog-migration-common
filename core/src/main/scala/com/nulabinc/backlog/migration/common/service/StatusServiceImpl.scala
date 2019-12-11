package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import javax.inject.Inject
import com.nulabinc.backlog4j.Status

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class StatusServiceImpl @Inject()(backlog: BacklogAPIClient) extends StatusService {

  override def allStatuses(): Seq[Status] =
    backlog.getStatuses.asScala.toSeq

}
