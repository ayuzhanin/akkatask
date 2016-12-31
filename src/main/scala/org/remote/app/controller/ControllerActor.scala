package org.remote.app.controller

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Props, ReceiveTimeout, Terminated}
import org.remote.app.messaging.Action
import org.remote.app.sender.ManagerActor

import scala.concurrent.duration._

object ControllerActor {
  def props(remoteHost: String): Props = {
    Props(classOf[ControllerActor], remoteHost)
  }

  def actorName(host: String) = s"${host}controller"
}
class ControllerActor(remoteHost: String) extends Actor {

  val remoteActorPath = s"akka.tcp://${context.system.name}@$remoteHost/user/${ManagerActor.actorName}"

  def resolve(): Unit = {
    context.actorSelection(remoteActorPath) ! Identify(remoteActorPath)
    import context.dispatcher
    context.system.scheduler.scheduleOnce(10.seconds, self, ReceiveTimeout)
  }

  resolve()

  override def receive: Receive = identifying

  def identifying: Receive = {
    case ActorIdentity(`remoteActorPath`, Some(actor)) =>
      context.watch(actor)
      println(s"ActorIdentity was received from ${actor.path}")
      context become active(actor)
    case ActorIdentity(_, None) => println(s"Remote actor not available $remoteActorPath")
    case ReceiveTimeout         => resolve()
    case a                      => println(s"Not ready yet for $a")
  }

  def active(actor: ActorRef): Receive = {
    case ac: Action =>
      println(ac)
      actor ! ac
    case Terminated(`actor`) =>
      println(s"${actor.path} was terminated")
      resolve()
      context.become(identifying)
    case ReceiveTimeout => //Ignore
  }
}