package com.nulabinc.backlog.migration.common.domain.mappings

import java.nio.charset.{Charset, StandardCharsets}

import com.nulabinc.backlog.migration.common.codec.{
  Encoder,
  PriorityMappingEncoder,
  StatusMappingEncoder,
  UserMappingEncoder
}
import com.nulabinc.backlog.migration.common.domain.{
  BacklogStatusName,
  BacklogStatuses,
  BacklogUser
}
import com.nulabinc.backlog4j.Priority
import monix.reactive.Observable

object MappingEncoder {

  private val charset: Charset = StandardCharsets.UTF_8

  private implicit val backlogStatusEncoder: Encoder[BacklogStatusName, Seq[String]] =
    (statusName: BacklogStatusName) => Seq(statusName.trimmed)

  private implicit val backlogPriorityEncoder: Encoder[Priority, Seq[String]] =
    (priority: Priority) => Seq(priority.getName)

  private implicit val backlogUserEncoder: Encoder[BacklogUser, Seq[String]] =
    (user: BacklogUser) => Seq(user.optUserId.getOrElse(""), user.optMailAddress.getOrElse(""))

  private val statusListHeader   = toByteArray(toRow(Seq("Name")))
  private val priorityListHeader = toByteArray(toRow(Seq("Name")))
  private val userListHeader     = toByteArray(toRow(Seq("Name", "Email")))

  def status[A](mappings: Seq[StatusMapping[A]])(implicit
      encoder: StatusMappingEncoder[A],
      header: MappingHeader[StatusMapping[_]]
  ): Observable[Array[Byte]] =
    fromHeader(header) +: toObservable(mappings)

  def priority[A](mappings: Seq[PriorityMapping[A]])(implicit
      encoder: PriorityMappingEncoder[A],
      header: MappingHeader[PriorityMapping[_]]
  ): Observable[Array[Byte]] =
    fromHeader(header) +: toObservable(mappings)

  def user[A](mappings: Seq[UserMapping[A]])(implicit
      encoder: UserMappingEncoder[A],
      header: MappingHeader[UserMapping[_]]
  ): Observable[Array[Byte]] =
    fromHeader(header) +: toObservable(mappings)

  def statusList(statuses: BacklogStatuses): Observable[Array[Byte]] =
    statusListHeader +: toObservable(statuses.map(_.name))

  def priorityList(priorities: Seq[Priority]): Observable[Array[Byte]] =
    priorityListHeader +: toObservable(priorities)

  def userList(users: Seq[BacklogUser]): Observable[Array[Byte]] =
    userListHeader +: toObservable(users)

  private def fromHeader(header: MappingHeader[Mapping[_]]): Array[Byte] =
    toByteArray(toRow(header.headers))

  private def toObservable[A](
      items: Seq[A]
  )(implicit encoder: Encoder[A, Seq[String]]): Observable[Array[Byte]] =
    Observable.fromIteratorUnsafe(items.iterator).map(encoder.encode).map(toRow).map(toByteArray)

  private def toRow(values: Seq[String]): String =
    s""""${values.mkString("\",\"")}\"\n""".stripMargin

  private def toByteArray(str: String): Array[Byte] =
    str.getBytes(charset)
}
