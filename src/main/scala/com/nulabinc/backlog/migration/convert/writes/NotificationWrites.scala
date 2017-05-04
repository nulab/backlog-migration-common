package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.domain.BacklogNotification
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.Notification

/**
  * @author uchida
  */
class NotificationWrites @Inject()(implicit val userWrites: UserWrites) extends Writes[Notification, BacklogNotification] with Logging {

  override def writes(notification: Notification): BacklogNotification = {
    BacklogNotification(optUser = Option(notification.getUser).map(Convert.toBacklog(_)),
                        optSenderUser = Option(notification.getSender).map(Convert.toBacklog(_)))
  }

}
