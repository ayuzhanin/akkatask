package org.remote.app

import org.remote.app.utils.CLIInputProcessor._
import org.remote.app.utils._

import scala.util.Failure

object App {
    def main(args: Array[String]): Unit = {

    val tryConf = CLIInputProcessor.configuration(args)
    for (conf <- tryConf) conf match {
      case config: ControllerConfig =>
        println("Starting controller role")
        Role.startController(config)
      case config: SenderConfig =>
        println("Starting sender role")
        Role.startSender(config)
      case other: Any =>
        println(s"Something was wrong $other")
    }
    tryConf match {
      case Failure(ex) => ex.printStackTrace()
      case _ =>
    }
  }
}