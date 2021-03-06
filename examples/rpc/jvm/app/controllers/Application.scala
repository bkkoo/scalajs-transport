package controllers

import scala.util._

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current
import shared.{ Api, MyRpcWrapper }

import play.sockjs.api.SockJSRouter
import transport.play._
import transport.rpc._

object Application extends Controller {
  
  def indexDev = Action { implicit request =>
    Ok(views.html.index(devMode = true, PlayUtils.sockJSRoute(sockJS)))
  }

  def indexOpt = Action { implicit request =>
    Ok(views.html.index(devMode = false, PlayUtils.sockJSRoute(sockJS)))
  }
  
  val transport = new SockJSServer()
  val sockJS = transport.action()
  
  new MyRpcWrapper(transport).serve(_.route[Api](Server))
}

object Server extends Api { 
  def list(path: String): Seq[String] = {
    val chunks = path.split("/", -1)
    val prefix = "./" + chunks.dropRight(1).mkString("/")
    val files = Option(new java.io.File(prefix).list()).toSeq.flatten
    files.filter(_.startsWith(chunks.last))
  }
  def double(i: Int) = 2 * i
}
