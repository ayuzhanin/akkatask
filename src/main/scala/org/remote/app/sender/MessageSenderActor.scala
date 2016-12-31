package org.remote.app.sender

import akka.actor.{Actor, ActorIdentity, ActorRef, Cancellable, Identify, PoisonPill, Props, ReceiveTimeout, Terminated}
import org.remote.app.messaging.{Message, UpdateMessagesPerSec}

import scala.concurrent.duration._

object MessageSenderActor {
  def props(remoteHost: String, local: String): Props = {
    Props(classOf[MessageSenderActor], remoteHost, local)
  }

  def remoteName(local: String) = s"from${local}receiver"
}

class MessageSenderActor(remoteHost: String, local: String) extends Actor {

  val remoteActorName = MessageSenderActor.remoteName(local)
  val remoteActorPath = s"akka.tcp://${context.system.name}@$remoteHost/user/${context.parent.path.name}/$remoteActorName"
  val rand = scala.util.Random

  import context.dispatcher

  resolve()

  def resolve() = {
    context.actorSelection(remoteActorPath) ! Identify(rand.nextInt(1000))
    context.system.scheduler.scheduleOnce(5.seconds, self, ReceiveTimeout)
  }

  override def postStop {
    println(s"${self.path} was stopped")
  }

  def messageFrequently(remote: ActorRef, messagesPerSec: Int): Cancellable = {
    val interval = (1.0 / messagesPerSec).second
    context.system.scheduler.schedule(Duration.Zero, interval) {
      val num = rand.nextInt(1000)
      //println(s"Sending to ${remote.path}: $num")
      remote ! Message(num)
    }
  }

  override def receive = identifying

  def identifying: Receive = {
    case ActorIdentity(identifyId, Some(remote)) =>
      context.watch(remote)
      println(s"ActorIdentity was received from ${remote.path}")
      val repeating = messageFrequently(remote, 10)
      context become active(remote, repeating)
    case ActorIdentity(_, None) =>
      println(s"Remote actor $remoteActorPath is not available")
    case ReceiveTimeout =>
      println("ReceiveTimeout: starting actor selection")
      resolve()
    case _ => println("Not ready yet")
  }

  def active(remote: ActorRef, current: Cancellable): Receive = {
    case UpdateMessagesPerSec(number) =>
      current.cancel()
      if (number > 0) {
        val repeating = messageFrequently(remote, number)
        context become active(remote, repeating)
      } else if (number == 0){
        context become active(remote, current)
      }
    case Terminated(`remote`) =>
      println(s"${`remote`.path} was terminated: starting actor selection")
      resolve()
      context become identifying
    case ReceiveTimeout => //Ignore
    case PoisonPill => current.cancel()
    case a => println(s"Unexpected message: $a")
  }
}