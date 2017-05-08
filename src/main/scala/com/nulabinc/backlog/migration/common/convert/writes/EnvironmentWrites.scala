package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogEnvironment
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.Environment

/**
  * @author uchida
  */
class EnvironmentWrites @Inject()() extends Writes[Environment, BacklogEnvironment] with Logging {

  override def writes(environment: Environment): BacklogEnvironment = {
    BacklogEnvironment(
      name = environment.getName,
      spaceId = environment.getSpaceId
    )
  }

}
