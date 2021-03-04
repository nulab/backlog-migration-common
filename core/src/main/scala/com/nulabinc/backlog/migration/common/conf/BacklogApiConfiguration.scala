package com.nulabinc.backlog.migration.common.conf

import java.nio.file.{Path, Paths}

import com.nulabinc.backlog.migration.common.client.IAAH

case class BacklogApiConfiguration(
    url: String,
    key: String,
    projectKey: String,
    backlogOutputPath: Path = Paths.get("./backlog"),
    override val iaah: IAAH
) extends BacklogConfiguration {
  val isNAISpace: Boolean = url.contains(NaiSpaceDomain)
}
