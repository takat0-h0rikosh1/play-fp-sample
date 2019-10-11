package bindable

import java.time.DayOfWeek

import cats.data.EitherT
import cats.implicits._
import controllers._
import play.api.mvc.QueryStringBindable

trait SearchRequestBindable extends BindableSupport {

  private type R = SearchRequest
  private type Params = Map[String, Seq[String]]
  private type Bindable[A] = QueryStringBindable[A]
  private type Bound[A] = Option[Either[String, A]]

  implicit def searchRequestBindable(
      implicit
      longBinder: Bindable[Long],
      stringBinder: Bindable[String],
      intervalBindable: Bindable[Interval],
      dayOfWeekBinder: Bindable[Seq[DayOfWeek]]
  ): Bindable[R] = new Bindable[R] {
    override def bind(key: String, params: Params): Bound[R] = {
      implicit val p: Params = params
      for {
        interval <- EitherT(intervalBindable.bind(key, params))
        dayOfWeek <- EitherT(
          Option(bindToRightOption[Seq[DayOfWeek]]("dayOfWeek")))
        note = EitherT(stringBinder.bind("note", params)).valueOrF(_ => None)
        limit <- EitherT(Option(bindToRightOption[Long]("limit")))
      } yield
        SearchRequest(
          interval,
          dayOfWeek.getOrElse(Seq.empty).toSet,
          note.map(Note),
          limit.map(Limit)
        )
    }.value

    override def unbind(key: String, r: R): String = {
      def unbind[T](key: String, s: Option[T])(
          implicit b: Bindable[T]): Option[String] =
        s.map(f => b.unbind(key, f))

      val keys = Seq(
        unbind("from", Some(r.interval)),
        unbind("dayOfWeek", Some(r.dayOfWeeks.toSeq)),
        unbind("note", r.note.map(_.v)),
        unbind("limit", r.limit.map(_.v)),
      )
      keys.flatten.mkString("&")
    }
  }
}
