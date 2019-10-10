package bindable

import java.time.format.TextStyle
import java.time.{Clock, DayOfWeek}
import java.util.Locale

import cats.data.EitherT
import cats.implicits._
import controllers.{Interval, Limit, Note, SearchRequest}
import play.api.mvc.QueryStringBindable

import scala.util.Try

trait SearchRequestBindable {

  type R = SearchRequest
  type Params = Map[String, Seq[String]]
  type Bindable[A] = QueryStringBindable[A]

  implicit lazy val clock: Clock = Clock.systemDefaultZone()

  implicit def searchRequestBindable(
    implicit intBinder: Bindable[Int],
    longBinder: Bindable[Long],
    stringBinder: Bindable[String],
    stringSeqBinder: Bindable[Seq[String  ]]
  ): Bindable[R] = new Bindable[R] {
    override def bind(key: String, params: Params): Option[Either[String, R]] = Some(Right({
      implicit val p: Params = params
      SearchRequest(
        Interval(
          bindToOption[Long, Long]("from")(x => x),
          bindToOption[Long, Long]("to")(x => x)
        ),
        bindToOption[Seq[String], Set[DayOfWeek]]("dayOfWeek")(_.flatMap(stringToDayOfWeek).toSet).getOrElse(Set.empty),
        bindToOption[String, Note]("note")(Note),
        bindToOption[Int, Limit]("limit")(Limit)
      )
    }))

    override def unbind(key: String, r: R): String = {
      def ubnd[T](key: String, s: Option[T])(implicit b: Bindable[T]): Option[String] =
        s.map(f => b.unbind(key, f))
      val keys = Seq(
          ubnd("from", Some(r.interval.from.toInstant.toEpochMilli)),
          ubnd("to", Some(r.interval.to.toInstant.toEpochMilli)),
          ubnd("dayOfWeek", Some(r.dayOfWeeks.map(_.getDisplayName(TextStyle.FULL, Locale.JAPAN)).toSeq)),
          ubnd("note", r.note.map(_.v)),
          ubnd("limit", r.limit.map(_.v)),
      )
      keys.flatten.mkString("&")
    }

    def stringToDayOfWeek(s: String): Option[DayOfWeek] =
      Try(DayOfWeek.valueOf(s.toUpperCase)).toOption

    private def bindToOption[A, B](key: String)(
      func: A => B
    )(implicit params: Params, bindable: Bindable[A]): Option[B] =
      EitherT(bindable.bind(key, params)).map(func).valueOrF(_ => None)

  }
}

object Implicits extends SearchRequestBindable
