package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import javax.inject.Inject
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.convert.writes.UserWrites

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class UserServiceImpl @Inject()(implicit val userWrites: UserWrites, backlog: BacklogAPIClient) extends UserService {

  override def allUsers(): Seq[BacklogUser] =
    backlog.getUsers.asScala.map(Convert.toBacklog(_))

  override def myself(): BacklogUser =
    Convert.toBacklog(backlog.getMyself)

}
