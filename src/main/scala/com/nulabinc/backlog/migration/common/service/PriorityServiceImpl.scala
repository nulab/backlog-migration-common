package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog4j.{BacklogClient, Priority}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class PriorityServiceImpl @Inject()(backlog: BacklogClient) extends PriorityService {

  override def allPriorities(): Seq[Priority] =
    backlog.getPriorities.asScala

}
