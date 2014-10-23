package client

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js
import org.scalajs.jquery.{jQuery => jQ, _}

import akka.actor._
import akka.scalajs.p2p._

import models._
import transport.client._
import transport.akka._
import SockJSClient.addressFromPlayRoute

import scalajs.concurrent.JSExecutionContext.Implicits.runNow

@JSExport("Client")
object Main {
  RegisterPicklers.registerPicklers()

  implicit val system = ActorSystem("chat-client")

  @JSExport
  def startup(): Unit = {
    ActorWrapper(new SockJSClient()).connectWithActor(addressFromPlayRoute())(
      EstablishRtcActor.props)
  }
}

class EstablishRtcActor(out: ActorRef) extends Actor {
  override def receive: Receive = {
    case Connected(peer) =>
      val ref = WebRTCCallee.answerWithActor(DemoActor.props)(context.system)
      peer ! ref
    case calleeRef: ActorRef =>
      WebRTCCaller(calleeRef).callWithActor(DemoActor.props)(context.system)
  }
}
object EstablishRtcActor {
  def props(out: ActorRef) = Props(new EstablishRtcActor(out))
}

class DemoActor(out: ActorRef) extends Actor {
  override def preStart() = {
    jQ("#msgform").submit { (event: JQueryEventObject) =>
      event.preventDefault()
      self ! Submit
    }
    jQ("#spinner").hide()
    jQ("#msgform").show()
  }
  
  override def postStop() = {
    jQ("#msgtext").prop("disabled", true)
    jQ("#discussion").append("<hr>")
  }

  def receive = {
    case Submit =>
      val text = jQ("#msgtext").value().toString
      if(!text.isEmpty) {
        jQ("#msgtext").value("")
        out ! Msg(text)
        Discussion.appendMy(text)
      }
      
    case Msg(text) =>
      Discussion.appendHis(text)
  }
  
  object Submit
}
object DemoActor {
  def props(out: ActorRef) = Props(new DemoActor(out))
}

object Discussion {
  def appendMy = append("self") _
  def appendHis = append("other") _
  private def append(cssClass: String)(message: String): Unit = {
    val discussion = jQ("#discussion")
    discussion.append(
      s"""<li class="$cssClass">
         |  <div class="avatar"></div>
         |  <div class="messages">
         |    <p>$message</p>
         |  </div>
         |</li>
      """.stripMargin)
    discussion.scrollTop(discussion.prop("scrollHeight").asInstanceOf[Int]) 
  }
}