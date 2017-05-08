package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog4j.{BacklogClient, Status}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class StatusServiceImpl @Inject()(backlog: BacklogClient) extends StatusService {

  override def allStatuses(): Seq[Status] =
    backlog.getStatuses.asScala

}
