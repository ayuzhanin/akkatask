package org.remote.app.utils

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import org.remote.app.controller.ControllerActor
import org.remote.app.sender.ManagerActor
import org.remote.app.utils.CLIInputProcessor.{ControllerConfig, SenderConfig}

object Role {
  def startController(config: ControllerConfig): Unit = {
    val address = config.localhost.split(':').toList
    val (hostname, port) = (address.head, address.last)
    val system = ActorSystem(AkkaConfig.systemName, ConfigFactory.parseString(AkkaConfig.create(hostname, port)))
    val hostControllerActorDict = config.hosts map { host => host -> system.actorOf(ControllerActor.props(host), ControllerActor.actorName(host)) }
    val map: Map[String, ActorRef] = hostControllerActorDict.toMap
    println(s"Started ${AkkaConfig.systemName}")
    CLIInputProcessor.until(CLIInputProcessor.interrupted, CLIInputProcessor.processInput)((map, "pass", system))
  }

  //val system = ActorSystem(systemName, ConfigFactory.load("remote"))
  def startSender(config: SenderConfig): Unit = {
    val address = config.localhost.split(':').toList
    val (hostname, port) = (address.head, address.last)
    val system = ActorSystem(AkkaConfig.systemName, ConfigFactory.parseString(AkkaConfig.create(hostname, port)))
    val actor = system.actorOf(ManagerActor.props(config.actions, config.localhost), ManagerActor.actorName)
    println(s"Started ${AkkaConfig.systemName}: ${actor.path} is created")
  }
}
