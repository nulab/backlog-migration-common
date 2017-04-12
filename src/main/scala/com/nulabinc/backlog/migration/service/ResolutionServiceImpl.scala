package com.nulabinc.backlog.migration.service
import javax.inject.Inject

import com.nulabinc.backlog4j.{BacklogClient, Resolution}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class ResolutionServiceImpl @Inject()(backlog: BacklogClient) extends ResolutionService {

  override def allResolutions(): Seq[Resolution] =
    backlog.getResolutions.asScala

}
