package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.streamer.{ Streamer => StreamerModel, StreamerForm }
import views._

object Streamer extends LilaController {

  private def api = Env.streamer.api

  def index(page: Int) = Open { implicit ctx =>
    val requests = getBool("requests") && isGranted(_.Streamers)
    Env.streamer.pager(page, requests) map { pager =>
      Ok(html.streamer.index(pager, requests))
    }
  }

  def show(username: String) = Open { implicit ctx =>
    OptionFuResult(api find username) { s =>
      WithVisibleStreamer(s) {
        Ok(html.streamer.show(s)).fuccess
      }
    }
  }

  def create = AuthBody { implicit ctx => me =>
    NoLame {
      NoShadowban {
        api.find(me) flatMap {
          case None => api.create(me) inject Redirect(routes.Streamer.edit)
          case _ => Redirect(routes.Streamer.edit).fuccess
        }
      }
    }
  }

  def edit = Auth { implicit ctx => me =>
    AsStreamer { s =>
      NoCache(Ok(html.streamer.edit(s, StreamerForm userForm s.streamer))).fuccess
    }
  }

  def editApply = AuthBody { implicit ctx => me =>
    AsStreamer { s =>
      implicit val req = ctx.body
      StreamerForm.userForm(s.streamer).bindFromRequest.fold(
        error => BadRequest(html.streamer.edit(s, error)).fuccess,
        data => api.update(s.streamer, data, isGranted(_.Streamers)) inject Redirect {
          s"${routes.Streamer.edit().url}${if (s.streamer is me) "" else "?u=" + s.user.id}"
        }
      )
    }
  }

  def approvalRequest = AuthBody { implicit ctx => me =>
    api.approval.request(me) inject Redirect(routes.Streamer.edit)
  }

  def picture = Auth { implicit ctx => _ =>
    AsStreamer { s =>
      NoCache(Ok(html.streamer.picture(s))).fuccess
    }
  }

  def pictureApply = AuthBody(BodyParsers.parse.multipartFormData) { implicit ctx => _ =>
    AsStreamer { s =>
      ctx.body.body.file("picture") match {
        case Some(pic) => api.uploadPicture(s.streamer, pic) recover {
          case e: lila.base.LilaException => BadRequest(html.streamer.picture(s, e.message.some))
        } inject Redirect(routes.Streamer.edit)
        case None => fuccess(Redirect(routes.Streamer.edit))
      }
    }
  }

  def pictureDelete = Auth { implicit ctx => _ =>
    AsStreamer { s =>
      api.deletePicture(s.streamer) inject Redirect(routes.Streamer.edit)
    }
  }

  private def AsStreamer(f: StreamerModel.WithUser => Fu[Result])(implicit ctx: Context) =
    ctx.me.fold(notFound) { me =>
      api.find(get("u").ifTrue(isGranted(_.Streamers)) | me.id) flatMap {
        _.fold(Ok(html.streamer.create(me)).fuccess)(f)
      }
    }

  private def WithVisibleStreamer(s: StreamerModel.WithUser)(f: Fu[Result])(implicit ctx: Context) =
    if (s.streamer.isListed || ctx.me.??(s.streamer.is) || isGranted(_.Admin)) f
    else notFound
}
