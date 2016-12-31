package org.remote.app.utils

object AkkaConfig {
  val systemName = "messagingSystem"

  def create(hostname: String, port: String): String =
    s"""
    akka {
      loglevel = "INFO"
      actor {
        provider = remote
      }
      remote {
        netty.tcp {
          hostname = "$hostname"
          port = $port
        }
      }
    }
    """
}
